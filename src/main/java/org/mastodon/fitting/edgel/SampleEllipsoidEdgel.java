package org.mastodon.fitting.edgel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.imglib2.algorithm.edge.Edgel;
import net.imglib2.util.LinAlgHelpers;
import org.mastodon.fitting.ellipsoid.DistPointHyperEllipsoid;
import org.mastodon.fitting.ellipsoid.DistPointHyperEllipsoid.Result;
import org.mastodon.fitting.ellipsoid.Ellipsoid;
import org.mastodon.fitting.ellipsoid.FitEllipsoid;
import org.mastodon.fitting.ellipsoid.HyperEllipsoid;

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

		final Cost costFunction = new EdgelDistanceCost( outsideCutoffDistance, insideCutoffDistance, angleCutoffDistance );

		final Random rand = new Random( System.currentTimeMillis() );
		final ArrayList< Integer > indices = new ArrayList<>();
		final double[][] coordinates = new double[ numPointsPerSample ][ 3 ];

		Ellipsoid bestEllipsoid = null;
		double bestCost = Double.POSITIVE_INFINITY;
		final double[] center = new double[ 3 ];

		for ( int sample = 0; sample < numSamples; ++sample )
		{
			try
			{
				indices.clear();
				for ( int s = 0; s < numPointsPerSample; ++s )
				{
					int i = rand.nextInt( edgels.size() );
					while ( indices.contains( i ) )
						i = rand.nextInt( edgels.size() );
					indices.add( i );
					edgels.get( i ).localize( coordinates[ s ] );
				}
				final Ellipsoid ellipsoid = FitEllipsoid.yuryPetrov( coordinates );

				// don't count degenerate samples
				final double[] radii = ellipsoid.getRadii();
				if ( Double.isNaN( radii[ 0 ] ) || Double.isNaN( radii[ 1 ] ) || Double.isNaN( radii[ 2 ] ) )
				{
					--sample;
					continue;
				}

				ellipsoid.localize( center );
				LinAlgHelpers.subtract( center, expectedCenter, center );
				if ( LinAlgHelpers.length( center ) > maxCenterDistance )
					continue;

				final double cost = costFunction.compute( ellipsoid, edgels );
				if ( cost < bestCost )
				{
					bestCost = cost;
					bestEllipsoid = ellipsoid;
				}
			}
			catch ( final IllegalArgumentException e )
			{
//				e.printStackTrace();
//				System.out.println( "oops" );
			}
			catch ( final RuntimeException e )
			{
//				System.out.println( "psd" );
			}
		}

		if ( bestEllipsoid == null )
			return null;

		final Ellipsoid refined = fitToInliers( bestEllipsoid, edgels, outsideCutoffDistance, insideCutoffDistance, angleCutoffDistance );
		if ( refined == null )
		{
//			System.err.println( "refined ellipsoid == null! This shouldn't happen!");
			return bestEllipsoid;
		}
		return refined;
	}

	public static Ellipsoid fitToInliers(
			final Ellipsoid guess,
			final List< Edgel > edgels,
			final double outsideCutoffDistance,
			final double insideCutoffDistance,
			final double angleCutoffDistance )
	{
		final ArrayList< Edgel > inliers = new ArrayList<>();
		final Cost costFunction = new EdgelDistanceCost( outsideCutoffDistance, insideCutoffDistance, angleCutoffDistance );
		for ( final Edgel edgel : edgels )
			if ( costFunction.isInlier( guess, edgel ) )
				inliers.add( edgel );

		final double[][] coordinates = new double[ inliers.size() ][ 3 ];
		for ( int i = 0; i < inliers.size(); ++i )
			inliers.get( i ).localize( coordinates[ i ] );

		final Ellipsoid ellipsoid = FitEllipsoid.yuryPetrov( coordinates );
		return ellipsoid;
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
