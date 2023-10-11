package org.mastodon.mamut.fitting.stardist;

import org.apache.commons.math3.geometry.euclidean.threed.Line;
import org.apache.commons.math3.geometry.euclidean.threed.Plane;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class LinePlaneIntersectionMath3Test
{
	@Test
	public void testLinePlaneIntersection()
	{
		Vector3D origin = new Vector3D( 0, 0, 0 );
		Vector3D normal = new Vector3D( 0, 0, 1 );
		Plane plane = new Plane( origin, normal, 0d );

		Vector3D point1 = new Vector3D( 0, 0, -1 );
		Vector3D point2 = new Vector3D( 0, 0, 1 );
		Line line = new Line( point1, point2, 0d );

		Vector3D intersectionPoint = plane.intersection( line );

		assertNotNull( intersectionPoint );
		assertEquals( 0, intersectionPoint.getX(), 0d );
		assertEquals( 0, intersectionPoint.getY(), 0d );
		assertEquals( 0, intersectionPoint.getZ(), 0d );
	}
}
