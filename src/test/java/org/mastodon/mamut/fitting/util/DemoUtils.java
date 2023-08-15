package org.mastodon.mamut.fitting.util;

import ij.ImagePlus;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImgToVirtualStack;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import org.mastodon.mamut.ProjectModel;
import org.mastodon.mamut.model.Link;
import org.mastodon.mamut.model.Model;
import org.mastodon.mamut.model.Spot;
import org.mastodon.mamut.views.bdv.MamutViewBdv;
import org.mastodon.model.DefaultSelectionModel;
import org.mastodon.model.SelectionModel;
import org.mastodon.views.bdv.SharedBigDataViewerData;

import javax.annotation.Nonnull;
import java.util.Objects;

public class DemoUtils
{
	private DemoUtils()
	{
		// prevent from instantiation
	}

	public static ProjectModel wrapAsAppModel( final Img< FloatType > image, final Model model )
	{
		final SharedBigDataViewerData sharedBigDataViewerData = asSharedBdvDataXyz( image );
		return ProjectModel.create( null, model, sharedBigDataViewerData, null );
	}

	public static MinimalProjectModel wrapAsMinimalModel( final Img< FloatType > image, final Model model )
	{
		SharedBigDataViewerData sharedBDVData = DemoUtils.asSharedBdvDataXyz( image );
		SelectionModel< Spot, Link > selectionModel = new DefaultSelectionModel<>( model.getGraph(), model.getGraphIdBimap() );
		return new MinimalProjectModel( model, sharedBDVData, selectionModel );
	}

	public static SharedBigDataViewerData asSharedBdvDataXyz( final Img< FloatType > image1 )
	{
		final ImagePlus image = ImgToVirtualStack.wrap( new ImgPlus<>( image1, "image", new AxisType[] { Axes.X, Axes.Y, Axes.Z } ) );
		return Objects.requireNonNull( SharedBigDataViewerData.fromImagePlus( image ) );
	}

	public static < T extends RealType< T > > void showBdvWindow( @Nonnull final ProjectModel appModel )
	{
		appModel.getWindowManager().createView( MamutViewBdv.class );
	}
}
