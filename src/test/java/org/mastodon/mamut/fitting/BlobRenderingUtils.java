package org.mastodon.mamut.fitting;

import bdv.viewer.ViewerOptions;
import ij.ImagePlus;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImgToVirtualStack;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.util.LinAlgHelpers;
import org.mastodon.views.bdv.SharedBigDataViewerData;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

public class BlobRenderingUtils
{

	@Nonnull
	public static SharedBigDataViewerData asSharedBdvDataXyz( Img< FloatType > image1 )
	{
		ImagePlus image = ImgToVirtualStack.wrap( new ImgPlus<>( image1, "image", new AxisType[] { Axes.X, Axes.Y, Axes.Z } ) );
		return Objects.requireNonNull( SharedBigDataViewerData.fromImagePlus( image, new ViewerOptions(), () -> {} ) );
	}

	/**
	 * Renders the density function of a multivariate normal distribution into a given image.
	 * @see <a href="https://en.wikipedia.org/wiki/Multivariate_normal_distribution">Wikipedia Multivariate normal distribution</a>
	 * @param center center of the distribution
	 * @param cov covariance matrix of the distribution (must be symmetric and positive definite)
	 * @param image the image to render into (image is a cube)
	 *
	 */
	public static void renderMultivariateNormalDistribution( double[] center, double[][] cov, @Nonnull Img< FloatType > image )
	{
		renderMultivariateNormalDistribution( center, cov, null, image );
	}

	/**
	 * Renders the density function of a multivariate normal distribution into a given image.
	 * @see <a href="https://en.wikipedia.org/wiki/Multivariate_normal_distribution">Wikipedia Multivariate normal distribution</a>
	 * @param center center of the distribution
	 * @param cov covariance matrix of the distribution (must be symmetric and positive definite)
	 * @param size size of the distribution (in pixels)
	 * @param image the image to render into (image is a cube)
	 *
	 */
	public static void renderMultivariateNormalDistribution( double[] center, double[][] cov, @Nullable Double size,
			@Nonnull Img< FloatType > image )
	{
		AffineTransform3D sigma = new AffineTransform3D();
		sigma.set(
				cov[ 0 ][ 0 ], cov[ 0 ][ 1 ], cov[ 0 ][ 2 ], 0,
				cov[ 1 ][ 0 ], cov[ 1 ][ 1 ], cov[ 1 ][ 2 ], 0,
				cov[ 2 ][ 0 ], cov[ 2 ][ 1 ], cov[ 2 ][ 2 ], 0
		);
		double[] coord = new double[ 3 ];
		double[] out = new double[ 3 ];
		LoopBuilder.setImages( Intervals.positions( image ), image ).forEachPixel( ( position, pixel ) -> {
			position.localize( coord );
			if ( size != null && Math.abs( center[ 0 ] - coord[ 0 ] ) > size / 2 )
				return;
			if ( size != null && Math.abs( center[ 1 ] - coord[ 1 ] ) > size / 2 )
				return;
			if ( size != null && Math.abs( center[ 2 ] - coord[ 2 ] ) > size / 2 )
				return;
			LinAlgHelpers.subtract( coord, center, coord );
			sigma.applyInverse( out, coord );
			// leave out the 1 / (sqrt( ( 2 * pi ) ^ 3 * det( cov )) factor to make the image more visible
			double value = Math.exp( -0.5 * scalarProduct( coord, out ) );
			pixel.setReal( 1000 * value );
		} );
	}

	/**
	 * Computes the scalar product of two vectors.
	 * @param a vector a
	 * @param b vector b
	 * @return the scalar product
	 */
	public static double scalarProduct( double[] a, double[] b )
	{
		if ( a == null || b == null )
			throw new IllegalArgumentException( "Vectors must not be null." );
		if ( a.length != b.length )
			throw new IllegalArgumentException( "Vectors must have the same length." );
		if ( a.length == 0 )
			throw new IllegalArgumentException( "Vectors must have at least one element." );
		return a[ 0 ] * b[ 0 ] + a[ 1 ] * b[ 1 ] + a[ 2 ] * b[ 2 ];
	}
}
