package org.mastodon.mamut.fitting;

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
public interface MinimalProjectModel
{
	/**
	 * Gets the {@link Model} of the project, i.e. all data tracking related data such as the graph and feature.
	 * @return the model.
	 */
	Model getModel();

	/**
	 * Gets the {@link SharedBigDataViewerData} of the project, i.e. the image data.
	 * @return the shared BDV data.
	 */
	SharedBigDataViewerData getSharedBdvData();

	/**
	 * Gets the {@link SelectionModel} of the project, i.e. the selection of spots and links.
	 * @return the selection model.
	 */
	SelectionModel< Spot, Link > getSelectionModel();
}
