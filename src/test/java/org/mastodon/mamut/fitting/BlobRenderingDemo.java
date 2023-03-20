package org.mastodon.mamut.fitting;

import java.util.Arrays;
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
import org.mastodon.views.bdv.overlay.util.JamaEigenvalueDecomposition;
import org.mastodon.views.grapher.display.style.DataDisplayStyleManager;
import org.mastodon.views.trackscheme.display.style.TrackSchemeStyleManager;
import org.scijava.ui.behaviour.KeyPressedManager;
import org.scijava.ui.behaviour.util.Actions;

import bdv.viewer.ViewerOptions;
import ij.ImagePlus;

import javax.annotation.Nonnull;

/**
 * Renders a multivariate normal distribution overlaid with the
 * respective ellipsoid in Big Data Viewer.
 */
public class BlobRenderingDemo
{

	public static void main( String... args )
	{
		// cov
		double[][] cov = {
				{ 400, 100, 200 },
				{ 100, 400, 300 },
				{ 200, 300, 400 }
		};
		double[] center = { 50, 50, 50 };
		Img< FloatType > image = BlobRenderingUtils.renderMultivariateNormalDistribution( center, cov );
		SharedBigDataViewerData sharedBigDataViewerData = BlobRenderingUtils.asSharedBdvDataXyz( image );
		Model model = new Model();
		// method init generates a spot
		model.getGraph().addVertex().init( 0, center, cov );

		Keymap keymap = new Keymap();
		MamutAppModel appModel = new MamutAppModel( model, sharedBigDataViewerData, new KeyPressedManager(), new TrackSchemeStyleManager(),
				new DataDisplayStyleManager(), new RenderSettingsManager(), new FeatureColorModeManager(), new KeymapManager(),
				new MamutPlugins( keymap ), new Actions( keymap.getConfig() ) );
		showBdvWindow( appModel );
	}

	private static < T extends RealType< T > > void showBdvWindow( @Nonnull MamutAppModel appModel )
	{
		MamutViewBdv ts = new MamutViewBdv( appModel, new HashMap<>() );
		ts.getFrame().setVisible( true );
	}
}
