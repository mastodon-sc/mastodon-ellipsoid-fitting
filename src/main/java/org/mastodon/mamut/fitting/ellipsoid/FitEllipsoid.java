/*-
 * #%L
 * mastodon-ellipsoid-fitting
 * %%
 * Copyright (C) 2015 - 2022 Tobias Pietzsch, Jean-Yves Tinevez
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
import bdv.tools.brightness.ConverterSetup;
import bdv.util.Affine3DHelpers;
import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvStackSource;
import bdv.util.Bounds;
import bdv.viewer.ConverterSetups;
import bdv.viewer.SourceAndConverter;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.edge.Edgel;
import net.imglib2.algorithm.edge.SubpixelEdgelDetection;
import net.imglib2.algorithm.gauss3.Gauss3;
import net.imglib2.converter.Converters;
import net.imglib2.converter.RealFloatConverter;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.LinAlgHelpers;
import net.imglib2.view.Views;
import org.mastodon.mamut.MamutAppModel;
import org.mastodon.mamut.fitting.FitEllipsoidPlugin;
import org.mastodon.mamut.fitting.edgel.Edgels;
import org.mastodon.mamut.fitting.edgel.SampleEllipsoidEdgel;
import org.mastodon.mamut.fitting.ui.EdgelsOverlay;
import org.mastodon.mamut.fitting.ui.EllipsoidOverlay;
import org.mastodon.mamut.model.Spot;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Map;

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
	 * to an n * 3 array of coordinates.
	 * </p>
	 *
	 * @param points
	 *            the 2D array of the points to fit.
	 * @return a new {@link Ellipsoid} object or {@code null} if the fit failed.
	 * @throws IllegalArgumentException if there are less than 9 points in the given array.
	 *
	 * @see <a href=
	 *      "http://www.mathworks.com/matlabcentral/fileexchange/24693-ellipsoid-fit"
	 *      >MATLAB script</a>
	 */
	@Nullable
	public static Ellipsoid yuryPetrov( final double[][] points ) throws IllegalArgumentException
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
		CholeskyDecomposition choleskyDecomposition = new CholeskyDecomposition( new Matrix( DTD ) );

		// symmetric, positive, definite?
		if ( !choleskyDecomposition.isSPD() )
			// in this case, ellipsoid fitting is not possible
			return null;
		final Matrix V = choleskyDecomposition.solve( new Matrix( b ) );
		Ellipsoid ellipsoid = ellipsoidFromEquation( V );
		final double[] radii = ellipsoid.getRadii();
		// if any of the radii is NaN, the fit failed, and we return null
		if ( Double.isNaN( radii[ 0 ] ) || Double.isNaN( radii[ 1 ] ) || Double.isNaN( radii[ 2 ] ) )
			return null;
		return ellipsoid;
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

	/**
	 * Try to fit an ellipsoid around the center of the given spot.
	 *
	 * @param spot the spot to fit
	 * @param source the image source
	 * @param sourceToGlobal the source to global transform
	 * @param appModel the app model
	 * @param isDebug whether to show debug images
	 * @return the fitted ellipsoid, or null if the fit failed.
	 */
	public static < T extends RealType< T > > Ellipsoid getFittedEllipsoid( final Spot spot, final SourceAndConverter< T > source,
			final AffineTransform3D sourceToGlobal, final MamutAppModel appModel, boolean isDebug )
	{
		// TODO: parameters -----------------
		final double smoothSigma = 2;

		final double minGradientMagnitude = 10;

		final double maxAngle = 5 * Math.PI / 180.0;
		final double maxFactor = 1.1;

		// TODO: test with different values, e.g. 100 and 1000 and see how many ellipsoids are found
		final int numSamples = 1000;
		final double outsideCutoffDistance = 3;
		final double insideCutoffDistance = 5;
		final double angleCutoffDistance = 30 * Math.PI / 180.0;
		final double maxCenterDistance = 10;
		// ----------------------------------

		final int timepoint = spot.getTimepoint();
		source.getSpimSource().getSourceTransform( timepoint, 0, sourceToGlobal );

		final double[] centerInGlobalCoordinates = new double[ 3 ];
		final double[] centerInLocalCoordinates = new double[ 3 ];
		spot.localize( centerInGlobalCoordinates );
		sourceToGlobal.applyInverse( centerInLocalCoordinates, centerInGlobalCoordinates );

		final double[] scale = new double[ 3 ];
		for ( int d = 0; d < 3; ++d )
			scale[ d ] = Affine3DHelpers.extractScale( sourceToGlobal, d );

		final long[] lMin = new long[ 3 ];
		final long[] lMax = new long[ 3 ];

		final double radius = Math.sqrt( spot.getBoundingSphereRadiusSquared() );
		for ( int d = 0; d < 3; ++d )
		{
			final double halfsize = 2 * radius / scale[ d ] + 2;
			lMin[ d ] = ( long ) ( centerInLocalCoordinates[ d ] - halfsize );
			lMax[ d ] = ( long ) ( centerInLocalCoordinates[ d ] + halfsize );
		}

		final RandomAccessibleInterval< T > cropped = Views
				.interval( Views.extendBorder( source.getSpimSource().getSource( timepoint, 0 ) ), lMin, lMax );
		final RandomAccessibleInterval< FloatType > converted =
				Converters.convert( cropped, new RealFloatConverter<>(), new FloatType() );

		final RandomAccessibleInterval< FloatType > input;
		if ( smoothSigma > 0 )
		{
			long[] widthHeightDepth = FitEllipsoidPlugin.longArrayFrom( cropped, Interval::dimensions );
			final RandomAccessibleInterval< FloatType > img = ArrayImgs.floats( widthHeightDepth );
			final double[] sigmas = new double[ 3 ];
			for ( int d = 0; d < 3; ++d )
				sigmas[ d ] = smoothSigma / scale[ d ];
			try
			{
				Gauss3.gauss( sigmas, Views.extendMirrorSingle( Views.zeroMin( converted ) ), img );
			}
			catch ( final IncompatibleTypeException e )
			{
				e.printStackTrace();
			}
			input = Views.translate( img, lMin );
		}
		else
			input = converted;

		final ArrayList< Edgel > lEdgels = SubpixelEdgelDetection.getEdgels( Views.zeroMin( input ),
				new ArrayImgFactory<>( new FloatType() ), minGradientMagnitude );
		final AffineTransform3D zeroMinSourceToGlobal = sourceToGlobal.copy();
		final AffineTransform3D shiftToMin = new AffineTransform3D();
		shiftToMin.translate( lMin[ 0 ], lMin[ 1 ], lMin[ 2 ] );
		zeroMinSourceToGlobal.concatenate( shiftToMin );
		final ArrayList< Edgel > gEdgels = Edgels.transformEdgels( lEdgels, zeroMinSourceToGlobal );
		final ArrayList< Edgel > filteredEdgels = Edgels.filterEdgelsByOcclusion(
				Edgels.filterEdgelsByDirection( gEdgels, centerInGlobalCoordinates ), centerInGlobalCoordinates,
				maxAngle, maxFactor );

		final long t1 = System.currentTimeMillis();
		final Ellipsoid ellipsoid = SampleEllipsoidEdgel.sample(
				filteredEdgels,
				centerInGlobalCoordinates,
				numSamples,
				outsideCutoffDistance,
				insideCutoffDistance,
				angleCutoffDistance,
				maxCenterDistance );
		final long t2 = System.currentTimeMillis();

		if ( isDebug )
		{
			System.out.println( "Computed ellipsoid in " + ( t2 - t1 ) + "ms. Ellipsoid: " + ellipsoid );
			Bdv bdv;
			final BdvStackSource< FloatType > inputSource =
					BdvFunctions.show( input, "FloatType input", Bdv.options().sourceTransform( sourceToGlobal ) );
			final ConverterSetups setups = appModel.getSharedBdvData().getConverterSetups();
			final ConverterSetup cs = setups.getConverterSetup( source );
			final Bounds bounds = setups.getBounds().getBounds( cs );
			inputSource.setDisplayRange( cs.getDisplayRangeMin(), cs.getDisplayRangeMax() );
			inputSource.setDisplayRangeBounds( bounds.getMinBound(), bounds.getMaxBound() );
			bdv = inputSource;

			EdgelsOverlay edgelsOverlay;
			edgelsOverlay = new EdgelsOverlay( filteredEdgels, 0.01 );
			BdvFunctions.showOverlay( edgelsOverlay, "filtered edgels", Bdv.options().addTo( bdv ) );

			BdvFunctions.showOverlay( new EllipsoidOverlay( ellipsoid ), "fitted ellipsoid",
					Bdv.options().addTo( bdv ) );
			final Map< Edgel, Double > costs = SampleEllipsoidEdgel.getCosts(
					filteredEdgels,
					ellipsoid,
					outsideCutoffDistance,
					insideCutoffDistance,
					angleCutoffDistance );
			edgelsOverlay.setCosts( costs );
		}
		return ellipsoid;
	}
}
