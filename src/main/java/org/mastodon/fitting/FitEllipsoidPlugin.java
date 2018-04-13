package org.mastodon.fitting;

import static org.mastodon.app.ui.ViewMenuBuilder.item;
import static org.mastodon.app.ui.ViewMenuBuilder.menu;

import bdv.tools.brightness.ConverterSetup;
import bdv.tools.brightness.MinMaxGroup;
import bdv.util.Bdv;
import bdv.util.BdvOverlaySource;
import bdv.util.BdvStackSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import java.util.function.BiConsumer;
import javax.swing.UIManager;

import mpicbg.spim.data.generic.AbstractSpimData;
import net.imglib2.EuclideanSpace;
import net.imglib2.Interval;
import net.imglib2.algorithm.edge.Edgel;
import net.imglib2.algorithm.edge.SubpixelEdgelDetection;
import net.imglib2.algorithm.gauss3.Gauss3;
import net.imglib2.converter.Converters;
import net.imglib2.converter.RealFloatConverter;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Util;
import org.mastodon.app.ui.ViewMenuBuilder;
import org.mastodon.collection.RefSet;
import org.mastodon.fitting.edgel.Edgels;
import org.mastodon.fitting.edgel.SampleEllipsoidEdgel;
import org.mastodon.fitting.ellipsoid.Ellipsoid;
import org.mastodon.fitting.ui.EdgelsOverlay;
import org.mastodon.fitting.ui.EllipsoidOverlay;
import org.mastodon.plugin.MastodonPlugin;
import org.mastodon.plugin.MastodonPluginAppModel;
import org.mastodon.revised.mamut.MamutAppModel;
import org.mastodon.revised.mamut.MamutProject;
import org.mastodon.revised.mamut.MamutProjectIO;
import org.mastodon.revised.mamut.Mastodon;
import org.mastodon.revised.model.mamut.Spot;
import org.scijava.AbstractContextual;
import org.scijava.Context;
import org.scijava.plugin.Plugin;
import org.scijava.ui.behaviour.util.AbstractNamedAction;
import org.scijava.ui.behaviour.util.Actions;
import org.scijava.ui.behaviour.util.RunnableAction;

import bdv.util.Affine3DHelpers;
import bdv.util.BdvFunctions;
import mpicbg.spim.data.generic.sequence.BasicSetupImgLoader;
import mpicbg.spim.data.registration.ViewRegistrations;
import mpicbg.spim.data.sequence.TimePoint;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.view.Views;

@Plugin( type = FitEllipsoidPlugin.class )
public class FitEllipsoidPlugin extends AbstractContextual implements MastodonPlugin
{
	private static final String FIT_SELECTED_VERTICES = "[ellipsoid fitting] fit selected vertices";

	private static final String[] FIT_SELECTED_VERTICES_KEYS = new String[] { "meta F" };

	private static Map< String, String > menuTexts = new HashMap<>();

	static
	{
		menuTexts.put( FIT_SELECTED_VERTICES, "Fit Selected Vertices" );
	}

	private final AbstractNamedAction fitSelectedVerticesAction;

	private MastodonPluginAppModel pluginAppModel;

	public FitEllipsoidPlugin()
	{
		fitSelectedVerticesAction = new RunnableAction( FIT_SELECTED_VERTICES, this::fitSelectedVertices );
		updateEnabledActions();
	}

	@Override
	public void setAppModel( final MastodonPluginAppModel model )
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

	private void fitSelectedVertices()
	{
		// TODO: parameters
		final int setupId = 0;


		System.out.println( "fitSelectedVertices()" );
		if ( pluginAppModel != null )
		{
			final MamutAppModel appModel = pluginAppModel.getAppModel();
			final AbstractSpimData< ? > spimData = appModel.getSharedBdvData().getSpimData();
			final BasicSetupImgLoader< ? > imgLoader = spimData.getSequenceDescription().getImgLoader().getSetupImgLoader( setupId );
			if ( !( imgLoader.getImageType() instanceof RealType ) )
				throw new IllegalArgumentException( "Expected EealType image source" );

			process( ( BasicSetupImgLoader ) imgLoader, setupId );

			System.out.println( "fitSelectedVertices()" );
		}
	}

	private static final boolean DEBUG = false;

