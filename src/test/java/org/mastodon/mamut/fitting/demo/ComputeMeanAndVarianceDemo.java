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
	private static final int PIXEL_VALUE = 42;

	public static void main( String[] args )
	{
		double[] center = { 40, 50, 60 };
		double[][] givenCovariance = {
				{ 400, 20, -10 },
				{ 20, 200, 30 },
				{ -10, 30, 100 }
		};

		Img< FloatType > image = generateExampleImage( center, givenCovariance );
		double[] mean = computeMean( image );
		double[][] computedCovariance = computeCovariance( image, mean );

		System.out.println( "Given center: " + Arrays.toString( center ) );
		System.out.println( "Computed mean: " + Arrays.toString( mean ) );
		System.out.println( "Given covariance: " + Arrays.deepToString( givenCovariance ) );
		System.out.println( "Computed covariance: " + Arrays.deepToString( computedCovariance ) );

		Model model = new Model();
		model.getGraph().addVertex().init( 0, mean, computedCovariance );
		DemoUtils.showBdvWindow( DemoUtils.wrapAsAppModel( image, model ) );
	}

	/**
	 * Returns an example image with a single ellipsoid. Pixel values are 0 or 42.
	 * 0 is background, 42 is the ellipsoid.
	 */
	private static Img< FloatType > generateExampleImage( double[] center, double[][] cov )
	{
		Img< FloatType > image = ArrayImgs.floats( 100, 100, 100 );
		MultiVariantNormalDistributionRenderer.renderMultivariateNormalDistribution( center, cov, image );
		LoopBuilder.setImages( image ).forEachPixel( pixel -> {
			if ( pixel.get() > 500 )
				pixel.set( PIXEL_VALUE );
			else
				pixel.set( 0 );
		} );
		return image;
	}

	/**
	 * Computes the mean position of the pixels whose value equals 42.
	 */
	private static double[] computeMean( Img< FloatType > image )
	{
		Cursor< FloatType > cursor = image.cursor();
		double[] sum = new double[ 3 ];
		double[] position = new double[ 3 ];
		long counter = 0;
		while ( cursor.hasNext() )
			if ( cursor.next().get() == PIXEL_VALUE )
			{
				cursor.localize( position );
				LinAlgHelpers.add( sum, position, sum );
				counter++;
			}
		LinAlgHelpers.scale( sum, 1. / counter, sum );
		return sum;
	}

	/**
	 * Computes the covariance matrix of the pixels whose value equals 42.
	 */
	private static double[][] computeCovariance( Img< FloatType > image, double[] mean )
	{
		Cursor< FloatType > cursor = image.cursor();
		long counter = 0;
		double[] position = new double[ 3 ];
		double[][] covariance = new double[ 3 ][ 3 ];
		cursor.reset();
		while ( cursor.hasNext() )
			if ( cursor.next().get() == PIXEL_VALUE )
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

	private static void scale( double[][] covariance, double factor )
	{
		for ( int i = 0; i < 3; i++ )
			for ( int j = 0; j < 3; j++ )
				covariance[ i ][ j ] *= factor;
	}
}
