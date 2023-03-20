package org.mastodon.mamut.fitting;

import bdv.viewer.ViewerOptions;
import ij.ImagePlus;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.display.imagej.ImgToVirtualStack;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.util.LinAlgHelpers;
import org.mastodon.views.bdv.SharedBigDataViewerData;

import javax.annotation.Nonnull;
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
	 * Renders the density function of a multivariate normal distribution into an image.
	 * @see <a href="https://en.wikipedia.org/wiki/Multivariate_normal_distribution">Wikipedia Multivariate normal distribution</a>
	 * @param center center of the distribution
	 * @param cov covariance matrix of the distribution
	 * @return the image
	 *
	 */
	@Nonnull
	public static Img< FloatType > renderMultivariateNormalDistribution( double[] center, double[][] cov )
	{
		Img< FloatType > image = ArrayImgs.floats( 100, 100, 100 );
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
			LinAlgHelpers.subtract( coord, center, coord );
			sigma.applyInverse( out, coord );
			double value = Math.exp( -scalarProduct( coord, out ) );
			pixel.setReal( 1000 * value );
		} );
		return image;
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
