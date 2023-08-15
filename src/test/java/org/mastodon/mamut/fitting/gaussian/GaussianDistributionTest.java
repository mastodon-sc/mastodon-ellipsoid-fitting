package org.mastodon.mamut.fitting.gaussian;

import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.type.numeric.real.FloatType;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mastodon.mamut.fitting.MultiVariantNormalDistributionRenderer;

import static org.junit.Assert.assertArrayEquals;

public class GaussianDistributionTest
{

	private static final int PIXEL_VALUE = 42;

	private static Img< FloatType > IMAGE;

	private static final double[] EXPECTED_CENTER = { 40, 50, 60 };

	private static final double[][] EXPECTED_COVARIANCE = {
			{ 400, 20, -10 },
			{ 20, 200, 30 },
			{ -10, 30, 100 }
	};

	/**
	 * Generates an example image with a single ellipsoid. Pixel values are 0 or 42.
	 * 0 is background, 42 is the ellipsoid.
	 */
	@BeforeClass
	public static void init()
	{
		IMAGE = ArrayImgs.floats( 100, 100, 100 );
		MultiVariantNormalDistributionRenderer.renderMultivariateNormalDistribution( EXPECTED_CENTER, EXPECTED_COVARIANCE, IMAGE );
		simulateSegmentation();
	}

	private static void simulateSegmentation()
	{
		LoopBuilder.setImages( IMAGE ).forEachPixel( pixel -> {
			if ( pixel.get() > 500 )
				pixel.set( PIXEL_VALUE );
			else
				pixel.set( 0 );
		} );
	}

	@Test
	public void testComputeMean()
	{
		double[] computedMean = GaussianDistribution.computeMean( IMAGE, pixelValue -> pixelValue.get() == PIXEL_VALUE );
		assertArrayEquals( EXPECTED_CENTER, computedMean, 0d );
	}

	@Test
	public void testComputeCovariance()
	{
		double[] computeMean = GaussianDistribution.computeMean( IMAGE, pixelValue -> pixelValue.get() == PIXEL_VALUE );
		double[][] computedCovariance =
				GaussianDistribution.computeCovariance( IMAGE, computeMean, pixelValue -> pixelValue.get() == PIXEL_VALUE );
		assertArrayEquals( EXPECTED_COVARIANCE[ 0 ], computedCovariance[ 0 ], 0d );
		assertArrayEquals( EXPECTED_COVARIANCE[ 1 ], computedCovariance[ 1 ], 0d );
		assertArrayEquals( EXPECTED_COVARIANCE[ 2 ], computedCovariance[ 2 ], 0d );
	}
}
