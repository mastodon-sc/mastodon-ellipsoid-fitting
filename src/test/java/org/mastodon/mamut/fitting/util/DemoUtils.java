package org.mastodon.mamut.fitting.util;

import java.util.HashMap;
import java.util.Objects;

import javax.annotation.Nonnull;

import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImgToVirtualStack;
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

import bdv.viewer.ViewerOptions;
import ij.ImagePlus;

public class DemoUtils
{

	private DemoUtils()
	{
		// prevent from instantiation
	}

	public static MamutAppModel wrapAsAppModel( Img< FloatType > image, Model model )
	{
		SharedBigDataViewerData sharedBigDataViewerData = asSharedBdvDataXyz( image );
		Keymap keymap = new Keymap();
		return new MamutAppModel( model, sharedBigDataViewerData, new KeyPressedManager(), new TrackSchemeStyleManager(),
				new DataDisplayStyleManager(), new RenderSettingsManager(), new FeatureColorModeManager(), new KeymapManager(),
				new MamutPlugins( keymap ), new Actions( keymap.getConfig() ) );
	}

	public static SharedBigDataViewerData asSharedBdvDataXyz( Img< FloatType > image1 )
	{
		ImagePlus image = ImgToVirtualStack.wrap( new ImgPlus<>( image1, "image", new AxisType[] { Axes.X, Axes.Y, Axes.Z } ) );
		return Objects.requireNonNull( SharedBigDataViewerData.fromImagePlus( image, new ViewerOptions(), () -> {} ) );
	}

	public static void showBdvWindow( @Nonnull MamutAppModel appModel )
	{
		MamutViewBdv ts = new MamutViewBdv( appModel, new HashMap<>() );
		ts.getFrame().setVisible( true );
	}
}
