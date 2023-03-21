package org.mastodon.mamut.fitting.ellipsoid;

import bdv.viewer.SourceAndConverter;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.real.FloatType;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mastodon.mamut.MamutAppModel;
import org.mastodon.mamut.fitting.BlobRenderingUtils;
import org.mastodon.mamut.model.Model;
import org.mastodon.mamut.model.Spot;
import org.mastodon.mamut.plugin.MamutPlugins;
import org.mastodon.ui.coloring.feature.FeatureColorModeManager;
import org.mastodon.ui.keymap.Keymap;
import org.mastodon.ui.keymap.KeymapManager;
import org.mastodon.views.bdv.SharedBigDataViewerData;
import org.mastodon.views.bdv.overlay.ui.RenderSettingsManager;
import org.mastodon.views.bdv.overlay.util.JamaEigenvalueDecomposition;
import org.mastodon.views.grapher.display.style.DataDisplayStyleManager;
import org.mastodon.views.trackscheme.display.style.TrackSchemeStyleManager;
import org.scijava.ui.behaviour.KeyPressedManager;
import org.scijava.ui.behaviour.util.Actions;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.Assert.assertTrue;

public class FitEllipsoidTest
{
	private static MamutAppModel appModel;

	private static Model model;

	private static Img< FloatType > image;

	private static SharedBigDataViewerData sharedBigDataViewerData;

	@BeforeClass
	public static void setUp()
	{
		Keymap keymap = new Keymap();
		image = ArrayImgs.floats( 100, 100, 100 );
		sharedBigDataViewerData = BlobRenderingUtils.asSharedBdvDataXyz( image );
		model = new Model();
		appModel = new MamutAppModel( model, sharedBigDataViewerData, new KeyPressedManager(), new TrackSchemeStyleManager(),
				new DataDisplayStyleManager(), new RenderSettingsManager(), new FeatureColorModeManager(), new KeymapManager(),
				new MamutPlugins( keymap ), new Actions( keymap.getConfig() ) );
	}

	@Test
	public void testSingleEllipsoid()
	{
		// input covariance matrix
		double[][] inputCovarianceMatrix = {
				{ 400, 100, 300 },
				{ 100, 400, 200 },
				{ 300, 200, 400 }
		};
		double[] inputCenter = { 50, 50, 50 };
		double maxPercentageDifference = 0.35d;
		double maxCenterOffset = 10;
		int numberOfFits = 1000;

		assertTrue( isFittingSuccessful( inputCovarianceMatrix, inputCenter, maxPercentageDifference, maxCenterOffset,
				numberOfFits ) );
	}

	@Test
	public void testMultipleEllipsoid()
	{
		String disclaimer = "This test may fail due to statistical reasons. Try running this test again, if it fails.";
		int numberOfTests = 100;

		int numberOfFits = 1000;
		int successFulTests1 = runTests( 0.3d, 10, numberOfFits, numberOfTests );
		// Nota bene: only 33% of the tests have to be successful to pass this test. This is due to the fact that the ellipsoid fitting has poor performance.
		double acceptedPercentageOfSuccessfulTests = 0.33d;
		assertTrue( disclaimer, successFulTests1 > numberOfTests * acceptedPercentageOfSuccessfulTests );

		numberOfFits = 100;
		int successFulTests2 = runTests( 0.3d, 10, numberOfFits, numberOfTests );
		// Nota bene: only 20% of the tests have to be successful to pass this test. This is due to the fact that the ellipsoid fitting has poor performance.
		acceptedPercentageOfSuccessfulTests = 0.25d;
		assertTrue( disclaimer, successFulTests2 > numberOfTests * acceptedPercentageOfSuccessfulTests );

		assertTrue( disclaimer, successFulTests1 > successFulTests2 );
	}

	/**
	 *
	 * @param acceptedMaxPercentagedDifference max relative difference between the axes of the input and the fitted ellipsoid
	 * @param acceptedMaxCenterOffset max offset of the center of the fitted ellipsoid to the center of the input ellipsoid in pixels. Evaluated in the x, y and z direction.
	 * @param numberOfFits number of fits to be performed to find the best fit
	 * @param numberOfTests number of tests to be performed
	 * @return number of successful fits according to the given parameters
	 */
	private static int runTests( double acceptedMaxPercentagedDifference, double acceptedMaxCenterOffset, int numberOfFits,
			int numberOfTests )
	{
		int countSuccessfulFits = 0;
		for ( int i = 0; i < numberOfTests; i++ )
		{
			double cov00 = ThreadLocalRandom.current().nextInt( 64, 400 );
			double cov01 = ThreadLocalRandom.current().nextInt( 20, 50 );
			double cov02 = ThreadLocalRandom.current().nextInt( 20, 50 );
			double cov12 = ThreadLocalRandom.current().nextInt( 20, 50 );
			// input covariance matrix
			double[][] inputCovarianceMatrix = {
					{ cov00, cov01, cov02 },
					{ cov01, cov00, cov12 },
					{ cov02, cov12, cov00 }
			};
			double center = ThreadLocalRandom.current().nextInt( 40, 60 );
			double[] inputCenter = { center, center, center };
			boolean successFulFit =
					isFittingSuccessful( inputCovarianceMatrix, inputCenter, acceptedMaxPercentagedDifference,
							acceptedMaxCenterOffset, numberOfFits );
			if ( successFulFit )
				countSuccessfulFits++;
			System.out.println( "Successful fits: " + countSuccessfulFits + "/" + ( i + 1 ) );
		}
		return countSuccessfulFits;
	}

