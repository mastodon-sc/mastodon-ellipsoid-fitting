package org.mastodon.mamut.fitting.stardist;

import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import org.ejml.simple.SimpleMatrix;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

// TODO implementation of non-maximum suppression (NMS) is yet required,
// cf. https://github.com/stardist/stardist-imagej/blob/master/src/main/java/de/csbdresden/stardist/StarDist2DNMS.java
// otherwise there are too many non-maxima / irrelevant star-convex shapes in the output
public class StarDist
{

	/**
	 * The predicted star convex shapes. Includes many irrelevant shapes.
	 * Non-maximum suppression (NMS) is yet required to reduce the number of shapes.
	 */
	private final List< List< SimpleMatrix > > starConvexShapes = new ArrayList<>();

	// scale all coordinates by this value and divide later to get subpixel resolution
	private static final long SCALE = 100;

	public StarDist( RandomAccessibleInterval< FloatType > distances, RandomAccessibleInterval< FloatType > probabilities )
	{
		this( distances, probabilities, 0.4, 2 );
	}

	private StarDist(
			RandomAccessibleInterval< FloatType > distances, RandomAccessibleInterval< FloatType > probabilities, double threshold,
			int buffer
	)
	{
		final long[] distancesTensor = Intervals.dimensionsAsLongArray( distances );
		final int dimensions = distancesTensor.length;
		if ( dimensions != 5 )
			throw new IllegalArgumentException( "Input is expected to have 5 dimensions, but has: " + dimensions + "dimensions." );

		int numberOfRays = ( int ) distancesTensor[ 4 ];
		final List< Point3D > rays = initRays( numberOfRays );

		processTensors( distances.randomAccess(), probabilities.randomAccess(), threshold, buffer, distancesTensor, rays );
	}

	public List< List< SimpleMatrix > > getStarConvexShapes()
	{
		return starConvexShapes;
	}

	/**
	 *
	 * @return the origins
	 */
	private void processTensors(
			RandomAccess< FloatType > distances, final RandomAccess< FloatType > probabilities, double threshold, int buffer,
			long[] distancesTensors, List< Point3D > initialRays
	)
	{
		// origin is the center of the star convex shape
		// TODO: aren't we computing too many star convexShapes here?
		// TODO: this loop takes a lot of time in 3D, cropping may help
		for ( int originX = buffer; originX < distancesTensors[ 0 ] - buffer; originX++ )
		{
			for ( int originY = buffer; originY < distancesTensors[ 1 ] - buffer; originY++ )
			{
				for ( int originZ = buffer; originZ < distancesTensors[ 2 ] - buffer; originZ++ )
				{
					Point3D origin = new Point3D( originX, originY, originZ );
					final float score = probabilities.setPositionAndGet( originX, originY, originZ, 0, 0 ).getRealFloat();
					if ( score > threshold )
					{
						final List< SimpleMatrix > starConvexShape = new ArrayList<>();
						for ( int i = 0; i < initialRays.size(); i++ )
						{
							Point3D initialRay = initialRays.get( i );
							double distance = distances.setPositionAndGet( originX, originY, originZ, 0, i ).getRealDouble();
							initialRay.scale( distance ).scale( SCALE );
							starConvexShape.add( origin.plus( initialRay ) );
						}
						starConvexShapes.add( starConvexShape );
					}
				}
			}
		}
		System.out.println( "Found " + starConvexShapes.size() + " candidate for star convex shapes (including non-maximum shapes)." );
		// TODO: add non-maximum suppression (NMS) here
	}

	// linear spacing between -1 and 1 (equivalent to np.linspace(-1,1,n_rays))
	private static Double[] linSpace( int points )
	{
		Double[] d = new Double[ points ];
		for ( int i = 0; i < points; i++ )
			d[ i ] = ( double ) -1 + i * ( ( double ) 1 - ( double ) -1 ) / ( points - 1 );
		return d;
	}

	private static Integer[] range( int n )
	{
		Integer[] result = new Integer[ n ];
		for ( int i = 0; i < n; i++ )
			result[ i ] = i;
		return result;
	}

	private static List< Point3D > initRays( int numberOfRays )
	{
		Point3D anisotropy = new Point3D( 2, 1, 1 );
		List< Double > z = Arrays.asList( linSpace( numberOfRays ) );
		List< Double > rho = z.stream().map( a -> Math.sqrt( 1 - a * a ) ).collect( Collectors.toList() );
		final List< Integer > phi = Arrays.asList( range( numberOfRays ) );
		List< Point3D > ret = new ArrayList<>( 96 );
		for ( int i = 0; i < numberOfRays; i++ )
		{
			ret.add( new Point3D( z.get( i ), rho.get( i ) * Math.sin( phi.get( i ) ),
					rho.get( i ) * Math.cos( phi.get( i ) )
			).elementDiv( anisotropy ) );
		}
		return ret;
	}
}
