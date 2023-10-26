package org.mastodon.mamut.fitting;

import org.mastodon.mamut.ProjectModel;
import org.mastodon.mamut.model.Link;
import org.mastodon.mamut.model.Model;
import org.mastodon.mamut.model.Spot;
import org.mastodon.model.SelectionModel;
import org.mastodon.views.bdv.SharedBigDataViewerData;

public class HeadlessProjectModel
{
	private final Model model;

	private final SharedBigDataViewerData sharedBdvData;

	private final SelectionModel< Spot, Link > selectionModel;

	HeadlessProjectModel(
			final Model model, final SharedBigDataViewerData sharedBdvData, final SelectionModel< Spot, Link > selectionModel
	)
	{
		this.model = model;
		this.sharedBdvData = sharedBdvData;
		this.selectionModel = selectionModel;
	}

	public HeadlessProjectModel( final ProjectModel projectModel )
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
