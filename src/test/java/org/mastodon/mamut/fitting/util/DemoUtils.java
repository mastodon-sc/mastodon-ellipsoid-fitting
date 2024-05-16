/*-
 * #%L
 * mastodon-ellipsoid-fitting
 * %%
 * Copyright (C) 2015 - 2024 Tobias Pietzsch, Jean-Yves Tinevez
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
import org.mastodon.mamut.fitting.MinimalProjectModel;
import org.mastodon.mamut.model.Link;
import org.mastodon.mamut.model.Model;
import org.mastodon.mamut.model.Spot;
import org.mastodon.mamut.views.bdv.MamutViewBdv;
import org.mastodon.model.DefaultSelectionModel;
import org.mastodon.model.SelectionModel;
import org.mastodon.views.bdv.SharedBigDataViewerData;
import org.scijava.Context;

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
		return ProjectModel.create( new Context(), model, sharedBigDataViewerData, null );
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
