package org.mastodon.mamut.fitting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.mastodon.mamut.fitting.ui.EdgelsOverlay;

import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvSource;
import ij.IJ;
import ij.ImagePlus;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.edge.Edgel;
import net.imglib2.algorithm.edge.SubpixelEdgelDetection;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealViews;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

public class DebugEdgels
{
	public static void main( String[] args )
	{
		final ImagePlus imp_crop = IJ.openImage( "/Users/pietzsch/Downloads/crop.tif" );
		System.out.println( "imp_crop = " + imp_crop );
		final RandomAccessibleInterval< UnsignedShortType > crop = ImageJFunctions.wrapShort( imp_crop );

		final ImagePlus imp_input = IJ.openImage( "/Users/pietzsch/Downloads/input.tif" );
		System.out.println( "imp_input = " + imp_input );
		final RandomAccessibleInterval< FloatType > input = ImageJFunctions.wrapFloat( imp_input );

		final double[] calibration = { 0.1, 0.1, 1 };

		final BdvSource bdv_input = BdvFunctions.show( input, "input",
				Bdv.options().sourceTransform( calibration ) );
		final BdvSource bdv_crop = BdvFunctions.show( crop, "crop",
				Bdv.options().addTo( bdv_input ).sourceTransform( calibration ) );

		bdv_input.setDisplayRangeBounds( 0, 255 );
		bdv_input.setDisplayRange( 30, 70 );
		bdv_input.setColor( new ARGBType( 0xff00ff ) );
		bdv_crop.setDisplayRangeBounds( 0, 255 );
		bdv_crop.setColor( new ARGBType( 0x00ff00 ) );
		bdv_crop.setActive( false );

		final double minGradientMagnitude = 1;
		final ArrayImgFactory< FloatType > arrayImgFactory = new ArrayImgFactory<>( new FloatType() );

		final ArrayList< Edgel > badEdgels = SubpixelEdgelDetection.getEdgels(
				input,
				arrayImgFactory,
				minGradientMagnitude );
		System.out.println( "bad edgels.size() = " + badEdgels.size() );
		final List< Edgel > someBadEdgels = randomSample( badEdgels, 1000 );

		final EdgelsOverlay badEdgelsOverlay = new EdgelsOverlay( someBadEdgels, 0.1 );
		BdvFunctions.showOverlay( badEdgelsOverlay, "someBadEdgels",
				Bdv.options().addTo( bdv_input ).sourceTransform( calibration ) );


		// ====================================================================


		final AffineTransform3D transform = new AffineTransform3D();
		transform.scale( calibration[ 0 ], calibration[ 1 ], calibration[ 2 ] );
		final Interval interval = Intervals.createMinSize( 0, 0, 0,
				( int ) ( input.dimension( 0 ) * calibration[ 0 ] ),
				( int ) ( input.dimension( 1 ) * calibration[ 1 ] ),
				( int ) ( input.dimension( 2 ) * calibration[ 2 ] ) );
		final RandomAccessibleInterval< FloatType > stretched =
				Views.interval(
						RealViews.affine(
								Views.interpolate(
										Views.extendBorder( input ),
										new NLinearInterpolatorFactory<>() ),
								transform ),
						interval );

		final BdvSource bdv_stretched = BdvFunctions.show( stretched, "stretched",
				Bdv.options().addTo( bdv_input ) );

		bdv_stretched.setDisplayRangeBounds( 0, 255 );
		bdv_stretched.setDisplayRange( 30, 70 );
		bdv_stretched.setColor( new ARGBType( 0x00ff00 ) );


		final ArrayList< Edgel > goodEdgels = SubpixelEdgelDetection.getEdgels(
				stretched,
				arrayImgFactory,
				minGradientMagnitude * calibration[ 2 ] );
		System.out.println( "good edgels.size() = " + goodEdgels.size() );
		final List< Edgel > someGoodEdgels = randomSample( goodEdgels, 1000 );

		final EdgelsOverlay goodEdgelsOverlay = new EdgelsOverlay( someGoodEdgels, 0.1 );
		BdvFunctions.showOverlay( goodEdgelsOverlay, "someGoodEdgels",
				Bdv.options().addTo( bdv_input ) );

	}

	private static <T> List<T> randomSample( final Collection<T> list, final int n)
	{
		if ( n > list.size() )
			return randomSample( list, list.size() );

		final List< T > all = new ArrayList<>( list );
		final List< T > sample = new ArrayList<>( n );
		final Random r = new Random();
		for ( int i = 0; i < n; i++ )
			sample.add( all.remove( r.nextInt( all.size() ) ) );
		return sample;
	}
}
