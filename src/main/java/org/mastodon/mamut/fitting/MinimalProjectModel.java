/*-
 * #%L
 * mastodon-ellipsoid-fitting
 * %%
 * Copyright (C) 2015 - 2025 Tobias Pietzsch, Jean-Yves Tinevez
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
package org.mastodon.mamut.fitting;

import org.mastodon.mamut.ProjectModel;
import org.mastodon.mamut.model.Link;
import org.mastodon.mamut.model.Model;
import org.mastodon.mamut.model.Spot;
import org.mastodon.model.SelectionModel;
import org.mastodon.views.bdv.SharedBigDataViewerData;

/**
 * A minimal interface to the data model of a Mastodon project.<p>
 * This interface facilities testing of the ellipsoid fitting, since it allows to circumvent the fact that the {@link org.mastodon.mamut.ProjectModel} creates GUI components on instantiation.<p>
 * It contains accessor methods to parts of the {@link org.mastodon.mamut.ProjectModel} that are safe to be used in a headless way.
 *
 * @author Stefan Hahmann
 */
public class MinimalProjectModel
{
	private final Model model;

	private final SharedBigDataViewerData sharedBdvData;

	private final SelectionModel< Spot, Link > selectionModel;

	public MinimalProjectModel(
			final Model model, final SharedBigDataViewerData sharedBdvData, final SelectionModel< Spot, Link > selectionModel
	)
	{
		this.model = model;
		this.sharedBdvData = sharedBdvData;
		this.selectionModel = selectionModel;
	}

	public MinimalProjectModel( final ProjectModel projectModel )
	{
		this( projectModel.getModel(), projectModel.getSharedBdvData(), projectModel.getSelectionModel() );
	}

	public Model getModel()
	{
		return model;
	}

	public SharedBigDataViewerData getSharedBdvData()
	{
		return sharedBdvData;
	}

	public SelectionModel< Spot, Link > getSelectionModel()
	{
		return selectionModel;
	}
}
