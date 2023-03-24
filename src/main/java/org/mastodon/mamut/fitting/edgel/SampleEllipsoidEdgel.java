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

import org.mastodon.mamut.fitting.ellipsoid.DistPointHyperEllipsoid;
import org.mastodon.mamut.fitting.ellipsoid.Ellipsoid;
import org.mastodon.mamut.fitting.ellipsoid.FitEllipsoid;
import org.mastodon.mamut.fitting.ellipsoid.HyperEllipsoid;
import org.mastodon.mamut.fitting.ellipsoid.DistPointHyperEllipsoid.Result;

import net.imglib2.algorithm.edge.Edgel;
import net.imglib2.util.LinAlgHelpers;

import javax.annotation.Nullable;

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
	 * Try to fit an ellipsoid to the given {@link List} of {@link Edgel} objects.
	 *
	 * @param edgels list of edgels to fit an ellipsoid to.
	 * @param expectedCenter the expected center of the ellipsoid to fit. The center of the fitted ellipsoid is allowed to deviate from this value by at most {@code maxCenterDistance}.
	 * @param numSamples the number of samples to try. For each sample, a random subset of {@code numPointsPerSample} edgels is selected and fitted to an ellipsoid. The best fitting ellipsoid is returned.
	 * @param outsideCutoffDistance the maximum allowed distance that and edgel may be outside the ellipsoid surface.
	 * @param insideCutoffDistance the maximum allowed distance that and edgel may be inside the ellipsoid surface.
	 * @param angleCutoffDistance the maximum allowed angle between the normal of the fitted ellipsoid and the normal of the edgel.
	 * @param maxCenterDistance the maximum allowed distance between the center of the fitted ellipsoid and the expected center.
	 * @return the best fitting ellipsoid, or {@code null} if no ellipsoid could be fit. The latter happens if there are not enough edgels for fitting (at least 9 are required) or if the fitting fails.
	 */
	@Nullable
	public static Ellipsoid sample(
			final List< Edgel > edgels,
			final double[] expectedCenter,
			final int numSamples,
			final double outsideCutoffDistance,
			final double insideCutoffDistance,
			final double angleCutoffDistance,
			final double maxCenterDistance )
	{
		final int numPointsPerSample = 9;
		// Avoid endless loop if there are not enough edgels for ellipsoid fitting.
		if ( edgels.size() < numPointsPerSample )
		{
			// System.out.println(
			//		"Too few edgels. Required " + numPointsPerSample + " edgels, but got " + edgels.size() + "." );
			return null;
		}

		final Cost costFunction = new EdgelDistanceCost( outsideCutoffDistance, insideCutoffDistance, angleCutoffDistance );

		final Random rand = new Random( System.currentTimeMillis() );
		final ArrayList< Integer > indices = new ArrayList<>();
		final double[][] coordinates = new double[ numPointsPerSample ][ 3 ];

		Ellipsoid bestEllipsoid = null;
		double bestCost = Double.POSITIVE_INFINITY;
		final double[] center = new double[ 3 ];

		for ( int sample = 0; sample < numSamples; sample++ )
		{
			try
			{
				indices.clear();
				for ( int s = 0; s < numPointsPerSample; s++ )
				{
					int i = rand.nextInt( edgels.size() );
					while ( indices.contains( i ) )
						i = rand.nextInt( edgels.size() );
					indices.add( i );
					edgels.get( i ).localize( coordinates[ s ] );
				}
				final Ellipsoid ellipsoid = FitEllipsoid.yuryPetrov( coordinates );

				// skip samples that cannot be fitted to an ellipsoid
				if ( ellipsoid == null )
					continue;

				ellipsoid.localize( center );
				if ( LinAlgHelpers.distance( center, expectedCenter ) > maxCenterDistance )
					continue;

				final double cost = costFunction.compute( ellipsoid, edgels );
				if ( cost < bestCost )
				{
					bestCost = cost;
					bestEllipsoid = ellipsoid;
				}
			}
			catch ( final IllegalArgumentException ignored )
			{
			}
		}

		if ( bestEllipsoid == null )
		{
			// System.out.println( "no best ellipsoid. potential reason: center of best ellipsoid too far from expected center." );
			return null;
		}

		// refined ellipsoid
		return fitToInliers( bestEllipsoid, edgels, costFunction );
	}

	/**
	 * Fit an ellipsoid to the {@link Edgel} objects that can be considered inliers of the given ellipsoid.
	 *
	 * @param ellipsoid             ellipsoid used to filter inliers.
	 * @param edgels                list of edgels to use for ellipsoid fitting.
	 * @param costFunction          the cost function to use for determining inliers.
	 * @return the ellipsoid best fitting to its inliers, or {@code ellipsoid} if no ellipsoid could be fit to the inliers.
	 */
	private static Ellipsoid fitToInliers(
			final Ellipsoid ellipsoid,
			final List< Edgel > edgels, Cost costFunction )
	{
		final ArrayList< Edgel > inliers = new ArrayList<>();
		for ( final Edgel edgel : edgels )
			if ( costFunction.isInlier( ellipsoid, edgel ) )
				inliers.add( edgel );

		final double[][] coordinates = new double[ inliers.size() ][ 3 ];
		for ( int i = 0; i < inliers.size(); ++i )
			inliers.get( i ).localize( coordinates[ i ] );

		if ( coordinates.length < 9 )
			return ellipsoid;

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
