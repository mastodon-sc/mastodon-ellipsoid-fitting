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

	MinimalProjectModel(
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
