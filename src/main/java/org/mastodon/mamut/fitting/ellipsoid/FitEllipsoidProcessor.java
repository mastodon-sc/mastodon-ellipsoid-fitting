package org.mastodon.mamut.fitting.ellipsoid;

import bdv.viewer.SourceAndConverter;

import net.imglib2.parallel.Parallelization;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.RealType;
import org.apache.commons.lang3.time.StopWatch;
import org.mastodon.collection.RefSet;
import org.mastodon.mamut.MamutAppModel;
import org.mastodon.mamut.model.Spot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A class that processes a list of vertices and fit an ellipsoid to each of them.
 */
public class FitEllipsoidProcessor
{

	private final AtomicInteger found = new AtomicInteger( 0 );

	private final AtomicInteger notFound = new AtomicInteger( 0 );

	private final StopWatch watch = new StopWatch();

	/**
	 * Process the vertices and fit an ellipsoid to each of them.
	 *
	 * @param source the source to use for the fitting
	 * @param appModel the app model
	 * @param parallelize if true, the vertices will be processed in parallel
	 * @param verbose if true, the fitting will be verbose
	 */
	public < T extends RealType< T > > void process( final SourceAndConverter< T > source, MamutAppModel appModel, boolean parallelize,
			boolean verbose )
	{
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

		if( parallelize )
		{
			// Note: RefCollections, are not usable in parallel forEach, so we need to convert them to a ArrayList.
			List< Spot > list = asArrayList( vertices );
			Parallelization.getTaskExecutor().forEach( list, spot -> {
				updateEllipsoid( spot, source, sourceToGlobal, appModel, vertices.size(), verbose );
			} );
		}
		else
		{
			for ( Spot spot : vertices )
				updateEllipsoid( spot, source, sourceToGlobal, appModel, vertices.size(), verbose );
		}

		System.out.println( "found: " + found.get() + " (" + Math.round( ( double ) found.get() / ( found.get() + notFound.get() ) * 100d )
				+ "%), not found: " + notFound.get() + " ("
				+ Math.round( ( double ) notFound.get() / ( found.get() + notFound.get() ) * 100d ) + "%), total time: "
				+ watch.formatTime() + ", time per spot: " + ( int ) ( ( double ) watch.getTime() / ( found.get() + notFound.get() ) )
				+ "ms." );
	}

	private static List< Spot > asArrayList( RefSet< Spot > vertices )
	{
		List<Spot> list = new ArrayList<>();
		for ( final Spot spot : vertices )
		{
			Spot spotCopy = vertices.createRef();
			spotCopy.refTo( spot );
			list.add( spot );
		}
		return list;
	}

	/**
	 * @return the number of ellipsoids that could be fitted.
	 */
	public int getNumFound()
	{
		return found.get();
	}

	private < T extends RealType< T > > void updateEllipsoid( final Spot spot, final SourceAndConverter< T > source,
			final AffineTransform3D sourceToGlobal, final MamutAppModel appModel, final int totalTasks, boolean verbose )
	{
		Ellipsoid ellipsoid = FitEllipsoid.getFittedEllipsoid( spot, source, sourceToGlobal, appModel, 1000, verbose, false );

		if ( ellipsoid == null )
		{
			notFound.getAndIncrement();
			if ( verbose )
				System.out.println( "no ellipsoid found. spot: " + spot.getLabel() );
			return;
		}
		else
			found.getAndIncrement();

		if ( verbose )
		{
			int outputRate = 1000;
			if ( ( found.get() + notFound.get() ) % outputRate == 0 )
				System.out.println( "Computed " + ( found.get() + notFound.get() ) + " of " + totalTasks + " ellipsoids ("
						+ Math.round( ( found.get() + notFound.get() ) / ( double ) totalTasks * 100d ) + "%). Total time: "
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
