package org.mastodon.mamut.fitting.regression;

import org.ejml.simple.SimpleMatrix;

import java.util.Random;

public class EllipsoidFromLinearRegressionDemo
{
	public static void main( String[] args )
	{
		doFit();
	}

	private static String withOp( double in )
	{
		if ( in < 0 )
		{
			return String.format( "- %.3f", -in );
		}
		else
		{
			return String.format( "+ %.3f", in );
		}
	}

	private static void doFit()
	{
		int n = 12;
		SimpleMatrix points = SimpleMatrix.random( n, 3, -1, 1, new Random() );
		SimpleMatrix lrX = new SimpleMatrix( n, 9 );

		for ( int row = 0; row < n; row++ )
		{

			double x = points.get( row, 0 );
			double y = points.get( row, 1 );
			double z = points.get( row, 2 );

			lrX.set( row, 0, x * x );
			lrX.set( row, 1, y * y );
			lrX.set( row, 2, z * z );
			lrX.set( row, 3, x * y );
			lrX.set( row, 4, x * z );
			lrX.set( row, 5, y * z );
			lrX.set( row, 6, x );
			lrX.set( row, 7, y );
			lrX.set( row, 8, z );
		}
		SimpleMatrix lrY = new SimpleMatrix( n, 1 );
		lrY.set( 1 );

		SimpleMatrix params = new LinearRegression().fit( lrX, lrY ).getParameters();

		System.out.printf( "%.3fx^2 %sy^2 %sz^2 %sx y %sx z %sy z %sx %sy %sz - 1 == 0",
				params.get( 0 ), withOp( params.get( 1 ) ), withOp( params.get( 2 ) ),
				withOp( params.get( 3 ) ), withOp( params.get( 4 ) ), withOp( params.get( 5 ) ),
				withOp( params.get( 6 ) ), withOp( params.get( 7 ) ), withOp( params.get( 8 ) )
		);
	}
}
