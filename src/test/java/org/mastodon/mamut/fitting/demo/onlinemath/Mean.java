package org.mastodon.mamut.fitting.demo.onlinemath;

public class Mean
{
	private long sum = 0;

	private int n;

	public void addValue( long x )
	{
		n++;
		sum += x;
	}

	public double get()
	{
		return ( double ) sum / n;
	}
}
