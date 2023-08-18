package org.mastodon.mamut.fitting.stardist2;

import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import org.ejml.simple.SimpleMatrix;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Candidates
{

    private final List< List< SimpleMatrix > > polygons = new ArrayList<>();

    private final List< Point3D > origins = new ArrayList<>();

    private final List< Float > scores = new ArrayList<>();

    private final List< Integer > winner = new ArrayList<>();

    // scale all coordinates by this value and divide later to get subpixel resolution
    private static final long S = 100;

    public Candidates( RandomAccessibleInterval< FloatType > prob, RandomAccessibleInterval< FloatType > dist )
    {
        this( prob, dist, 0.4 );
    }

    public Candidates( RandomAccessibleInterval< FloatType > prob, RandomAccessibleInterval< FloatType > dist, double threshold )
    {
        this( prob, dist, threshold, 2 );
    }

    public Candidates( RandomAccessibleInterval< FloatType > prob, RandomAccessibleInterval< FloatType > dist, double threshold, int b )
    {

        final long[] shape = Intervals.dimensionsAsLongArray( dist );
        final int ndim = shape.length;
        assert ndim == 5;

        int nRays = ( int ) shape[ 4 ];
        final List< Point3D > rays = getRays( nRays );

        final RandomAccess< FloatType > r = prob.randomAccess();
        final RandomAccess< FloatType > s = dist.randomAccess();

        for ( int ox = b; ox < shape[ 0 ] - b; ox++ )
        {
            for ( int oy = b; oy < shape[ 1 ] - b; oy++ )
            {
                for ( int oz = b; oy < shape[ 2 ] - b; oz++ )
                {
                    Point3D origin = new Point3D( ox, oy, oz );
                    final float score = r.setPositionAndGet( ox, oy, oz ).getRealFloat();
                    if ( score > threshold )
                    {
                        final List< SimpleMatrix > poly = new ArrayList<>();
                        for ( int rayIdx = 0; rayIdx < nRays; rayIdx++ )
                        {
                            double d = s.setPositionAndGet( ox, oy, oz, rayIdx ).getRealDouble();
                            poly.add( origin.plus( rays.get( rayIdx ).scale( d ).scale( S ) ) );
                        }
                        polygons.add( poly );
                        scores.add( score );
                        origins.add( origin );
                    }
                }
            }
        }
    }

    public static Double[] linSpace( double min, double max, int points )
    {
        Double[] d = new Double[ points ];
        for ( int i = 0; i < points; i++ )
        {
            d[ i ] = min + i * ( max - min ) / ( points - 1 );
        }
        return d;
    }

    public static Integer[] range( int n )
    {
        Integer[] ret = new Integer[ n ];
        for ( int i = 0; i < n; i++ )
            ret[ i ] = i;
        return ret;
    }

    private static List< Point3D > getRays( int nRays )
    {
        Point3D anisotropy = new Point3D( 2, 1, 1 );
        List< Double > z = Arrays.asList( linSpace( -1, 1, nRays ) );
        List< Double > rho = z.stream().map( a -> Math.sqrt( 1 - a * a ) ).collect( Collectors.toList() );
        final double g = Math.PI * ( 3 - Math.sqrt( 5 ) );
        final List< Integer > phi = Arrays.asList( range( nRays ) );
        ArrayList< Point3D > ret = new ArrayList< Point3D >( 96 );
        for ( int i = 0; i < nRays; i++ )
        {
            ret.add( new Point3D( z.get( i ), rho.get( i ) * Math.sin( phi.get( i ) ),
                    rho.get( i ) * Math.cos( phi.get( i ) )
            ).elementDiv( anisotropy ) );
        }
        return ret;
    }

    public List< List< SimpleMatrix > > getPolygons()
    {
        return polygons;
    }

    public List< Float > getScores()
    {
        return scores;
    }

    public List< Point3D > getOrigins()
    {
        return origins;
    }
}
