package org.mastodon.mamut.fitting.stardist;

import net.imglib2.util.LinAlgHelpers;
import org.jzy3d.chart.Chart;
import org.jzy3d.chart.ChartLauncher;
import org.jzy3d.chart.controllers.mouse.camera.AWTCameraMouseController;
import org.jzy3d.chart.factories.AWTChartFactory;
import org.jzy3d.chart.factories.IChartFactory;
import org.jzy3d.colors.Color;
import org.jzy3d.maths.Coord3d;
import org.jzy3d.plot3d.primitives.LineStrip;
import org.jzy3d.plot3d.primitives.Polygon;
import org.jzy3d.plot3d.primitives.Scatter;
import org.jzy3d.plot3d.primitives.Sphere;
import org.jzy3d.plot3d.rendering.canvas.Quality;
import org.jzy3d.plot3d.rendering.scene.Scene;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StarConvexPolyhedraVisualization
{

	public static void main( String[] args )
	{
		// Create a 3D scatter plot chart
		IChartFactory factory = new AWTChartFactory();
		Chart chart = new Chart( factory, Quality.Advanced() );
		Scene scene = chart.getScene();

		testLattice( scene );
		//testContains( scene );

		// Add mouse controllers for interaction (optional)
		chart.addController( new AWTCameraMouseController( chart ) );

		// Open the chart window
		ChartLauncher.openChart( chart );
	}

	private static void testContains( Scene scene )
	{
		StarConvexPolyhedraTest test = new StarConvexPolyhedraTest();
		test.setUp();
		StarConvexPolyhedra polyhedra = test.bigPolyhedraAt50;
		List< double[] > points = polyhedra.getPoints();
		List< Coord3d > pointsJzy = new ArrayList<>();
		points.forEach( point -> pointsJzy.add( new Coord3d( point ) ) );

		// Create a line strip to connect the points
		LineStrip lineStrip = new LineStrip();
		lineStrip.add( pointsJzy );
		lineStrip.setColor( Color.BLUE );
		scene.add( lineStrip );

		// Create a scatter plot
		Scatter scatterVertices = new Scatter( pointsJzy, Color.RED );
		scatterVertices.setWidth( 5.0f );
		scene.add( scatterVertices );

		// center
		Coord3d center = new Coord3d( polyhedra.getCenter() );
		Scatter scatterCenter = new Scatter( Collections.singletonList( center ), Color.GREEN );
		scatterCenter.setWidth( 20f );
		scene.add( scatterCenter );

		// test point
		Coord3d testPoint = new Coord3d( test.shouldBeOutside );
		Scatter scatterTestPoint = new Scatter( Collections.singletonList( testPoint ), Color.BLUE );
		scatterTestPoint.setWidth( 20f );
		scene.add( scatterTestPoint );

		// nearest points
		List< Coord3d > nearestPoints = new ArrayList<>();
		polyhedra.findNearestPoints( test.shouldBeOutside ).forEach( point -> {
			Coord3d coord3d = new Coord3d( point );
			nearestPoints.add( coord3d );
		} );
		Scatter scatterNearestPoints = new Scatter( nearestPoints, Color.MAGENTA );
		scatterNearestPoints.setWidth( 20f );
		scene.add( scatterNearestPoints );

	}

	private static void testLattice( Scene scene )
	{
		Sphere sphere = new Sphere( new Coord3d( 0, 0, 0 ), 1f, 10, Color.GREEN );
		scene.add( sphere );

		// Create point data
		StarConvexPolyhedraTest test = new StarConvexPolyhedraTest();
		test.setUp();
		StarConvexPolyhedra polyhedra = test.unitPolyhedraAtZero;
		List< Coord3d > latticeJzy = new ArrayList<>();
		polyhedra.getLattice().forEach( point -> latticeJzy.add( new Coord3d( point ) ) );

		// Create a line strip to connect the points
		LineStrip lineStrip = new LineStrip();
		lineStrip.add( latticeJzy );
		lineStrip.setColor( Color.BLUE );
		scene.add( lineStrip );

		// Create a scatter plot
		Scatter latticeScatter = new Scatter( latticeJzy, Color.RED );
		latticeScatter.setWidth( 5.0f );
		scene.add( latticeScatter );

		double[] center = new double[] { 0, 0, 0 };
		double[] candidate = new double[] { 0, 2, 0 };
		double[] candidateOnSurface = new double[ 3 ];
		LinAlgHelpers.subtract( candidateOnSurface, center, candidateOnSurface );
		LinAlgHelpers.normalize( candidateOnSurface );

		List< double[] > nearestPoints = polyhedra.findNearestPoints( candidate );
		List< Coord3d > nearestPointsJzy = new ArrayList<>();
		nearestPoints.forEach( point -> nearestPointsJzy.add( new Coord3d( point ) ) );

		Coord3d centerJzy = new Coord3d( center );
		Coord3d candidateJzy = new Coord3d( candidate );
		Coord3d candidateOnSurfaceJzy = new Coord3d( candidateOnSurface );

		Scatter centerScatter = new Scatter( Collections.singletonList( centerJzy ), Color.GREEN );
		centerScatter.setWidth( 20f );
		scene.add( centerScatter );

		Scatter candidateScatter = new Scatter( Collections.singletonList( candidateJzy ), Color.BLUE );
		candidateScatter.setWidth( 20f );
		scene.add( candidateScatter );

		Scatter candidateOnSurfaceScatter = new Scatter( Collections.singletonList( candidateOnSurfaceJzy ), Color.YELLOW );
		candidateOnSurfaceScatter.setWidth( 20f );
		scene.add( candidateOnSurfaceScatter );

		Scatter nearestPointsScatter = new Scatter( nearestPointsJzy, Color.MAGENTA );
		nearestPointsScatter.setWidth( 20f );
		scene.add( nearestPointsScatter );

		Polygon triangle = new Polygon();
		nearestPointsJzy.forEach( triangle::add );
		scene.add( triangle );

		boolean isInside = polyhedra.contains( candidate );
		System.out.println( "candidate inside = " + isInside );
		isInside = polyhedra.contains( center );
		System.out.println( "center inside = " + isInside );
	}
}
