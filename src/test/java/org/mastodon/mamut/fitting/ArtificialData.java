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
package org.mastodon.mamut.fitting;

import java.util.Random;

import net.imglib2.Interval;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.util.LinAlgHelpers;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import org.mastodon.collection.RefObjectMap;
import org.mastodon.collection.ref.RefObjectHashMap;
import org.mastodon.mamut.MamutAppModel;
import org.mastodon.mamut.fitting.ellipsoid.Ellipsoid;
import org.mastodon.mamut.model.Model;
import org.mastodon.mamut.model.Spot;

/**
 * Renders a grid of ellipsoids in 3D, and wraps the result in a {@link MamutAppModel}.
 */
public class ArtificialData
{

	private final Random random = new Random( 1 );

	private final int size = 80;

	private final int columns = 4;

	private final int numberOfSpots = columns * columns * columns;

	private final MamutAppModel appModel;

	private final RefObjectMap< Spot, Ellipsoid > ellipsoids;

	public ArtificialData()
	{
		Model model = new Model();
		ellipsoids = new RefObjectHashMap<>( model.getGraph().vertices().getRefPool(), numberOfSpots );
		Img< FloatType > image = ArrayImgs.floats( columns * size, columns * size, columns * size );
		Spot ref = model.getGraph().vertexRef();
		for ( int i = 0; i < columns; i++ )
			for ( int j = 0; j < columns; j++ )
				for ( int k = 0; k < columns; k++ )
				{
					double[] center = { i * size + size / 2, j * size + size / 2, k * size + size / 2 };
					Interval interval = Intervals.createMinSize( i * size, j * size, k * size, size, size, size );
					Ellipsoid ellipsoid = randomizedEllipsoid( center );
					Spot spot = model.getGraph().addVertex(ref);
					spot.init( 0, center, 10 );
					ellipsoids.put( spot, ellipsoid );
					drawSpot( image, interval, ellipsoid );
				}

		appModel = DemoUtils.wrapAsAppModel( image, model );
		selectAllVerticies();
	}

	private static void drawSpot( Img< FloatType > image, Interval interval, Ellipsoid ellipsoid )
	{
		IntervalView< FloatType > crop = Views.interval( image, interval );
		MultiVariantNormalDistributionRenderer.renderMultivariateNormalDistribution(
				ellipsoid.getCenter(), ellipsoid.getCovariance(),
				crop );
	}

	private void selectAllVerticies()
	{
		for ( Spot vertex : appModel.getModel().getGraph().vertices() )
			appModel.getSelectionModel().setSelected( vertex, true );
	}

	private Ellipsoid randomizedEllipsoid( double[] center )
	{
		double[][] inputCovarianceMatrix = randomizedCovarianceMatrix();
		double[] randomizedCenter = randomizeCenter( center );
		return new Ellipsoid( randomizedCenter, inputCovarianceMatrix, null, null, null );
	}

	private double[] randomizeCenter( double[] center )
	{
		double[] offset = {
				randomDouble(- 5, 5),
				randomDouble(- 5, 5),
				randomDouble(- 5, 5)
		};
		double[] randomizedCenter = new double[ 3 ];
		LinAlgHelpers.add( center, offset, randomizedCenter );
		return randomizedCenter;
	}

	private double[][] randomizedCovarianceMatrix()
	{
		AffineTransform3D a = new AffineTransform3D();
		double minAxis = 8;
		double maxAxis = 16;
		a.scale( randomDouble( minAxis, maxAxis ), randomDouble( minAxis, maxAxis ), randomDouble( minAxis, maxAxis ) );
		a.rotate( 0, randomDouble( 0, 2 * Math.PI ) );
		a.rotate( 1, randomDouble( 0, 2 * Math.PI ) );
		a.rotate( 2, randomDouble( 0, 2 * Math.PI ) );
		AffineTransform3D b = transposed( a );
		a.concatenate( b );
		double[][] matrix = new double[ 3 ][ 3 ];
		for ( int i = 0; i < 3; i++ )
			for ( int j = 0; j < 3; j++ )
				matrix[ i ][ j ] = a.get( i, j );
		return matrix;
	}

	private double randomDouble( double min, double max )
	{
		return random.nextDouble() * ( max - min ) + min;
	}

	private static AffineTransform3D transposed( AffineTransform3D transform )
	{
		AffineTransform3D r = new AffineTransform3D();
		for ( int i = 0; i < 3; i++ )
			for ( int j = 0; j < 3; j++ )
				r.set( transform.get( i, j ), j, i );
		return r;
	}

	public MamutAppModel getAppModel()
	{
		return appModel;
	}

	public RefObjectMap< Spot, Ellipsoid> getExpectedEllipsoids()
	{
		return ellipsoids;
	}
}
