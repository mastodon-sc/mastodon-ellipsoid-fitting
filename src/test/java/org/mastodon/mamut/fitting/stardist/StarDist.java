package org.mastodon.mamut.fitting.stardist;

import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;

import java.util.ArrayList;
import java.util.List;

// TODO implementation of non-maximum suppression (NMS) is yet required,
// cf. https://github.com/stardist/stardist-imagej/blob/master/src/main/java/de/csbdresden/stardist/StarDist2DNMS.java
// otherwise there are too many non-maxima / irrelevant star-convex shapes in the output
public class StarDist
{

	/**
	 * The predicted star convex shapes. Includes many irrelevant shapes.
	 * Non-maximum suppression (NMS) is yet required to reduce the number of shapes.
	 */
	private final List< StarConvexPolyhedra > starConvexShapes = new ArrayList<>();

	public StarDist( RandomAccessibleInterval< FloatType > distances, RandomAccessibleInterval< FloatType > probabilities )
	{
		this( distances, probabilities, 0.4, 2 );
	}

	private StarDist(
			RandomAccessibleInterval< FloatType > distances, RandomAccessibleInterval< FloatType > probabilities, double threshold,
			int buffer
	)
	{
		final long[] dimensions = Intervals.dimensionsAsLongArray( distances );
		if ( dimensions.length != 5 )
			throw new IllegalArgumentException( "Input is expected to have 5 dimensions, but has: " + dimensions.length + "dimensions." );

		int numberOfRays = ( int ) dimensions[ 4 ];
		System.out.println( "numberOfRays = " + numberOfRays );

		processTensors( distances.randomAccess(), probabilities.randomAccess(), threshold, buffer, dimensions, numberOfRays );
	}

	public List< StarConvexPolyhedra > getStarConvexShapes()
	{
		return starConvexShapes;
	}

	/**
	 *
	 * @return the origins
	 */
	private void processTensors(
			RandomAccess< FloatType > distances, final RandomAccess< FloatType > probabilities, double threshold, int buffer,
			long[] distancesTensors, int numberOfRays
	)
	{
		// origin is the center of the star convex shape
		System.out.println( "Computing star convex shapes..." );
		System.out.println( "x: (" + buffer + " - " + ( distancesTensors[ 0 ] - buffer ) + ")" );
		System.out.println( "y: (" + buffer + " - " + ( distancesTensors[ 1 ] - buffer ) + ")" );
		System.out.println( "z: (" + buffer + " - " + ( distancesTensors[ 2 ] - buffer ) + ")" );
		for ( int originX = buffer; originX < distancesTensors[ 0 ] - buffer; originX++ )
		{
			for ( int originY = buffer; originY < distancesTensors[ 1 ] - buffer; originY++ )
			{
				for ( int originZ = buffer; originZ < distancesTensors[ 2 ] - buffer; originZ++ )
				{
					final float score = probabilities.setPositionAndGet( originX, originY, originZ, 0, 0 ).getRealFloat();
					if ( score > threshold )
					{
						// TODO use new class StarConvexPolyhedra here
						List< Double > distanceList = new ArrayList<>();
						for ( int i = 0; i < numberOfRays; i++ )
						{
							double distance = distances.setPositionAndGet( originX, originY, originZ, 0, i ).getRealDouble();
							distanceList.add( distance );
						}
						StarConvexPolyhedra polyhedra = new StarConvexPolyhedra( new double[] { originX, originY, originZ }, distanceList );
						starConvexShapes.add( polyhedra );
					}
				}
			}
		}

		System.out.println( "Found " + starConvexShapes.size() + " candidate for star convex shapes above threshold of " + threshold
				+ " (including non-maximum shapes)." );
		// TODO: add non-maximum suppression (NMS) here
		// cf. https://github.com/stardist/stardist-imagej/blob/master/src/main/java/de/csbdresden/stardist/Candidates.java#L123
	}
}
