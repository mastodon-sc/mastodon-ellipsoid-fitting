/*-
 * #%L
 * mastodon-ellipsoid-fitting
 * %%
 * Copyright (C) 2015 - 2022 Tobias Pietzsch, Jean-Yves Tinevez
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


import static org.mastodon.app.ui.ViewMenuBuilder.item;
import static org.mastodon.app.ui.ViewMenuBuilder.menu;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiConsumer;

import org.apache.commons.lang3.time.StopWatch;
import org.mastodon.app.ui.ViewMenuBuilder;
import org.mastodon.collection.RefSet;
import org.mastodon.mamut.MamutAppModel;
import org.mastodon.mamut.fitting.ellipsoid.Ellipsoid;
import org.mastodon.mamut.fitting.ellipsoid.FitEllipsoid;
import org.mastodon.mamut.model.Spot;
import org.mastodon.mamut.plugin.MamutPlugin;
import org.mastodon.mamut.plugin.MamutPluginAppModel;
import org.mastodon.ui.keymap.CommandDescriptionProvider;
import org.mastodon.ui.keymap.CommandDescriptions;
import org.mastodon.ui.keymap.KeyConfigContexts;
import org.scijava.AbstractContextual;
import org.scijava.plugin.Plugin;
import org.scijava.ui.behaviour.util.AbstractNamedAction;
import org.scijava.ui.behaviour.util.Actions;
import org.scijava.ui.behaviour.util.RunnableAction;

import bdv.viewer.SourceAndConverter;
import net.imglib2.EuclideanSpace;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.RealType;

@Plugin( type = FitEllipsoidPlugin.class )
public class FitEllipsoidPlugin extends AbstractContextual implements MamutPlugin
{
	private static final String FIT_SELECTED_VERTICES = "[ellipsoid fitting] fit selected vertices";

	private static final String[] FIT_SELECTED_VERTICES_KEYS = new String[] { "meta F", "alt F" };

	private static Map< String, String > menuTexts = new HashMap<>();

	private final AtomicInteger found = new AtomicInteger( 0 );

	private final AtomicInteger notFound = new AtomicInteger( 0 );

	private final StopWatch watch = new StopWatch();

	static
	{
		menuTexts.put( FIT_SELECTED_VERTICES, "Fit Selected Vertices" );
	}

	/*
	 * Command description.
	 */
	@Plugin( type = Descriptions.class )
	public static class Descriptions extends CommandDescriptionProvider
	{
		public Descriptions()
		{
			super( KeyConfigContexts.MASTODON );
		}

		@Override
		public void getCommandDescriptions( final CommandDescriptions descriptions )
		{
			descriptions.add(
					FIT_SELECTED_VERTICES,
					FIT_SELECTED_VERTICES_KEYS,
					"Fit the currently selected spots to ellipsoids that best wrap bright pixels in the first source." );
		}
	}

	private final AbstractNamedAction fitSelectedVerticesAction;

	private MamutPluginAppModel pluginAppModel;

	public FitEllipsoidPlugin()
	{
		fitSelectedVerticesAction = new RunnableAction( FIT_SELECTED_VERTICES, this::fitSelectedVertices );
		updateEnabledActions();
	}

	@Override
	public void setAppPluginModel( final MamutPluginAppModel model )
	{
		this.pluginAppModel = model;
		updateEnabledActions();
	}

	@Override
	public List< ViewMenuBuilder.MenuItem > getMenuItems()
	{
		return Arrays.asList(
				menu( "Plugins",
						menu( "Ellipsoid Fitting",
								item( FIT_SELECTED_VERTICES ) ) ) );
	}

	@Override
	public Map< String, String > getMenuTexts()
	{
		return menuTexts;
	}

	@Override
	public void installGlobalActions( final Actions actions )
	{
		actions.namedAction( fitSelectedVerticesAction, FIT_SELECTED_VERTICES_KEYS );
	}

	private void updateEnabledActions()
	{
		final MamutAppModel appModel = ( pluginAppModel == null ) ? null : pluginAppModel.getAppModel();
		fitSelectedVerticesAction.setEnabled( appModel != null );
	}

	@SuppressWarnings( { "unchecked", "rawtypes" } )
	private void fitSelectedVertices()
	{
		// TODO: parameters to select which source to act on
		final int sourceIndex = 0;

		if ( pluginAppModel != null )
		{
			final MamutAppModel appModel = pluginAppModel.getAppModel();
			final SourceAndConverter< ? > source = appModel.getSharedBdvData().getSources().get( sourceIndex );
			if ( !( source.getSpimSource().getType() instanceof RealType ) )
				throw new IllegalArgumentException( "Expected RealType image source" );

			process( ( SourceAndConverter ) source );
		}
	}

	private static final boolean DEBUG = false;

	@SuppressWarnings( "unused" )
	private < T extends RealType< T > > void process( final SourceAndConverter< T > source )
	{
		final MamutAppModel appModel = pluginAppModel.getAppModel();

		final RefSet< Spot > vertices = appModel.getSelectionModel().getSelectedVertices();
		if ( vertices.isEmpty() )
		{
			System.out.println( "no vertex selected" );
			return;
		}

		final AffineTransform3D sourceToGlobal = new AffineTransform3D();
		// init watch and counters
		{
			watch.reset();
			watch.start();
			found.set( 0 );
			notFound.set( 0 );
		}

		// Fixed thread number depending on the number of available processors
		int processors = Runtime.getRuntime().availableProcessors();
		ExecutorService executorService = Executors.newFixedThreadPool( processors );
		List< Callable< Object > > todo = new ArrayList<>( vertices.size() );

		// parallelize over vertices
		for ( final Spot spot : vertices )
		{
			Spot spotCopy = appModel.getModel().getGraph().vertexRef();
			spotCopy.refTo( spot );
			todo.add( Executors.callable( () -> updateEllipsoid( spotCopy, source, sourceToGlobal, appModel, vertices.size() ) ) );
		}

		// invoke all tasks
		try
		{
			executorService.invokeAll( todo );
		}
		catch ( InterruptedException e )
		{
			e.printStackTrace();
		}

		System.out.println( "found: " + found.get() + " ("
				+ Math.round( ( double ) found.get() / ( found.get() + notFound.get() ) * 100d )
				+ "%), not found: " + notFound.get() + " ("
				+ Math.round( ( double ) notFound.get() / ( found.get() + notFound.get() ) * 100d )
				+ "%), total time: " + watch.formatTime()
				+ ", time per spot: " + ( int ) ( ( double ) watch.getTime() / ( found.get() + notFound.get() ) )
				+ "ms." );
	}

	// TODO: move to imglib2 core and make versions for int[], double[] ???
	public static < T extends EuclideanSpace > long[] longArrayFrom( final T t, final BiConsumer< T, long[] > get )
	{
		final long[] a = new long[ t.numDimensions() ];
		get.accept( t, a );
		return a;
	}

	private < T extends RealType< T > > void updateEllipsoid( final Spot spot, final SourceAndConverter< T > source,
			final AffineTransform3D sourceToGlobal,
			final MamutAppModel appModel, final int totalTasks )
	{
		Ellipsoid ellipsoid = FitEllipsoid.getFittedEllipsoid( spot, source, sourceToGlobal, appModel, 100, DEBUG, DEBUG );

		if ( ellipsoid == null )
		{
			notFound.getAndIncrement();
			if ( DEBUG )
				System.out.println( "no ellipsoid found. spot: " + spot.getLabel() );
			return;
		}
		else
			found.getAndIncrement();

		if ( DEBUG )
		{
			int outputRate = 1000;
			if ( ( found.get() + notFound.get() ) % outputRate == 0 )
				System.out
						.println( "Computed " + ( found.get() + notFound.get() ) + " of " + totalTasks
								+ " ellipsoids ("
								+ Math.round( ( found.get() + notFound.get() ) / ( double ) totalTasks * 100d )
								+ "%). Total time: "
								+ watch.formatTime() );
		}

		ReentrantReadWriteLock.WriteLock writeLock = appModel.getModel().getGraph().getLock().writeLock();
		writeLock.lock();
		try
		{
			spot.setPosition( ellipsoid );
			spot.setCovariance( ellipsoid.getCovariance() );
		}
		finally
		{
			writeLock.unlock();
		}
	}
}
