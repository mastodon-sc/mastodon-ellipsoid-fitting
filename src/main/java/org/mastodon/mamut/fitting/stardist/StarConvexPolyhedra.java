package org.mastodon.mamut.fitting.stardist;

import cn.jimmiez.pcu.common.graphics.Octree;
import net.imglib2.util.LinAlgHelpers;

import javax.vecmath.Point3d;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Class to generate a star convex polyhedra.
 *
 * @see <a href="https://ieeexplore.ieee.org/stamp/stamp.jsp?tp=&arnumber=9093435">Star-convex Polyhedra for 3D Object Detection and Segmentation in Microscopy</a>
 * @see <a href="https://ieeexplore.ieee.org/stamp/stamp.jsp?tp=&arnumber=9093435">Measurement of areas on a sphere using Fibonacci and latitude–longitude lattices.</a>
 * @author Stefan Hahmann
 */
public class StarConvexPolyhedra
{
	private final List< double[] > lattice;

	private static final int DEFAULT_SIZE = 96;

	private static final List< double[] > DEFAULT_LATTICE = FibonacciLattices.getValues( DEFAULT_SIZE );

	private final double[] center;

	private final List< double[] > points;

	private final BoundingBox3D boundingBox;

	private final Octree octree;

	/**
	 * Creates a star convex polyhedra with the given center and distances to the points. The number of points that the polyhedra contains is determined by the number of given distances.
	 * @param center the center of the polyhedra. Must not be null. Expected order: xyz.
	 * @param distances the distances from the center to the points. Must not be null. Must contain at least 4 distances.
	 */
	public StarConvexPolyhedra( final double[] center, final List< Double > distances )
	{
		if ( center == null )
			throw new IllegalArgumentException( "center cannot be null." );
		if ( distances == null )
			throw new IllegalArgumentException( "distances cannot be null." );
		if ( distances.isEmpty() )
			throw new IllegalArgumentException( "distances cannot be empty." );
		int nPoints = distances.size();
		if ( nPoints == DEFAULT_SIZE )
			this.lattice = DEFAULT_LATTICE;
		else
			this.lattice = FibonacciLattices.getValues( nPoints );
		if ( nPoints < 4 )
			throw new IllegalArgumentException( "At least 4 distances are required." );
		octree = new Octree();
		initOctree();
		this.center = center;
		this.points = new ArrayList<>();
		for ( int i = 0; i < nPoints; i++ )
		{
			double[] point = new double[ 3 ];
			LinAlgHelpers.scale( lattice.get( i ), distances.get( i ), point );
			LinAlgHelpers.add( center, point, point );
			points.add( point );
		}

		double[] minMax = minMax();
		double[] min = new double[] { minMax[ 0 ], minMax[ 1 ], minMax[ 2 ] };
		double[] max = new double[] { minMax[ 3 ], minMax[ 4 ], minMax[ 5 ] };
		this.boundingBox = new BoundingBox3D( min, max );
	}

	private void initOctree()
	{
		List< Point3d > latticePoints = new ArrayList<>();
		this.lattice.forEach( point -> latticePoints.add( new Point3d( point[ 0 ], point[ 1 ], point[ 2 ] ) ) );
		octree.buildIndex( latticePoints );
	}

	StarConvexPolyhedra(
			final double[] center, final List< double[] > vertices, final double[] min, final double[] max, final List< double[] > lattice
	)
	{
		this.center = center;
		this.lattice = lattice;
		this.points = vertices;
		this.boundingBox = new BoundingBox3D( min, max );
		this.octree = new Octree();
		initOctree();
	}

	/**
	 * Tests if the given point is inside the star convex polyhedra and returns true if it is.<p>
	 * Workflow:
	 * <ul>
	 *     <li>Project given point on unit sphere</li>
	 *     <ol>
	 *         <li>Subtract center from point</li>
	 *         <li>Normalize point</li>
	 *         <li>Point is now on unit sphere</li>
	 *     </ol>
	 *     <li>Find the 3 nearest points to this point on the unit sphere</li>
	 *     <li>Construct a triangle from these 3 points</li>
	 *     <li>Test on which side of the triangle the point lies</li>
	 *     <li>If the point lies on the same side as the center, it is inside the polyhedra</li>
	 * </ul>
	 * @param point the point to test. Must not be null.
	 * @return true if the given point is inside the star convex polyhedra.
	 */
	public boolean contains( final double[] point )
	{
		if ( point == null )
			throw new IllegalArgumentException( "Point cannot be null." );
		if ( Arrays.equals( point, center ) )
			return true;
		List< double[] > nearestPoints = findNearestPoints( point );
		return sideOfTriangle( point, nearestPoints ) == sideOfTriangle( center, nearestPoints );
	}

