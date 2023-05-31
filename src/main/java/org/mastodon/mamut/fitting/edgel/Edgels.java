/*-
 * #%L
 * mastodon-ellipsoid-fitting
 * %%
 * Copyright (C) 2015 - 2023 Tobias Pietzsch, Jean-Yves Tinevez
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
package org.mastodon.mamut.fitting.edgel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.imglib2.algorithm.edge.Edgel;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.LinAlgHelpers;

public class Edgels
{
	/**
	 * Returns a list of those {@code edgels} that have {@code expectedCenter}
	 * in their positive half-space (towards brighter pixels).
	 *
	 * @param edgels
	 *            input edgels
	 * @param expectedCenter
	 *            expected ellipsoid center
	 * @return filtered list of edgels
	 */
	public static ArrayList< Edgel > filterEdgelsByDirection( final List< Edgel > edgels, final double[] expectedCenter )
	{
		final ArrayList< Edgel > result = new ArrayList<>();

		final double[] p = new double[ 3 ];
		final double[] x = new double[ 3 ];
		for ( final Edgel edgel : edgels )
		{
			edgel.localize( p );
			LinAlgHelpers.subtract( expectedCenter, p, x );
			if ( LinAlgHelpers.dot( x, edgel.getGradient() ) > 0 )
				result.add( edgel );
		}

		return result;
	}

	/**
	 * Returns a list of those {@code edgels} that are not occluded by other
	 * edgels closer to the {@code expectedCenter}. For each pair of edgels
	 * whose vectors towards {@code expectedCenter} have an angle less than
	 * {@code maxAngle}: If the more distant one is within {@code maxFactor} the
	 * distance to the closer one, discard it.
	 *
	 * @param edgels
	 *            input edgels
	 * @param expectedCenter
	 *            expected ellipsoid center
	 * @param maxAngle
	 *            the max angle to test.
	 * @param maxFactor
	 *            the max factor.
	 * @return a new list.
	 */
	public static ArrayList< Edgel > filterEdgelsByOcclusion(
			final List< Edgel > edgels,
			final double[] expectedCenter,
			final double maxAngle,
			final double maxFactor )
	{
		final double cos = Math.cos( maxAngle );

		final boolean[] valid = new boolean[ edgels.size() ];
		Arrays.fill( valid, true );

		final double[] p1 = new double[ 3 ];
		final double[] p2 = new double[ 3 ];
		for ( int i = 0; i < valid.length; ++i )
		{
			if ( !valid[ i ] )
				continue;

			final Edgel e1 = edgels.get( i );
			e1.localize( p1 );
			LinAlgHelpers.subtract( p1, expectedCenter, p1 );
			final double l1 = LinAlgHelpers.length( p1 );

			for ( int j = 0; j < valid.length; ++j )
			{
				if ( !valid[ j ] )
					continue;

				final Edgel e2 = edgels.get( j );
				if ( e2 == e1 )
					continue;

				e2.localize( p2 );
				LinAlgHelpers.subtract( p2, expectedCenter, p2 );
				final double l2 = LinAlgHelpers.length( p2 );

				if ( LinAlgHelpers.dot( p1, p2 ) / (l1 * l2 ) > cos )
				{
					if ( l2 > maxFactor * l1 )
						valid[ j ] = false;
					else if ( l1 > maxFactor * l2 )
					{
						valid[ i ] = false;
						break;
					}
				}
			}
		}

		final ArrayList< Edgel > result = new ArrayList<>();
		for ( int i = 0; i < valid.length; ++i )
			if ( valid[ i ] )
				result.add( edgels.get( i ) );
		return result;
	}

	public static ArrayList< Edgel > transformEdgels( final List< Edgel > edgels, final AffineTransform3D transform )
	{
		final double[] m = new double[ 3 * 3 ];
		for ( int r = 0; r < 3; ++r )
			for ( int c = 0; c < 3; ++c )
				m[ 3 * r + c ] = transform.get( r, c );
		LinAlgHelpers.invert3x3( m );
		final AffineTransform3D normalTransform = new AffineTransform3D();
		for ( int r = 0; r < 3; ++r )
			for ( int c = 0; c < 3; ++c )
				normalTransform.set( m[ 3 * r + c ], r, c );

		final ArrayList< Edgel > result = new ArrayList<>();

		final double[] p = new double[ 3 ];
		final double[] tp = new double[ 3 ];

		final double[] n = new double[ 3 ];
		final double[] tn = new double[ 3 ];

		for ( final Edgel edgel : edgels )
		{
			LinAlgHelpers.scale( edgel.getGradient(), edgel.getMagnitude(), n );
			normalTransform.apply( n, tn );
			final double mag = LinAlgHelpers.length( tn );
			LinAlgHelpers.scale( tn, 1.0 / mag, tn );

			edgel.localize( p );
			transform.apply( p, tp );

			result.add( new Edgel( tp, tn, mag ) );
		}

		return result;
	}
}
