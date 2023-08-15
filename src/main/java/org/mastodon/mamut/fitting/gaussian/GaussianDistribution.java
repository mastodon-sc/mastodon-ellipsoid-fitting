package org.mastodon.mamut.fitting.gaussian;

import net.imglib2.Cursor;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.LinAlgHelpers;

import java.util.function.Predicate;

/**
 * Gaussian distribution fitting methods.
 *
 * @author Stefan Hahmann
 * @author Matthias Arzt
 */
public class GaussianDistribution
{
	/**
	 * Computes the mean position of the pixels, which are successfully evaluated by the given {@code evaluator}.
	 * @param image the image
	 * @param evaluator the evaluator
	 * @return the mean position
	 */
	public static double[] computeMean( Img< FloatType > image, Predicate< FloatType > evaluator )
	{
		Cursor< FloatType > cursor = image.cursor();
		double[] sum = new double[ 3 ];
		double[] position = new double[ 3 ];
		long counter = 0;
		while ( cursor.hasNext() )
			if ( evaluator.test( cursor.next() ) )
			{
				cursor.localize( position );
				LinAlgHelpers.add( sum, position, sum );
				counter++;
			}
		if ( counter == 0 )
			throw new IllegalArgumentException( "No pixels match the given predicate." );
		LinAlgHelpers.scale( sum, 1. / counter, sum );
		return sum;
	}

	/**
	 * Computes the covariance matrix of the pixels, which are successfully evaluated by the given {@code evaluator}.
	 * @param image the image
	 * @param evaluator the evaluator
	 * @return the mean position
	 */
	public static double[][] computeCovariance( Img< FloatType > image, double[] mean, Predicate< FloatType > evaluator )
	{
		Cursor< FloatType > cursor = image.cursor();
		long counter = 0;
		double[] position = new double[ 3 ];
		double[][] covariance = new double[ 3 ][ 3 ];
		cursor.reset();
		while ( cursor.hasNext() )
			if ( evaluator.test( cursor.next() ) )
			{
				cursor.localize( position );
				LinAlgHelpers.subtract( position, mean, position );
				for ( int i = 0; i < 3; i++ )
					for ( int j = 0; j < 3; j++ )
						covariance[ i ][ j ] += position[ i ] * position[ j ];
				counter++;
			}
		if ( counter == 0 )
			throw new IllegalArgumentException( "No pixels match the given predicate." );
		scale( covariance, 5. / counter ); // I don't know why the factor 5 is needed. But it works.
		return covariance;
	}

	private static void scale( double[][] covariance, double factor )
	{
		for ( int i = 0; i < 3; i++ )
			for ( int j = 0; j < 3; j++ )
				covariance[ i ][ j ] *= factor;
	}
}
