/*-
 * #%L
 * mastodon-ellipsoid-fitting
 * %%
 * Copyright (C) 2015 - 2022 Tobias Pietzsch, Jean-Yves Tinevez
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package org.mastodon.mamut.fitting.edgel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.annotation.Nonnull;

import org.mastodon.mamut.fitting.ellipsoid.DistPointHyperEllipsoid;
import org.mastodon.mamut.fitting.ellipsoid.Ellipsoid;
import org.mastodon.mamut.fitting.ellipsoid.FitEllipsoid;
import org.mastodon.mamut.fitting.ellipsoid.HyperEllipsoid;
import org.mastodon.mamut.fitting.ellipsoid.DistPointHyperEllipsoid.Result;

import net.imglib2.algorithm.edge.Edgel;
import net.imglib2.util.LinAlgHelpers;

import gnu.trove.TIntArrayList;

public class SampleEllipsoidEdgel
{
	public static Map< Edgel, Double > getCosts(
			final List< ? extends Edgel > points,
			final Ellipsoid ellipsoid,
			final double outsideCutoffDistance,
			final double insideCutoffDistance,
			final double angleCutoffDistance )
	{
		final Cost costFunction = new EdgelDistanceCost( outsideCutoffDistance, insideCutoffDistance, angleCutoffDistance );
		final Map< Edgel, Double > costs = new HashMap<>();
		for ( final Edgel e : points )
//			costs.put( e, costFunction.compute( ellipsoid, e ) );
			costs.put( e, ( double ) ( costFunction.isInlier( ellipsoid, e ) ? 0 : 1 ) );
		return costs;
	}

	/**
	 * Try to fit an ellipsoid to the given edgels.
	 *
	 * @throws NoEllipsoidFoundException if no ellipsoid could be derived from the
	 * 		   given edgels.
	 */
	@Nonnull
	public static Ellipsoid sample(
			final List< Edgel > edgels,
			final double[] expectedCenter,
			final int numSamples,
			final int numCandidates,
			final double outsideCutoffDistance,
			final double insideCutoffDistance,
			final double angleCutoffDistance,
			final double maxCenterDistance )
	{
		final int numPointsPerSample = 9;
		if ( edgels.size() < numPointsPerSample )
			throw new NoEllipsoidFoundException( "Not enough edgels to fit an ellipsoid." );

		final Cost costFunction = new EdgelDistanceCost( outsideCutoffDistance, insideCutoffDistance, angleCutoffDistance );

		final Random rand = new Random( System.currentTimeMillis() );
		final TIntArrayList indices = new TIntArrayList();
		final double[][] coordinates = new double[ numPointsPerSample ][ 3 ];

		Ellipsoid bestEllipsoid = null;
		double bestCost = Double.POSITIVE_INFINITY;
		final double[] center = new double[ 3 ];
		int candidates = 0;

		for ( int sample = 0; sample < numSamples; ++sample )
		{
			sampleCoordinatesFromEdgels( edgels, coordinates, rand, indices );

			final Ellipsoid ellipsoid = tryFitEllipsoidYuryPetrov( coordinates );

			if ( isEllipsoidValid( ellipsoid, expectedCenter, maxCenterDistance, center ) )
			{
				final double cost = costFunction.compute( ellipsoid, edgels );
				if ( cost < bestCost )
				{
					bestCost = cost;
					bestEllipsoid = ellipsoid;
				}

				if ( ++candidates >= numCandidates )
					break;
			}
		}

		if ( bestEllipsoid == null ) // no ellipsoid found
			throw new NoEllipsoidFoundException( "No ellipsoid found, that is near to the expected center." );

		try
		{
			// refined ellipsoid
			return fitToInliers( edgels, bestEllipsoid, costFunction );
		}
		catch ( final RuntimeException e )
		{
			// refinement failed
			return bestEllipsoid;
		}
	}

	private static void sampleCoordinatesFromEdgels( List< Edgel > edgels, double[][] coordinates, Random rand, TIntArrayList indices )
	{
		indices.clear();
		for ( int s = 0; s < coordinates.length; ++s )
		{
			int i = rand.nextInt( edgels.size() );
			while ( indices.contains( i ) )
				i = rand.nextInt( edgels.size() );
			indices.add( i );
			edgels.get( i ).localize( coordinates[ s ] );
		}
	}

	private static Ellipsoid tryFitEllipsoidYuryPetrov( double[][] coordinates )
	{
		try
		{
			return FitEllipsoid.yuryPetrov( coordinates );
		}
		catch ( final RuntimeException e )
		{
			return null;
		}
	}

	private static boolean isEllipsoidValid( Ellipsoid ellipsoid, double[] expectedCenter, double maxCenterDistance, double[] center )
	{
		if ( ellipsoid == null )
			return false;

		// skip degenerate samples
		final double[] radii = ellipsoid.getRadii();
		if ( Double.isNaN( radii[ 0 ] ) || Double.isNaN( radii[ 1 ] ) || Double.isNaN( radii[ 2 ] ) )
			return false;

		ellipsoid.localize( center );
		return LinAlgHelpers.distance( expectedCenter, center ) <= maxCenterDistance;
	}

	/**
	 * Fits an ellipsoid to all the edgels that are considered to be inliers of
	 * the given "{@code guess}" ellipsoid.
	 *
	 * @param edgels       The edgels that are used to fit the ellipsoid.
	 * @param guess        The ellipsoid that is used to determine the inliers.
	 * @param costFunction The cost function that decides whether an edgel is an
	 *                     inlier or not.
	 * @return the fitted ellipsoid.
	 *
	 * @throws RuntimeException if the fitting fails.
	 */
	public static Ellipsoid fitToInliers(
			final List< Edgel > edgels,
			final Ellipsoid guess,
			final Cost costFunction )
	{
		final ArrayList< Edgel > inliers = new ArrayList<>();
		for ( final Edgel edgel : edgels )
			if ( costFunction.isInlier( guess, edgel ) )
				inliers.add( edgel );

		final double[][] coordinates = new double[ inliers.size() ][ 3 ];
		for ( int i = 0; i < inliers.size(); ++i )
			inliers.get( i ).localize( coordinates[ i ] );

		return FitEllipsoid.yuryPetrov( coordinates );
	}

	interface Cost
	{
		double compute( final Ellipsoid ellipsoid, final List< Edgel > points );

		double compute( final Ellipsoid ellipsoid, final Edgel point );

		boolean isInlier( final Ellipsoid ellipsoid, final Edgel point );
	}

	static class EdgelDistanceCost implements Cost
	{
		private final double outsideCutoff;

		private final double insideCutoff;

		private final double angleCutoff;

		private final double[] p = new double[ 3 ];

		private final double[] n = new double[ 3 ];

		public EdgelDistanceCost(
				final double outsideCutoffDistance,
				final double insideCutoffDistance,
				final double angleCutoffDistance )
		{
			outsideCutoff = outsideCutoffDistance;
			insideCutoff = insideCutoffDistance;
			angleCutoff = angleCutoffDistance;
		}

		@Override
		public double compute( final Ellipsoid ellipsoid, final List< Edgel > edgels )
		{
			double cost = 0;
			for ( final Edgel edgel : edgels )
				cost += compute ( ellipsoid, edgel );
			return cost;
		}

		@Override
		public double compute( final Ellipsoid ellipsoid, final Edgel edgel )
		{
			final Result result = DistPointHyperEllipsoid.distPointHyperEllipsoid( edgel, ellipsoid );
			final double dDist = result.distance;

			edgel.localize( p );
			HyperEllipsoid.normal( ellipsoid, p, n );
			final double dAngle = Math.acos( -LinAlgHelpers.dot( n, edgel.getGradient() ) );

			if ( ellipsoid.contains( edgel ) )
				return Math.min( dAngle, angleCutoff ) / angleCutoff + Math.min( dDist, insideCutoff ) / insideCutoff;
			else
				return Math.min( dAngle, angleCutoff ) / angleCutoff + Math.min( dDist, outsideCutoff ) / outsideCutoff;
		}

		@Override
		public boolean isInlier( final Ellipsoid ellipsoid, final Edgel edgel )
		{
			final Result result = DistPointHyperEllipsoid.distPointHyperEllipsoid( edgel, ellipsoid );
			final double dDist = result.distance;

			edgel.localize( p );
			HyperEllipsoid.normal( ellipsoid, p, n );
			final double dAngle = Math.acos( -LinAlgHelpers.dot( n, edgel.getGradient() ) );

			if ( ellipsoid.contains( edgel ) )
				return dAngle < angleCutoff && dDist < insideCutoff;
			else
				return dAngle < angleCutoff && dDist < outsideCutoff;
		}
	}
}
