/*-
 * #%L
 * mastodon-ellipsoid-fitting
 * %%
 * Copyright (C) 2015 - 2024 Tobias Pietzsch, Jean-Yves Tinevez
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
package org.mastodon.mamut.fitting.ui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import bdv.util.BdvOverlay;
import net.imglib2.algorithm.edge.Edgel;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.LinAlgHelpers;

public class EdgelsOverlay extends BdvOverlay
{
	private final List< Edgel > edgels;

	private final double magnitudeScale;

	private Color col;

	public EdgelsOverlay(
			final List< Edgel > edgels,
			final double magnitudeScale
	)
	{
		this.edgels = edgels;
		this.magnitudeScale = magnitudeScale;
	}

	@Override
	protected void draw( final Graphics2D g )
	{
		final boolean showNormal = true;

		col = new Color( info.getColor().get() );

		final AffineTransform3D transform = new AffineTransform3D();
		info.getViewerTransform( transform );
		transform.concatenate( sourceTransform );

		final double[] lPos = new double[ 3 ];
		final double[] gPos = new double[ 3 ];
		final double[] nlPos = new double[ 3 ];
		final double[] ngPos = new double[ 3 ];
		for ( final Edgel e : edgels )
		{
			e.localize( lPos );

			transform.apply( lPos, gPos );
			final double size = getPointSize( gPos );
			final int x = ( int ) ( gPos[ 0 ] - 0.5 * size );
			final int y = ( int ) ( gPos[ 1 ] - 0.5 * size );
			final int w = ( int ) size;

			g.setColor( getColor( gPos, e ) );
			g.fillOval( x, y, w, w );

			if ( showNormal )
			{
				final double[] gradient = e.getGradient();
				final double magnitude = e.getMagnitude();

				LinAlgHelpers.scale( gradient, magnitude * magnitudeScale, nlPos );
				LinAlgHelpers.add( lPos, nlPos, nlPos );
				transform.apply( nlPos, ngPos );

				final int x1 = ( int ) ( gPos[ 0 ] );
				final int y1 = ( int ) ( gPos[ 1 ] );
				final int x2 = ( int ) ( ngPos[ 0 ] );
				final int y2 = ( int ) ( ngPos[ 1 ] );
				g.drawLine( x1, y1, x2, y2 );
			}
		}
	}

	/**
	 * screen pixels [x,y,z]
	 **/
	private Color getColor( final double[] gPos, final Edgel edgel )
	{
		int alpha = 255 - ( int ) Math.round( Math.abs( 3 * gPos[ 2 ] ) );

		if ( alpha < 10 )
			alpha = 10;

		if ( costs == null )
			return new Color( col.getRed(), col.getGreen(), col.getBlue(), alpha );

		final int red = ( int ) ( 255 * costs.get( edgel ) / maxCost );
		final int green = 255 - red;

		return new Color( red, green, 0, alpha );

	}

	private double getPointSize( final double[] gPos )
	{
		if ( Math.abs( gPos[ 2 ] ) < 3 )
			return 5.0;
		else
			return 3.0;
	}

	private Map< Edgel, Double > costs;

	private double maxCost;

	public void setCosts( final Map< Edgel, Double > costs )
	{
		this.costs = costs;
		maxCost = 0;
		for ( final Entry< Edgel, Double > entry : costs.entrySet() )
			maxCost = Math.max( entry.getValue(), maxCost );
	}
}