	List< double[] > getLattice()
	{
		return lattice;
	}

	BoundingBox3D getBoundingBox3D()
	{
		return boundingBox;
	}

	List< double[] > getPoints()
	{
		return points;
	}

	double[] getCenter()
	{
		return center;
	}

	List< double[] > findNearestPoints( final double[] candidate )
	{
		double[] copy = new double[ 3 ];
		// project on unit sphere
		LinAlgHelpers.subtract( candidate, center, copy );
		LinAlgHelpers.normalize( copy );

		int[] indices = octree.searchNearestNeighbors( 3, new Point3d( copy[ 0 ], copy[ 1 ], copy[ 2 ] ) );

		List< double[] > result = new ArrayList<>();
		for ( int i = 0; i < 3; i++ )
		{
			result.add( points.get( indices[ i ] ) );
		}
		return result;
	}

	private static byte sideOfTriangle( final double[] point, final List< double[] > triangle )
	{
		if ( triangle.size() != 3 )
			throw new IllegalArgumentException( "Triangle must have 3 vertices, but has: " + triangle.size() );
		double[] point1 = triangle.get( 0 );
		double[] point2 = triangle.get( 1 );
		double[] point3 = triangle.get( 2 );

		double[] diff21 = new double[ 3 ];
		LinAlgHelpers.subtract( point2, point1, diff21 );
		double[] diff31 = new double[ 3 ];
		LinAlgHelpers.subtract( point3, point1, diff31 );

		double[] cross = new double[ 3 ];
		LinAlgHelpers.cross( diff21, diff31, cross );
		LinAlgHelpers.normalize( cross );
		double[] toTestPoint = new double[ 3 ];
		LinAlgHelpers.subtract( point, point1, toTestPoint );
		double dotProduct = LinAlgHelpers.dot( cross, toTestPoint );
		return ( byte ) Math.signum( dotProduct );
	}

	private double[] minMax()
	{
		double minX = Double.MAX_VALUE;
		double minY = Double.MAX_VALUE;
		double minZ = Double.MAX_VALUE;
		double maxX = Double.MIN_VALUE;
		double maxY = Double.MIN_VALUE;
		double maxZ = Double.MIN_VALUE;

		for ( double[] vector : points )
		{
			double x = vector[ 0 ];
			double y = vector[ 1 ];
			double z = vector[ 2 ];

			minX = Math.min( minX, x );
			minY = Math.min( minY, y );
			minZ = Math.min( minZ, z );

			maxX = Math.max( maxX, x );
			maxY = Math.max( maxY, y );
			maxZ = Math.max( maxZ, z );
		}

		return new double[] { minX, minY, minZ, maxX, maxY, maxZ };
	}

	class BoundingBox3D
	{
		private final double[] minPoint;

		private final double[] maxPoint;

		public BoundingBox3D( double[] minPoint, double[] maxPoint )
		{
			this.minPoint = minPoint;
			this.maxPoint = maxPoint;
		}

		public double[] getMinPoint()
		{
			return minPoint;
		}

		public double[] getMaxPoint()
		{
			return maxPoint;
		}

		public boolean contains( final double[] point )
		{
			if ( point == null )
				throw new IllegalArgumentException( "Point cannot be null." );
			if ( Arrays.equals( point, minPoint ) || Arrays.equals( point, maxPoint ) )
				return true;
			if ( Arrays.equals( point, center ) )
				return true;
			// Check if the given 3D point is inside the bounding box.
			return point[ 0 ] >= minPoint[ 0 ] && point[ 0 ] <= maxPoint[ 0 ]
					&& point[ 1 ] >= minPoint[ 1 ] && point[ 1 ] <= maxPoint[ 1 ]
					&& point[ 2 ] >= minPoint[ 2 ] && point[ 2 ] <= maxPoint[ 2 ];
		}
	}
}
