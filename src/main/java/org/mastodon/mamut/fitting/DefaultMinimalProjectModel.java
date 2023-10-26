package org.mastodon.mamut.fitting;

import org.mastodon.mamut.ProjectModel;
import org.mastodon.mamut.model.Link;
import org.mastodon.mamut.model.Model;
import org.mastodon.mamut.model.Spot;
import org.mastodon.model.SelectionModel;
import org.mastodon.views.bdv.SharedBigDataViewerData;

/**
 * A default implementation of {@link MinimalProjectModel}.
 */
public class DefaultMinimalProjectModel implements MinimalProjectModel
{
	private final Model model;

	private final SharedBigDataViewerData sharedBdvData;

	private final SelectionModel< Spot, Link > selectionModel;

	DefaultMinimalProjectModel(
			final Model model, final SharedBigDataViewerData sharedBdvData, final SelectionModel< Spot, Link > selectionModel
	)
	{
		this.model = model;
		this.sharedBdvData = sharedBdvData;
		this.selectionModel = selectionModel;
	}

	public DefaultMinimalProjectModel( final ProjectModel projectModel )
	{
		this( projectModel.getModel(), projectModel.getSharedBdvData(), projectModel.getSelectionModel() );
	}

	@Override
	public Model getModel()
	{
		return model;
	}

	@Override
	public SharedBigDataViewerData getSharedBdvData()
	{
		return sharedBdvData;
	}

	@Override
	public SelectionModel< Spot, Link > getSelectionModel()
	{
		return selectionModel;
	}
}
