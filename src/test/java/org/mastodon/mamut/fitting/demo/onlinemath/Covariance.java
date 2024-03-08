package org.mastodon.mamut.fitting.demo.onlinemath;

/**
 * Computes the covariance for two variables using an online algorithm.
 * @see <a href="https://en.wikipedia.org/wiki/Algorithms_for_calculating_variance#Online">Algorithms for calculating variance</a>
 */
public class Covariance
{
	private double meanX = 0;

	private double meanY = 0;

	private double c = 0;

	private int n = 0;

	public void addValue( double x, double y )
	{
		n++;
		double dx = x - meanX;
		meanX += dx / n;
		meanY += ( y - meanY ) / n;
		c += dx * ( y - meanY );
	}

	public double get()
	{
		return c / ( n - 1 );
	}
}