	private < T extends RealType< T > > void process( BasicSetupImgLoader< T > imgLoader, final int setupId )
	{
		// TODO: parameters -----------------
		final double smoothSigma = 2;

		final double minGradientMagnitude = 10;

		final double maxAngle = 5 * Math.PI / 180.0;
		final double maxFactor = 1.1;

		final int numSamples = 100;
		final double outsideCutoffDistance = 3;
		final double insideCutoffDistance = 5;
		final double angleCutoffDistance = 30 * Math.PI / 180.0;
		final double maxCenterDistance = 10;
		// ----------------------------------




		final MamutAppModel appModel = pluginAppModel.getAppModel();
		final AbstractSpimData< ? > spimData = appModel.getSharedBdvData().getSpimData();
		final List< TimePoint > timePointsOrdered = spimData.getSequenceDescription().getTimePoints().getTimePointsOrdered();
		final ViewRegistrations viewReg = spimData.getViewRegistrations();

		final RefSet< Spot > vertices = appModel.getSelectionModel().getSelectedVertices();
		if ( vertices.isEmpty() )
			System.err.println( "no vertex selected" );

		for ( final Spot spot : vertices )
		{
			final int timepointId = timePointsOrdered.get( spot.getTimepoint() ).getId();
			final AffineTransform3D sourceToGlobal = viewReg.getViewRegistration( timepointId, setupId ).getModel();

			final double[] gCenter = new double[ 3 ];
			final double[] lCenter = new double[ 3 ];
			spot.localize( gCenter );
			sourceToGlobal.applyInverse( lCenter, gCenter );

			final double[] scale = new double[ 3 ];
			for ( int d = 0; d < 3; ++d )
				scale[ d ] = Affine3DHelpers.extractScale( sourceToGlobal, d );


			final long[] lMin = new long[ 3 ];
			final long[] lMax = new long[ 3 ];
			final double radius = Math.sqrt( spot.getBoundingSphereRadiusSquared() );
			for ( int d = 0; d < 3; ++d )
			{
				final double halfsize = 2 * radius / scale[ d ] + 2;
				lMin[ d ] = ( long ) ( lCenter[ d ] - halfsize );
				lMax[ d ] = ( long ) ( lCenter[ d ] + halfsize );
			}

			RandomAccessibleInterval< T > cropped = Views.interval( Views.extendBorder( imgLoader.getImage( timepointId ) ), lMin, lMax );
			final RandomAccessibleInterval< FloatType > converted = Converters.convert( cropped, new RealFloatConverter<>(), new FloatType() );


			final RandomAccessibleInterval< FloatType > input;
			if ( smoothSigma > 0 )
			{
				final RandomAccessibleInterval< FloatType > img = ArrayImgs.floats( longArrayFrom( cropped, Interval::dimensions ) );
				final double[] sigmas = new double[ 3 ];
				for ( int d = 0; d < 3; ++d )
					sigmas[ d ] = smoothSigma / scale[ d ];
				try
				{
					Gauss3.gauss( sigmas, Views.zeroMin( converted ), img );
				}
				catch ( IncompatibleTypeException e )
				{
				}
				input = Views.translate( img, lMin );
			}
			else
				input = converted;

			Bdv bdv;
			if ( DEBUG )
			{
				final BdvStackSource< FloatType > inputSource = BdvFunctions.show( input, "FloatType input", Bdv.options().sourceTransform( sourceToGlobal ) );
				final ConverterSetup cs = appModel.getSharedBdvData().getConverterSetups().get( setupId );
				final MinMaxGroup mmg = appModel.getSharedBdvData().getSetupAssignments().getMinMaxGroup( cs );
				inputSource.setDisplayRange( cs.getDisplayRangeMin(), cs.getDisplayRangeMax() );
				inputSource.setDisplayRangeBounds( mmg.getRangeMin(), mmg.getRangeMax() );
				bdv = inputSource;
			}

			final ArrayList< Edgel > lEdgels = SubpixelEdgelDetection.getEdgels( Views.zeroMin( input ), new ArrayImgFactory<>(), minGradientMagnitude );
			AffineTransform3D zeroMinSourceToGlobal = sourceToGlobal.copy();
			AffineTransform3D shiftToMin = new AffineTransform3D();
			shiftToMin.translate( lMin[ 0 ], lMin[ 1 ], lMin[ 2 ] );
			zeroMinSourceToGlobal.concatenate( shiftToMin );
			final ArrayList< Edgel > gEdgels = Edgels.transformEdgels( lEdgels, zeroMinSourceToGlobal );
			final ArrayList< Edgel > filteredEdgels = Edgels.filterEdgelsByOcclusion( Edgels.filterEdgelsByDirection( gEdgels, gCenter ), gCenter, maxAngle, maxFactor );

			EdgelsOverlay edgelsOverlay;
			if( DEBUG )
			{
				edgelsOverlay = new EdgelsOverlay( filteredEdgels, 0.01 );
				BdvFunctions.showOverlay( edgelsOverlay, "filtered edgels", Bdv.options().addTo( bdv ) );
			}

			final long t1 = System.currentTimeMillis();
			final Ellipsoid ellipsoid = SampleEllipsoidEdgel.sample(
					filteredEdgels,
					gCenter,
					numSamples,
					outsideCutoffDistance,
					insideCutoffDistance,
					angleCutoffDistance,
					maxCenterDistance );
			final long t2 = System.currentTimeMillis();
			System.out.println( t2 - t1 );

			if ( DEBUG )
			{
				BdvFunctions.showOverlay( new EllipsoidOverlay( ellipsoid ), "fitted ellipsoid", Bdv.options().addTo( bdv ) );
				final Map< Edgel, Double > costs = SampleEllipsoidEdgel.getCosts(
						filteredEdgels,
						ellipsoid,
						outsideCutoffDistance,
						insideCutoffDistance,
						angleCutoffDistance );
				edgelsOverlay.setCosts( costs );
			}

			spot.setPosition( ellipsoid );
			spot.setCovariance( ellipsoid.getCovariance() );
		}
	}

	// TODO: move to imglib2 core and make versions for int[], double[] ???
	public static < T extends EuclideanSpace > long[] longArrayFrom( T t, BiConsumer< T, long[] > get )
	{
		long[] a = new long[ t.numDimensions() ];
		get.accept( t, a );
		return a;
	}

	public static void main( final String[] args ) throws Exception
	{
		Locale.setDefault( Locale.US );
		UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );

		final Mastodon mastodon = new Mastodon();
		new Context().inject( mastodon );
		mastodon.run();

		final MamutProject project = new MamutProjectIO().load( "/Users/pietzsch/Desktop/Mastodon/testdata/MaMut_Parhyale_demo" );
		mastodon.openProject( project );
	}
}
