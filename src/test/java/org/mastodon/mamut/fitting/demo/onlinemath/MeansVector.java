package org.mastodon.mamut.fitting.demo.onlinemath;

public class MeansVector
{
	private final Mean[] means;

	public MeansVector( int dimensions )
	{
		means = new Mean[ dimensions ];
	}

	public void addValue( int[] x )
	{
		for ( int i = 0; i < x.length; i++ )
		{
			if ( means[ i ] == null )
				means[ i ] = new Mean();
			means[ i ].addValue( x[ i ] );
		}
	}

	public double[] get()
	{
		double[] result = new double[ means.length ];
		for ( int i = 0; i < means.length; i++ )
			result[ i ] = means[ i ].get();
		return result;
	}
}
