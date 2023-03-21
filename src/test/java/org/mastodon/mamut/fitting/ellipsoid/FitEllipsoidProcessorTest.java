package org.mastodon.mamut.fitting.ellipsoid;

import bdv.viewer.SourceAndConverter;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mastodon.mamut.MamutAppModel;
import org.mastodon.mamut.MamutViewBdv;
import org.mastodon.mamut.fitting.BlobRenderingUtils;
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
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FitEllipsoidProcessorTest
{
	private static MamutAppModel appModel;

	private static SharedBigDataViewerData sharedBigDataViewerData;

	@BeforeClass
	public static void setUp()
	{
		Keymap keymap = new Keymap();
		Img< FloatType > image = ArrayImgs.floats( 400, 400, 400 );
		Model model = new Model();
		sharedBigDataViewerData = BlobRenderingUtils.asSharedBdvDataXyz( image );
		appModel = new MamutAppModel( model, sharedBigDataViewerData, new KeyPressedManager(), new TrackSchemeStyleManager(),
				new DataDisplayStyleManager(), new RenderSettingsManager(), new FeatureColorModeManager(), new KeymapManager(),
				new MamutPlugins( keymap ), new Actions( keymap.getConfig() ) );

		int size = 100;
		for ( int i = 0; i < 4; i++ )
			for ( int j = 0; j < 4; j++ )
				for ( int k = 0; k < 4; k++ )
				{
					double cov00 = ThreadLocalRandom.current().nextInt( 64, 400 );
					double cov01 = ThreadLocalRandom.current().nextInt( 20, 50 );
					double cov02 = ThreadLocalRandom.current().nextInt( 20, 50 );
					double cov12 = ThreadLocalRandom.current().nextInt( 20, 50 );
					// input covariance matrix
					double[][] inputCovarianceMatrix = {
							{ cov00, cov01, cov02 },
							{ cov01, cov00, cov12 },
							{ cov02, cov12, cov00 }
					};
					int centerX = i * size + size / 2;
					int centerY = j * size + size / 2;
					int centerZ = k * size + size / 2;
					double[] inputCenter = { centerX, centerY, centerZ };

					BlobRenderingUtils.renderMultivariateNormalDistribution( inputCenter, inputCovarianceMatrix, ( double ) size, image );
					System.out.println( "Adding spot at " + centerX + ", " + centerY + ", " + centerZ );
					// method init generates a spot
					model.getGraph().addVertex().init( 0, inputCenter, inputCovarianceMatrix );
				}
	}

	private static < T extends RealType< T > > void showBdvWindow( @Nonnull MamutAppModel appModel )
	{
		MamutViewBdv ts = new MamutViewBdv( appModel, new HashMap<>() );
		ts.getFrame().setVisible( true );
	}

	@Test
	public void testProcess()
	{
		final SourceAndConverter< ? > source = sharedBigDataViewerData.getSources().get( 0 );
		appModel.getModel().getGraph().vertices().forEach( vertex -> appModel.getSelectionModel().setSelected( vertex, true ) );

		long t1 = System.currentTimeMillis();
		FitEllipsoidProcessor fitEllipsoidProcessorParallel = new FitEllipsoidProcessor();
		fitEllipsoidProcessorParallel.process( ( SourceAndConverter ) source, appModel, true, false );
		System.out.println( "Found " + fitEllipsoidProcessorParallel.getFound().get() + " spots (parallel)." );
		assertEquals( 64, fitEllipsoidProcessorParallel.getFound().get() );
		long t2 = System.currentTimeMillis();

		long t3 = System.currentTimeMillis();
		FitEllipsoidProcessor fitEllipsoidProcessorSequential = new FitEllipsoidProcessor();
		fitEllipsoidProcessorSequential.process( ( SourceAndConverter ) source, appModel, false, false );
		System.out.println( "Found " + fitEllipsoidProcessorSequential.getFound().get() + " spots (sequential)." );
		assertEquals( 64, fitEllipsoidProcessorSequential.getFound().get() );
		long t4 = System.currentTimeMillis();

		System.out.println( "Parallel: " + ( t2 - t1 ) + " ms" );
		System.out.println( "Sequential: " + ( t4 - t3 ) + " ms" );
		// parallel should be faster, if there is more than 1 core available
		if ( Runtime.getRuntime().availableProcessors() > 1 )
			assertTrue( ( t2 - t1 ) < ( t4 - t3 ) );
	}
}
