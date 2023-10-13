package org.mastodon.mamut.fitting.stardist;

import bdv.util.BdvFunctions;
import bdv.util.RandomAccessibleIntervalSource;
import bdv.viewer.Source;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.numeric.integer.ByteType;
import org.mastodon.mamut.feature.EllipsoidIterable;
import org.mastodon.mamut.model.Model;
import org.mastodon.mamut.model.Spot;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class StarConvexPolyhedraDemo
{
	public static void main( String[] args )
	{
		Img< ByteType > img = ArrayImgs.bytes( 200, 200, 200 );

		Source< ByteType > source = new RandomAccessibleIntervalSource<>( img, new ByteType(), "Star convex polyhedra demo" );

		Model model = new Model();
		Spot spot = model.getGraph().addVertex();
		double[] center = new double[] { 50, 100, 100 };
		spot.init( 0, center, 40 );
		EllipsoidIterable< ByteType > ellipsoidIterable = new EllipsoidIterable<>( source );

		StarConvexPolyhedraIterable< ByteType > polyhedraIterable = new StarConvexPolyhedraIterable<>( source );

		Random random = new Random();
		random.setSeed( 1 );
		List< Double > distances = random.doubles( 96, 5, 40 )
				.boxed()
				.collect( Collectors.toList() );
		StarConvexPolyhedra polyhedra = new StarConvexPolyhedra( new double[] { 150, 100, 100 }, distances );

		ellipsoidIterable.reset( spot );
		ellipsoidIterable.forEach( byteType -> {byteType.set( ( byte ) 100 );} );
		polyhedraIterable.reset( polyhedra, 0 );
		polyhedraIterable.forEach( byteType -> {byteType.set( ( byte ) 100 );} );

		BdvFunctions.show( img, "Star convex polyhedra demo" );
	}
}
