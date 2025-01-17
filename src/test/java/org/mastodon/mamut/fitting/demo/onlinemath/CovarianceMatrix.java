/*-
 * #%L
 * mastodon-ellipsoid-fitting
 * %%
 * Copyright (C) 2015 - 2025 Tobias Pietzsch, Jean-Yves Tinevez
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
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
