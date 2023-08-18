package org.mastodon.mamut.fitting.stardist;

import org.ejml.simple.SimpleMatrix;

public class Point3D extends SimpleMatrix
{
    Point3D( double x, double y, double z )
    {
        super( 3, 1, true, x, y, z );
    }

    Point3D elementDiv( Point3D other )
    {
        return new Point3D( get( 0 ) / other.get( 0 ), get( 1 ) / other.get( 1 ), get( 2 ) / other.get( 2 ) );
    }
}
