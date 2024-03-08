package org.mastodon.mamut.fitting.demo.onlinemath;

/**
 * Computes the covariance matrix for a series of coordinates using the online algorithm.
 * @see <a href="https://en.wikipedia.org/wiki/Algorithms_for_calculating_variance#Online">Algorithms for calculating variance</a>
 */
public class CovarianceMatrix
{
	private final Covariance[][] covariances;

	public CovarianceMatrix( int dimensions )
	{
		covariances = new Covariance[ dimensions ][ dimensions ];
	}

	public void addValue( int[] x )
	{
		if ( x.length != covariances.length )
			throw new IllegalArgumentException( "Input vector has wrong dimension." );
		for ( int i = 0; i < x.length; i++ )
		{
			for ( int j = i; j < x.length; j++ )
			{
				if ( covariances[ i ][ j ] == null )
					covariances[ i ][ j ] = new Covariance();
				covariances[ i ][ j ].addValue( x[ i ], x[ j ] );
				if ( i != j )
					covariances[ j ][ i ] = covariances[ i ][ j ];
			}
		}
	}

	public double[][] get()
	{
		double[][] result = new double[ covariances.length ][ covariances.length ];
		for ( int i = 0; i < covariances.length; i++ )
			for ( int j = 0; j < covariances.length; j++ )
				result[ i ][ j ] = covariances[ i ][ j ].get();
		return result;
	}
}
