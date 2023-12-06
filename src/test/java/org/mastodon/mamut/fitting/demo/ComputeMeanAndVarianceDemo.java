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
package org.mastodon.mamut.fitting.demo;

import net.imglib2.Cursor;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.LinAlgHelpers;

import org.mastodon.mamut.fitting.util.DemoUtils;
import org.mastodon.mamut.fitting.util.MultiVariantNormalDistributionRenderer;
import org.mastodon.mamut.model.Model;

import java.util.Arrays;

/**
 * Computing the mean position and covariance matrix for a given segmented
 * region of an image is an easy way to get good ellipsoid parameters for
 * that segment.
 * <p>
 * Here is an example of how to do that.
 */
public class ComputeMeanAndVarianceDemo
{

	public static void main( String[] args )
	{
		double[] center = { 40, 50, 60 };
		double[][] givenCovariance = {
				{ 400, 20, -10 },
				{ 20, 200, 30 },
				{ -10, 30, 100 }
		};

		long[] dimensions = { 100, 100, 100 };
		int background = 0;
		int pixelValue = 1;
		Img< FloatType > image = generateExampleImage( center, givenCovariance, dimensions, background, pixelValue );
		double[] mean = computeMean( image, pixelValue );
		double[][] computedCovariance = computeCovariance( image, mean, pixelValue );

		System.out.println( "Given center: " + Arrays.toString( center ) );
		System.out.println( "Computed mean: " + Arrays.toString( mean ) );
		System.out.println( "Given covariance: " + Arrays.deepToString( givenCovariance ) );
		System.out.println( "Computed covariance: " + Arrays.deepToString( computedCovariance ) );

		Model model = new Model();
		model.getGraph().addVertex().init( 0, mean, computedCovariance );
		DemoUtils.showBdvWindow( DemoUtils.wrapAsAppModel( image, model ) );
	}

	/**
	 * Returns an example image with a single ellipsoid.
	 *
	 * @param center center of the ellipsoid
	 * @param cov covariance matrix of the ellipsoid
	 * @param dimensions dimensions of the image
	 * @param background value of the background
	 * @param pixelValue value of the ellipsoid
	 */
	private static Img< FloatType > generateExampleImage(
			final double[] center, final double[][] cov, final long[] dimensions, final int background, final int pixelValue
	)
	{
		Img< FloatType > image = ArrayImgs.floats( dimensions );
		MultiVariantNormalDistributionRenderer.renderMultivariateNormalDistribution( center, cov, image );
		LoopBuilder.setImages( image ).forEachPixel( pixel -> {
			if ( pixel.get() > 500 )
				pixel.set( pixelValue );
			else
				pixel.set( background );
		} );
		return image;
	}

	/**
	 * Computes the mean position of the pixels whose value equals the given {@code pixelValue}.
	 *
	 * @param image the image
	 * @param pixelValue the pixel value
	 * @return the mean position
	 */
	private static double[] computeMean( final Img< FloatType > image, final int pixelValue )
	{
		Cursor< FloatType > cursor = image.cursor();
		double[] sum = new double[ 3 ];
		double[] position = new double[ 3 ];
		long counter = 0;
		while ( cursor.hasNext() )
			if ( cursor.next().get() == pixelValue )
			{
				cursor.localize( position );
				LinAlgHelpers.add( sum, position, sum );
				counter++;
			}
		LinAlgHelpers.scale( sum, 1. / counter, sum );
		return sum;
	}

	/**
	 * Computes the covariance matrix of the pixels whose value equals the given {@code pixelValue}.
	 *
	 * @param image the image
	 * @param mean the mean position
	 * @param pixelValue the pixel value
	 */
	private static double[][] computeCovariance( final Img< FloatType > image, final double[] mean, final int pixelValue )
	{
		Cursor< FloatType > cursor = image.cursor();
		long counter = 0;
		double[] position = new double[ 3 ];
		double[][] covariance = new double[ 3 ][ 3 ];
		cursor.reset();
		while ( cursor.hasNext() )
			if ( cursor.next().get() == pixelValue )
			{
				cursor.localize( position );
				LinAlgHelpers.subtract( position, mean, position );
				for ( int i = 0; i < 3; i++ )
					for ( int j = 0; j < 3; j++ )
						covariance[ i ][ j ] += position[ i ] * position[ j ];
				counter++;
			}
		scale( covariance, 5. / counter ); // I don't know why the factor 5 is needed. But it works.
		return covariance;
	}

	private static void scale( final double[][] covariance, final double factor )
	{
		for ( int i = 0; i < 3; i++ )
			for ( int j = 0; j < 3; j++ )
				covariance[ i ][ j ] *= factor;
	}
}
