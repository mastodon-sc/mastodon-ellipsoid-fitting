package org.mastodon.mamut.fitting;

import java.util.HashMap;
import java.util.Objects;

import bdv.viewer.SourceAndConverter;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.display.imagej.ImgToVirtualStack;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.util.LinAlgHelpers;

import org.jetbrains.annotations.NotNull;
import org.mastodon.mamut.MamutAppModel;
import org.mastodon.mamut.MamutViewBdv;
import org.mastodon.mamut.fitting.ellipsoid.Ellipsoid;
import org.mastodon.mamut.fitting.ellipsoid.FitEllipsoid;
import org.mastodon.mamut.model.Model;
import org.mastodon.mamut.model.Spot;
import org.mastodon.mamut.plugin.MamutPlugins;
import org.mastodon.ui.coloring.feature.FeatureColorModeManager;
import org.mastodon.ui.keymap.Keymap;
import org.mastodon.ui.keymap.KeymapManager;
import org.mastodon.views.bdv.SharedBigDataViewerData;
import org.mastodon.views.bdv.overlay.ui.RenderSettingsManager;
import org.mastodon.views.grapher.display.style.DataDisplayStyleManager;
import org.mastodon.views.trackscheme.display.style.TrackSchemeStyleManager;
import org.scijava.ui.behaviour.KeyPressedManager;
import org.scijava.ui.behaviour.util.Actions;

import bdv.viewer.ViewerOptions;
import ij.ImagePlus;

import javax.annotation.Nonnull;

/**
 * Renders a multivariant normal distribution overlaid with the
 * respective ellipsoid in Big Data Viewer.
 */
public class BlobRenderingDemo
{

	public static void main( String... args )
	{
		// TODO randomize
		// cov
		double[][] cov = {
				{ 400, 100, 200 },
				{ 100, 400, 300 },
				{ 200, 300, 400 }
		};
		// TODO raster
		double[] center = { 50, 50, 50 };
		Img< FloatType > image = renderMultivariateNormalDistribution( center, cov );
		SharedBigDataViewerData sbdvd = asSharedBdvDataXyz( image );
		Model model = new Model();
		// init generates a spot
		model.getGraph().addVertex().init( 0, center, cov );

		Keymap keymap = new Keymap();
		MamutAppModel appModel =
				new MamutAppModel( model, sbdvd, new KeyPressedManager(), new TrackSchemeStyleManager(), new DataDisplayStyleManager(),
						new RenderSettingsManager(),
						new FeatureColorModeManager(), new KeymapManager(), new MamutPlugins(
								keymap ),
						new Actions( keymap.getConfig() ) );
		showBdvWindow( appModel );

		{
			final AffineTransform3D sourceToGlobal = new AffineTransform3D();
			Spot spot = model.getGraph().vertices().iterator().next();
			final SourceAndConverter< ? > source = sbdvd.getSources().get( 0 );
			@SuppressWarnings( { "unchecked", "rawtypes" } )
			Ellipsoid ellipsoid = FitEllipsoid.getFittedEllipsoid( spot, ( SourceAndConverter ) source, sourceToGlobal, appModel, true );
			if ( ellipsoid == null )
			{
				System.out.println( "No ellipsoid could be fitted." );
				return;
			}
			ellipsoid.getCovariance();
			ellipsoid.getCenter();
		}
	}

	private static < T extends RealType< T > > void showBdvWindow( @Nonnull MamutAppModel appModel )
	{
		MamutViewBdv ts = new MamutViewBdv( appModel, new HashMap<>() );
		ts.getFrame().setVisible( true );
	}

	@Nonnull
	private static SharedBigDataViewerData asSharedBdvDataXyz( Img< FloatType > image1 )
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
	private static Img< FloatType > renderMultivariateNormalDistribution( double[] center, double[][] cov )
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
			pixel.setReal( value );
		} );
		return image;
	}

	private static double scalarProduct( double[] a, double[] b )
	{
		// TODO does this method exist already in imglib2 somewhere?
		// SortTreeUtils
		return a[ 0 ] * b[ 0 ] + a[ 1 ] * b[ 1 ] + a[ 2 ] * b[ 2 ];
	}
}
