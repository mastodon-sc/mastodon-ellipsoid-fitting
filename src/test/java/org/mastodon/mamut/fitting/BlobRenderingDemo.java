package org.mastodon.mamut.fitting;

import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import org.mastodon.mamut.MamutAppModel;
import org.mastodon.mamut.MamutViewBdv;
import org.mastodon.mamut.model.Model;
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

import javax.annotation.Nonnull;
import java.util.HashMap;

/**
 * Renders a multivariate normal distribution overlaid with the
 * respective ellipsoid in Big Data Viewer.
 */
public class BlobRenderingDemo
{

	public static void main( String... args )
	{
		// cov
		double[][] inputCovarianceMatrix = {
				{ 400, 100, 200 },
				{ 100, 400, 300 },
				{ 200, 300, 400 }
		};
		double[] inputCenter = { 50, 50, 50 };
		Model model = new Model();

		Img< FloatType > image = ArrayImgs.floats( 100, 100, 100 );
		BlobRenderingUtils.renderMultivariateNormalDistribution( inputCenter, inputCovarianceMatrix, image );
		// method init generates a spot
		model.getGraph().addVertex().init( 0, inputCenter, inputCovarianceMatrix );
		SharedBigDataViewerData sharedBigDataViewerData = BlobRenderingUtils.asSharedBdvDataXyz( image );
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
