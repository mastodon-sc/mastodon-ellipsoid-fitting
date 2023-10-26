/*-
 * #%L
 * mastodon-ellipsoid-fitting
 * %%
 * Copyright (C) 2015 - 2023 Tobias Pietzsch, Jean-Yves Tinevez
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.time.StopWatch;
import org.mastodon.app.ui.ViewMenuBuilder;
import org.mastodon.collection.RefSet;
import org.mastodon.mamut.KeyConfigScopes;
import org.mastodon.mamut.ProjectModel;
import org.mastodon.mamut.fitting.edgel.Edgels;
import org.mastodon.mamut.fitting.edgel.NoEllipsoidFoundException;
import org.mastodon.mamut.fitting.edgel.SampleEllipsoidEdgel;
import org.mastodon.mamut.fitting.ellipsoid.Ellipsoid;
import org.mastodon.mamut.fitting.ui.EdgelsOverlay;
import org.mastodon.mamut.fitting.ui.EllipsoidOverlay;
import org.mastodon.mamut.model.Spot;
import org.mastodon.mamut.plugin.MamutPlugin;
import org.mastodon.ui.keymap.KeyConfigContexts;
import org.scijava.AbstractContextual;
import org.scijava.plugin.Plugin;
import org.scijava.ui.behaviour.io.gui.CommandDescriptionProvider;
import org.scijava.ui.behaviour.io.gui.CommandDescriptions;
import org.scijava.ui.behaviour.util.AbstractNamedAction;
import org.scijava.ui.behaviour.util.Actions;
import org.scijava.ui.behaviour.util.RunnableAction;

import bdv.tools.brightness.ConverterSetup;
import bdv.util.Affine3DHelpers;
import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvStackSource;
import bdv.util.Bounds;
import bdv.viewer.ConverterSetups;
import bdv.viewer.SourceAndConverter;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.edge.Edgel;
import net.imglib2.algorithm.edge.SubpixelEdgelDetection;
import net.imglib2.algorithm.gauss3.Gauss3;
import net.imglib2.converter.RealTypeConverters;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.parallel.Parallelization;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Cast;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

@Plugin( type = FitEllipsoidPlugin.class )
public class FitEllipsoidPlugin extends AbstractContextual implements MamutPlugin
{
	private static final String FIT_SELECTED_VERTICES = "[ellipsoid fitting] fit selected vertices";

	private static final String[] FIT_SELECTED_VERTICES_KEYS = new String[] { "meta F", "alt F" };

	private static Map< String, String > menuTexts = new HashMap<>();

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
			super( KeyConfigScopes.MAMUT, KeyConfigContexts.MASTODON );
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

	private HeadlessProjectModel headlessProjectModel;

	public FitEllipsoidPlugin()
	{
		fitSelectedVerticesAction = new RunnableAction( FIT_SELECTED_VERTICES, this::fitSelectedVertices );
	}

	@Override
	public void setAppPluginModel( final ProjectModel projectModel )
	{
		this.headlessProjectModel = new HeadlessProjectModel( projectModel );
	}

	void setHeadlessProjectModel( final HeadlessProjectModel headlessProjectModel )
	{
		this.headlessProjectModel = headlessProjectModel;
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

	void fitSelectedVertices()
	{
		// TODO: parameters to select which source to act on
		final int sourceIndex = 0;
		final SourceAndConverter< ? > source = headlessProjectModel.getSharedBdvData().getSources().get( sourceIndex );
		if ( !( source.getSpimSource().getType() instanceof RealType ) )
			throw new IllegalArgumentException( "Expected RealType image source" );
		process( Cast.unchecked( source ) );
	}

	private static final boolean TRACE = false;

	private static final boolean DEBUG = false;

	private static final boolean DEBUG_UI = false;

	@SuppressWarnings( "unused" )
	private < T extends RealType< T > > void process( final SourceAndConverter< T > source )
	{
		final RefSet< Spot > vertices = headlessProjectModel.getSelectionModel().getSelectedVertices();
		if ( vertices.isEmpty() )
			System.err.println( "no vertex selected" );

		// init watch and counters
		final AtomicInteger found = new AtomicInteger( 0 );
		final AtomicInteger notFound = new AtomicInteger( 0 );
		final StopWatch watch = new StopWatch();
		watch.start();

		// parallelize over vertices
		final ArrayList< Spot > threadSafeVertices = asArrayList( vertices );
		// NB: RefSet is not thread-safe for iteration.
		final int totalTasks = vertices.size();
		final ReentrantReadWriteLock.WriteLock writeLock = headlessProjectModel.getModel().getGraph().getLock().writeLock();

		Parallelization.getTaskExecutor().forEach( threadSafeVertices, spot -> {
			// loop over vertices in parallel using multiple threads

			try
			{
				final long t1 = System.currentTimeMillis();
				final Ellipsoid ellipsoid = fitEllipsoid( spot, source );
				final long runtime = System.currentTimeMillis() - t1;
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
				found.getAndIncrement();
				if ( TRACE )
					System.out.println( "Computed ellipsoid in " + runtime + "ms. Ellipsoid: " + ellipsoid );
			}
			catch ( final NoEllipsoidFoundException e )
			{
				notFound.getAndIncrement();
				if ( DEBUG )
				{
					System.out.println( "No ellipsoid found. spot: " + spot.getLabel() );
					System.out.println( "Reason: " + e.getMessage() );
				}
			}
			catch ( final Exception e )
			{
				notFound.getAndIncrement();
				System.err.println( "Error while fitting ellipsoid for spot: " + spot.getLabel() );
				e.printStackTrace();
			}

			final int outputRate = 1000;
			if ( DEBUG && ( found.get() + notFound.get() ) % outputRate == 0 )
				System.out.println( "Computed " + ( found.get() + notFound.get() ) + " of " + totalTasks
						+ " ellipsoids ("
						+ Math.round( ( found.get() + notFound.get() ) / ( double ) totalTasks * 100d )
						+ "%). Total time: "
						+ watch.formatTime() );

		} );

		System.out.println( "found: " + found.get() + " ("
				+ Math.round( ( double ) found.get() / ( found.get() + notFound.get() ) * 100d )
				+ "%), not found: " + notFound.get() + " ("
				+ Math.round( ( double ) notFound.get() / ( found.get() + notFound.get() ) * 100d )
				+ "%), total time: " + watch.formatTime()
				+ ", time per spot: " + ( int ) ( ( double ) watch.getTime() / ( found.get() + notFound.get() ) )
				+ "ms." );

		// set undo point if at least one spot was fitted
		if ( found.get() > 0 )
			headlessProjectModel.getModel().setUndoPoint();
	}

	private static ArrayList< Spot > asArrayList( final RefSet< Spot > vertices )
	{
		final ArrayList< Spot > list = new ArrayList<>();
		for ( final Spot spot : vertices )
		{
			final Spot spotCopy = vertices.createRef();
			spotCopy.refTo( spot );
			list.add( spotCopy );
		}
		return list;
	}

	/**
	 * Fit an ellipsoid for the given spot.
	 *
	 * @throws NoEllipsoidFoundException
	 *             if the ellipsoid fitting algorithm simple does not yield a
	 *             result.
	 * @throws RuntimeException
	 *             if there are other problems, e.g. the image source is not
	 *             present or the image is not a {@link RealType}.
	 */
	@Nonnull
	private < T extends RealType< T > > Ellipsoid fitEllipsoid( final Spot spot, final SourceAndConverter< T > source )
	{
		// TODO: parameters -----------------
		final double smoothSigma = 2;

		final double minGradientMagnitude = 10;

		final double maxAngle = 5 * Math.PI / 180.0;
		final double maxFactor = 1.1;

		final int numSamples = 1000;
		final int numCandidates = 100;
		final double outsideCutoffDistance = 3;
		final double insideCutoffDistance = 5;
		final double angleCutoffDistance = 30 * Math.PI / 180.0;
		final double maxCenterDistance = 10;
		// ----------------------------------

		final int timepoint = spot.getTimepoint();
		final AffineTransform3D sourceToGlobal = new AffineTransform3D();
		source.getSpimSource().getSourceTransform( timepoint, 0, sourceToGlobal );
		final RandomAccessibleInterval< T > frame = source.getSpimSource().getSource( timepoint, 0 );

		if ( frame == null )
			throw new RuntimeException( "No image data for spot: " + spot.getLabel() + " timepoint: " + timepoint );

		final RandomAccessibleInterval< T > cropped = cropSpot( sourceToGlobal, frame, spot );

		final RandomAccessibleInterval< FloatType > converted = RealTypeConverters.convert( cropped, new FloatType() );

		final RandomAccessibleInterval< FloatType > input = gaussianBlur( smoothSigma, extractScale( sourceToGlobal ), converted );

		final ArrayList< Edgel > gEdgels = getAllEgels( minGradientMagnitude, sourceToGlobal, input );

		final double[] centerInGlobalCoordinates = spot.positionAsDoubleArray();
		final ArrayList< Edgel > filteredEdgels = Edgels.filterEdgelsByOcclusion(
				Edgels.filterEdgelsByDirection( gEdgels, centerInGlobalCoordinates ), centerInGlobalCoordinates,
				maxAngle, maxFactor );

		final Ellipsoid ellipsoid = SampleEllipsoidEdgel.sample(
				filteredEdgels,
				centerInGlobalCoordinates,
				numSamples,
				numCandidates,
				outsideCutoffDistance,
				insideCutoffDistance,
				angleCutoffDistance,
				maxCenterDistance );

		if ( DEBUG_UI )
			showBdvDebugWindow( source, outsideCutoffDistance, insideCutoffDistance, angleCutoffDistance, sourceToGlobal, input, filteredEdgels, ellipsoid );
		return ellipsoid;
	}

	private static < T extends RealType< T > > RandomAccessibleInterval< T > cropSpot( final AffineTransform3D sourceToGlobal,
			final RandomAccessibleInterval< T > frame, final Spot spot )
	{
		final double[] centerInGlobalCoordinates = spot.positionAsDoubleArray();
		final double radius = Math.sqrt( spot.getBoundingSphereRadiusSquared() );
		final double[] centerInLocalCoordinates = new double[ 3 ];
		sourceToGlobal.applyInverse( centerInLocalCoordinates, centerInGlobalCoordinates );

		final double[] scale = extractScale( sourceToGlobal );

		final long[] lMin = new long[ 3 ];
		final long[] lMax = new long[ 3 ];

		for ( int d = 0; d < 3; ++d )
		{
			final double halfsize = 2 * radius / scale[ d ] + 2;
			lMin[ d ] = ( long ) ( centerInLocalCoordinates[ d ] - halfsize );
			lMax[ d ] = ( long ) ( centerInLocalCoordinates[ d ] + halfsize );
		}

		Interval interval = FinalInterval.wrap( lMin, lMax );
		interval = Intervals.intersect( interval, frame );

		return Views.interval( frame, interval );
	}

	private static RandomAccessibleInterval< FloatType > gaussianBlur( final double sigma, final double[] scale, final RandomAccessibleInterval< FloatType > input )
	{
		if ( sigma <= 0.0 )
			return input;
		final long[] size = input.dimensionsAsLongArray();
		final long[] min = input.minAsLongArray();
		final RandomAccessibleInterval< FloatType > img = ArrayImgs.floats( size );
		final double[] sigmas = new double[ 3 ];
		for ( int d = 0; d < 3; ++d )
			sigmas[ d ] = sigma / scale[ d ];
		try
		{
			Gauss3.gauss( sigmas, Views.extendMirrorSingle( Views.zeroMin( input ) ), img );
		}
		catch ( final IncompatibleTypeException e )
		{
			throw new RuntimeException( e );
		}
		return Views.translate( img, min );
	}

	private static ArrayList< Edgel > getAllEgels( final double minGradientMagnitude, final AffineTransform3D sourceToGlobal, final RandomAccessibleInterval< FloatType > input )
	{
		final ArrayList< Edgel > lEdgels = SubpixelEdgelDetection.getEdgels( Views.zeroMin( input ),
				new ArrayImgFactory<>( new FloatType() ), minGradientMagnitude );
		final AffineTransform3D zeroMinSourceToGlobal = sourceToGlobal.copy();
		final AffineTransform3D shiftToMin = new AffineTransform3D();
		final long[] lMin = input.minAsLongArray();
		shiftToMin.translate( lMin[ 0 ], lMin[ 1 ], lMin[ 2 ] );
		zeroMinSourceToGlobal.concatenate( shiftToMin );
		return Edgels.transformEdgels( lEdgels, zeroMinSourceToGlobal );
	}

	private static double[] extractScale( final AffineTransform3D sourceToGlobal )
	{
		final double[] scale = new double[ 3 ];
		for ( int d = 0; d < 3; ++d )
			scale[ d ] = Affine3DHelpers.extractScale( sourceToGlobal, d );
		return scale;
	}

	private void showBdvDebugWindow( final SourceAndConverter< ? > source, final double outsideCutoffDistance, final double insideCutoffDistance, final double angleCutoffDistance,
			final AffineTransform3D sourceToGlobal, final RandomAccessibleInterval< FloatType > input, final ArrayList< Edgel > filteredEdgels, final Ellipsoid ellipsoid )
	{
		final BdvStackSource< FloatType > inputSource =
				BdvFunctions.show( input, "FloatType input", Bdv.options().sourceTransform( sourceToGlobal ) );
		final ConverterSetups setups = headlessProjectModel.getSharedBdvData().getConverterSetups();
		final ConverterSetup cs = setups.getConverterSetup( source );
		final Bounds bounds = setups.getBounds().getBounds( cs );
		inputSource.setDisplayRange( cs.getDisplayRangeMin(), cs.getDisplayRangeMax() );
		inputSource.setDisplayRangeBounds( bounds.getMinBound(), bounds.getMaxBound() );
		final Bdv bdv = inputSource.getBdvHandle();

		final EdgelsOverlay edgelsOverlay = new EdgelsOverlay( filteredEdgels, 0.01 );
		BdvFunctions.showOverlay( edgelsOverlay, "filtered edgels", Bdv.options().addTo( bdv ) );

		BdvFunctions.showOverlay( new EllipsoidOverlay( ellipsoid ), "fitted ellipsoid",
				Bdv.options().addTo( bdv ) );
		final Map< Edgel, Double > costs = SampleEllipsoidEdgel.getCosts(
				filteredEdgels,
				ellipsoid,
				outsideCutoffDistance,
				insideCutoffDistance,
				angleCutoffDistance );
		edgelsOverlay.setCosts( costs );
	}
}
