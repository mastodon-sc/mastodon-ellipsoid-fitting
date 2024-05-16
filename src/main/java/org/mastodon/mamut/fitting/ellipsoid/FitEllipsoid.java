/*-
 * #%L
 * mastodon-ellipsoid-fitting
 * %%
 * Copyright (C) 2015 - 2024 Tobias Pietzsch, Jean-Yves Tinevez
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package org.mastodon.mamut.fitting.ellipsoid;

import Jama.CholeskyDecomposition;
import Jama.Matrix;
import net.imglib2.util.LinAlgHelpers;

/**
 * Adapted from BoneJ's FitEllipsoid.
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
public class FitEllipsoid
{
	/**
	 * <p>
	 * Ellipsoid fitting method by Yury Petrov.<br>
	 * Fits an ellipsoid in the form <i>Ax</i><sup>2</sup> +
	 * <i>By</i><sup>2</sup> + <i>Cz</i><sup>2</sup> + 2<i>Dxy</i> + 2<i>Exz</i>
	 * + 2<i>Fyz</i> + 2<i>Gx</i> + 2<i>Hy</i> + 2<i>Iz</i> = 1 <br>
	 * To an n * 3 array of coordinates.
	 * </p>
	 * 
	 * @param points
	 *            the 2D array of the points to fit.
	 * @return a new {@link Ellipsoid} object.
	 *
	 * @throws IllegalArgumentException if there are less than 9 points.
	 *
	 * @throws RuntimeException it the given points don't form a unique ellipsoid.
	 *            This happens for example if the points are all on one plane.
	 *
	 * @see <a href=
	 *      "http://www.mathworks.com/matlabcentral/fileexchange/24693-ellipsoid-fit"
	 *      >MATLAB script</a>
	 */
	public static Ellipsoid yuryPetrov( final double[][] points )
	{
		final int nPoints = points.length;
		if ( nPoints < 9 )
			throw new IllegalArgumentException( "Too few points; need at least 9 to calculate a unique ellipsoid" );

		final double[][] d = new double[ nPoints ][ 9 ];
		final double[][] b = new double[ 9 ][ 1 ];
		for (int i = 0; i < nPoints; i++) {
			final double x = points[i][0];
			final double y = points[i][1];
			final double z = points[i][2];
			d[i][0] = x * x;
			d[i][1] = y * y;
			d[i][2] = z * z;
			d[i][3] = 2 * x * y;
			d[i][4] = 2 * x * z;
			d[i][5] = 2 * y * z;
			d[i][6] = 2 * x;
			d[i][7] = 2 * y;
			d[i][8] = 2 * z;
			for ( int j = 0; j < 9; ++j )
				b[j][0] += d[i][j];
		}

		final double[][] DTD = new double[ 9 ][ 9 ];
		LinAlgHelpers.multATB( d, d, DTD );
		final Matrix V = new CholeskyDecomposition( new Matrix( DTD ) ).solve( new Matrix( b ) );
		return ellipsoidFromEquation( V );
	}

	/**
	 * Calculate the matrix representation of the ellipsoid from the equation variables
	 * <i>ax</i><sup>2</sup> + <i>by</i><sup>2</sup> + <i>cz</i><sup>2</sup> +
	 * 2<i>dxy</i> + 2<i>exz</i> + 2<i>fyz</i> + 2<i>gx</i> + 2<i>hy</i> +
	 * 2<i>iz</i> = 1 <br />
	 *
	 * @param V vector (a,b,c,d,e,f,g,h,i)
	 * @return the ellipsoid.
	 */
	private static Ellipsoid ellipsoidFromEquation( final Matrix V )
	{
		final double a = V.get( 0, 0 );
		final double b = V.get( 1, 0 );
		final double c = V.get( 2, 0 );
		final double d = V.get( 3, 0 );
		final double e = V.get( 4, 0 );
		final double f = V.get( 5, 0 );
		final double g = V.get( 6, 0 );
		final double h = V.get( 7, 0 );
		final double i = V.get( 8, 0 );

		final double[][] aa = new double[][] {
				{ a, d, e },
				{ d, b, f },
				{ e, f, c } };
		final double[] bb = new double[] { g, h, i };
		final double[] cc = new Matrix( aa ).solve( new Matrix( bb, 3 ) ).getRowPackedCopy();
		LinAlgHelpers.scale( cc, -1, cc );

		final double[] At = new double[ 3 ];
		LinAlgHelpers.mult( aa, cc, At );
		final double r33 = LinAlgHelpers.dot( cc, At ) + 2 * LinAlgHelpers.dot( bb, cc ) - 1;
		LinAlgHelpers.scale( aa, -1 / r33, aa );

		return new Ellipsoid( cc, null, aa, null, null );
	}
}