	private static boolean isFittingSuccessful( double[][] inputCovarianceMatrix, double[] inputCenter,
			double maxPercentageDifference, double maxCenterOffset, int numberOfFits )
	{
		BlobRenderingUtils.renderMultivariateNormalDistribution( inputCenter, inputCovarianceMatrix, image );
		// method init generates a spot
		model.getGraph().addVertex().init( 0, inputCenter, inputCovarianceMatrix );
		final AffineTransform3D sourceToGlobal = new AffineTransform3D();
		Spot spot = model.getGraph().vertices().iterator().next();
		final SourceAndConverter< ? > source = sharedBigDataViewerData.getSources().get( 0 );
		@SuppressWarnings( { "unchecked", "rawtypes" } )
		Ellipsoid fittedEllipsoid =
				FitEllipsoid.getFittedEllipsoid( spot, ( SourceAndConverter ) source, sourceToGlobal, appModel, numberOfFits, false,
						false );
		model.getGraph().remove( spot );
		if ( fittedEllipsoid == null )
		{
			System.out.println( "fittedEllipsoid == null" );
			return false;
		}
		Triple< Double, Double, Double > expectedAxes = getEigenvalues( inputCovarianceMatrix );
		Triple< Double, Double, Double > fittedAxes = getEigenvalues( fittedEllipsoid.getCovariance() );

		double relativeDifferenceShortAxis = Math.abs( 1 - ( expectedAxes.getLeft() / fittedAxes.getLeft() ) );
		double relativeDifferenceMiddleAxis = Math.abs( 1 - ( expectedAxes.getMiddle() / fittedAxes.getMiddle() ) );
		double relativeDifferenceLongAxis = Math.abs( 1 - ( expectedAxes.getRight() / fittedAxes.getRight() ) );
		double meanRelativeDifferenceOfAxes =
				( relativeDifferenceShortAxis + relativeDifferenceMiddleAxis + relativeDifferenceLongAxis ) / 3;
		System.out.println( "expectedAxes: " + expectedAxes.getLeft() + ", " + expectedAxes.getMiddle() + ", " + expectedAxes.getRight() );
		System.out.println( "fittedAxes: " + fittedAxes.getLeft() + ", " + fittedAxes.getMiddle() + ", " + fittedAxes.getRight() );
		System.out.println( "meanRelativeDifferenceOfAxes: " + Math.round( meanRelativeDifferenceOfAxes * 100d ) + "%" );

		double differenceX = Math.abs( inputCenter[ 0 ] - fittedEllipsoid.getCenter()[ 0 ] );
		double differenceY = Math.abs( inputCenter[ 1 ] - fittedEllipsoid.getCenter()[ 1 ] );
		double differenceZ = Math.abs( inputCenter[ 2 ] - fittedEllipsoid.getCenter()[ 2 ] );
		double meanCenterOffset = ( differenceX + differenceY + differenceZ ) / 3;
		System.out.println( "inputCenter: " + inputCenter[ 0 ] + ", " + inputCenter[ 1 ] + ", " + inputCenter[ 2 ] );
		System.out.println( "fittedEllipsoid.getCenter(): " + fittedEllipsoid.getCenter()[ 0 ] + ", " + fittedEllipsoid.getCenter()[ 1 ]
				+ ", " + fittedEllipsoid.getCenter()[ 2 ] );
		System.out.println( "meanCenterOffset: " + Math.round( meanCenterOffset ) );

		if ( relativeDifferenceShortAxis > maxPercentageDifference )
			return false;
		if ( relativeDifferenceMiddleAxis > maxPercentageDifference )
			return false;
		if ( relativeDifferenceLongAxis > maxPercentageDifference )
			return false;

		if ( Math.abs( inputCenter[ 0 ] - fittedEllipsoid.getCenter()[ 0 ] ) > maxCenterOffset )
			return false;
		if ( Math.abs( inputCenter[ 1 ] - fittedEllipsoid.getCenter()[ 1 ] ) > maxCenterOffset )
			return false;
		if ( Math.abs( inputCenter[ 2 ] - fittedEllipsoid.getCenter()[ 2 ] ) > maxCenterOffset )
			return false;
		System.out.println( "Successful fit according to the given acceptance criteria." );
		return true;
	}

	private static Triple< Double, Double, Double > getEigenvalues( double[][] matrix )
	{
		final JamaEigenvalueDecomposition eigenvalueDecomposition = new JamaEigenvalueDecomposition( 3 );
		eigenvalueDecomposition.decomposeSymmetric( matrix );
		double[] eigenvalues = eigenvalueDecomposition.getRealEigenvalues();
		// sort axes lengths in ascending order
		Arrays.sort( eigenvalues );
		return Triple.of( Math.sqrt( eigenvalues[ 0 ] ), Math.sqrt( eigenvalues[ 1 ] ), Math.sqrt( eigenvalues[ 2 ] ) );
	}
}
