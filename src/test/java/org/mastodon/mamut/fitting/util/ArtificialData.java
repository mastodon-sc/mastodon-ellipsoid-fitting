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
package org.mastodon.mamut.fitting.util;

import java.util.Random;

import org.mastodon.collection.RefObjectMap;
import org.mastodon.collection.ref.RefObjectHashMap;
import org.mastodon.mamut.ProjectModel;
import org.mastodon.mamut.fitting.MinimalProjectModel;
import org.mastodon.mamut.fitting.ellipsoid.Ellipsoid;
import org.mastodon.mamut.model.Model;
import org.mastodon.mamut.model.Spot;
import org.scijava.Context;

import net.imglib2.Interval;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.util.LinAlgHelpers;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

/**
 * Renders a grid of ellipsoids in 3D, and wraps the result in a {@link ProjectModel}.
 */
public class ArtificialData
{

	private final Random random = new Random( 1 );

	private final int size = 80;

	private final int columns = 4;

	private final int numberOfSpots = columns * columns * columns;

	private final Context context;

	private final MinimalProjectModel minimalProjectModel;

	private final RefObjectMap< Spot, Ellipsoid > ellipsoids;

	public ArtificialData( final Context context )
	{
		this.context = context;
		final Model model = new Model();
		ellipsoids = new RefObjectHashMap<>( model.getGraph().vertices().getRefPool(), numberOfSpots );
		final Img< FloatType > image = ArrayImgs.floats( columns * size, columns * size, columns * size );
		final Spot ref = model.getGraph().vertexRef();
		for ( int i = 0; i < columns; i++ )
			for ( int j = 0; j < columns; j++ )
				for ( int k = 0; k < columns; k++ )
				{
					final double[] center = { i * size + size / 2, j * size + size / 2, k * size + size / 2 };
					final Interval interval = Intervals.createMinSize( i * size, j * size, k * size, size, size, size );
					final Ellipsoid ellipsoid = randomizedEllipsoid( center );
					final Spot spot = model.getGraph().addVertex(ref);
					spot.init( 0, center, 10 );
					ellipsoids.put( spot, ellipsoid );
					drawSpot( image, interval, ellipsoid );
				}
		minimalProjectModel = DemoUtils.wrapAsMinimalModel( image, model );
		selectAllVerticies();
	}

	private static void drawSpot( final Img< FloatType > image, final Interval interval, final Ellipsoid ellipsoid )
	{
		final IntervalView< FloatType > crop = Views.interval( image, interval );
		MultiVariantNormalDistributionRenderer.renderMultivariateNormalDistribution(
				ellipsoid.getCenter(), ellipsoid.getCovariance(),
				crop );
	}

	private void selectAllVerticies()
	{
		for ( final Spot vertex : minimalProjectModel.getModel().getGraph().vertices() )
			minimalProjectModel.getSelectionModel().setSelected( vertex, true );
	}

	private Ellipsoid randomizedEllipsoid( final double[] center )
	{
		final double[][] inputCovarianceMatrix = randomizedCovarianceMatrix();
		final double[] randomizedCenter = randomizeCenter( center );
		return new Ellipsoid( randomizedCenter, inputCovarianceMatrix, null, null, null );
	}

	private double[] randomizeCenter( final double[] center )
	{
		final double[] offset = {
				randomDouble(- 5, 5),
				randomDouble(- 5, 5),
				randomDouble(- 5, 5)
		};
		final double[] randomizedCenter = new double[ 3 ];
		LinAlgHelpers.add( center, offset, randomizedCenter );
		return randomizedCenter;
	}

	private double[][] randomizedCovarianceMatrix()
	{
		final AffineTransform3D a = new AffineTransform3D();
		final double minAxis = 8;
		final double maxAxis = 16;
		a.scale( randomDouble( minAxis, maxAxis ), randomDouble( minAxis, maxAxis ), randomDouble( minAxis, maxAxis ) );
		a.rotate( 0, randomDouble( 0, 2 * Math.PI ) );
		a.rotate( 1, randomDouble( 0, 2 * Math.PI ) );
		a.rotate( 2, randomDouble( 0, 2 * Math.PI ) );
		final AffineTransform3D b = transposed( a );
		a.concatenate( b );
		final double[][] matrix = new double[ 3 ][ 3 ];
		for ( int i = 0; i < 3; i++ )
			for ( int j = 0; j < 3; j++ )
				matrix[ i ][ j ] = a.get( i, j );
		return matrix;
	}

	private double randomDouble( final double min, final double max )
	{
		return random.nextDouble() * ( max - min ) + min;
	}

	private static AffineTransform3D transposed( final AffineTransform3D transform )
	{
		final AffineTransform3D r = new AffineTransform3D();
		for ( int i = 0; i < 3; i++ )
			for ( int j = 0; j < 3; j++ )
				r.set( transform.get( i, j ), j, i );
		return r;
	}

	public ProjectModel getAppModel()
	{
		return ProjectModel.create( context, minimalProjectModel.getModel(), minimalProjectModel.getSharedBdvData(), null );
	}

	public MinimalProjectModel getMinimalProjectModel()
	{
		return minimalProjectModel;
	}

	public RefObjectMap< Spot, Ellipsoid> getExpectedEllipsoids()
	{
		return ellipsoids;
	}
}
