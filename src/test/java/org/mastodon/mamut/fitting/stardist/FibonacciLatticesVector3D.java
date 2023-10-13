package org.mastodon.mamut.fitting.stardist;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import java.util.ArrayList;
import java.util.List;

/**
 * Class to compute the points of a spherical Fibonacci lattice
 *
 * @see <a href="https://ieeexplore.ieee.org/stamp/stamp.jsp?tp=&arnumber=9093435">Star-convex Polyhedra for 3D Object Detection and Segmentation in Microscopy</a>
 * @see <a href="https://ieeexplore.ieee.org/stamp/stamp.jsp?tp=&arnumber=9093435">Measurement of areas on a sphere using Fibonacci and latitude–longitude lattices.</a>
 */
public class FibonacciLatticesVector3D extends FibonacciLattices
{
	private FibonacciLatticesVector3D()
	{
		super();
	}

	private static final double PHI = ( 1 + Math.sqrt( 5 ) ) / 2;

	/**
	 * Returns the points of a spherical Fibonacci lattice with n points
	 *
	 * @param n number of points
	 * @return points of a spherical Fibonacci lattice with n points.
	 * @see <a href="https://ieeexplore.ieee.org/stamp/stamp.jsp?tp=&arnumber=9093435">Measurement of areas on a sphere using Fibonacci and latitude–longitude lattices.</a> Section 2.1, 3656
	 */
	public static List< Vector3D > getPoints( int n )
	{
		List< Vector3D > points = new ArrayList<>();
		for ( int k = 0; k < n; k++ )
		{
			double[] zyx = getZYX( k, n );
			points.add( new Vector3D( zyx[ 2 ], zyx[ 1 ], zyx[ 0 ] ) );
		}
		return points;
	}

	static double[] getZYX( double k, double n )
	{
		double[] zyx = new double[ 3 ];
		double z = -1 + 2.0 * k / ( n - 1 );
		zyx[ 0 ] = z;
		zyx[ 1 ] = Math.sqrt( 1 - z * z ) * Math.sin( 2 * Math.PI * ( 1 - 1 / PHI ) * k );
		zyx[ 2 ] = Math.sqrt( 1 - z * z ) * Math.cos( 2 * Math.PI * ( 1 - 1 / PHI ) * k );
		return zyx;
	}
}
