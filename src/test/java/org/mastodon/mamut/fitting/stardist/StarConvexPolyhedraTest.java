package org.mastodon.mamut.fitting.stardist;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class StarConvexPolyhedraTest
{
	StarConvexPolyhedra unitPolyhedraAtZero;

	StarConvexPolyhedra tinyPolyhedraAtZero;

	StarConvexPolyhedra bigPolyhedraAt50;

	private final double[] centerZero = new double[] { 0, 0, 0 };

	double[] center50 = new double[] { 50, 50, 50 };

	private double[] pointInsideTinyPolyhedra;

	private double[] pointOutSideTinyPolyhedra;

	double[] shouldBeOutside = new double[] { 25, 12, 50 };

	private final Random random = new Random();

	@Before
	public void setUp()
	{
		random.setSeed( 1 );

		List< Double > distances = new ArrayList<>( Collections.nCopies( 96, 1d ) );
		unitPolyhedraAtZero = new StarConvexPolyhedra( centerZero, distances );

		distances = random.doubles( 96, 2, 5 )
				.boxed()
				.collect( Collectors.toList() );
		tinyPolyhedraAtZero = new StarConvexPolyhedra( centerZero, distances );
		distances = random.doubles( 96, 5, 40 )
				.boxed()
				.collect( Collectors.toList() );
		bigPolyhedraAt50 = new StarConvexPolyhedra( center50, distances );
		pointInsideTinyPolyhedra = new double[] { 1, 1, 1 };
		pointOutSideTinyPolyhedra = new double[] { 10, 20, 15 };
	}

	@Test
	public void testContains1()
	{
		assertTrue( tinyPolyhedraAtZero.contains( centerZero ) );
		assertTrue( tinyPolyhedraAtZero.contains( pointInsideTinyPolyhedra ) );
		assertFalse( tinyPolyhedraAtZero.contains( pointOutSideTinyPolyhedra ) );
	}

	@Test
	public void testContains()
	{
		random.setSeed( 1 );
		assertTrue( bigPolyhedraAt50.contains( center50 ) );
		assertFalse( bigPolyhedraAt50.contains( shouldBeOutside ) ); //
	}

	@Test
	public void testGetBoundingBox3D()
	{
		StarConvexPolyhedra.BoundingBox3D boundingBox3D = tinyPolyhedraAtZero.getBoundingBox3D();
		assertTrue( boundingBox3D.contains( centerZero ) );
		assertTrue( boundingBox3D.contains( pointInsideTinyPolyhedra ) );
		assertFalse( boundingBox3D.contains( pointOutSideTinyPolyhedra ) );
	}
}
