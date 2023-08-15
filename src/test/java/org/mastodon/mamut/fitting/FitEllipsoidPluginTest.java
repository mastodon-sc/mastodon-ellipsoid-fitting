/*-
 * #%L
 * mastodon-ellipsoid-fitting
 * %%
 * Copyright (C) 2015 - 2023 Tobias Pietzsch, Jean-Yves Tinevez
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
package org.mastodon.mamut.fitting;

import net.imglib2.util.LinAlgHelpers;
import net.imglib2.util.StopWatch;

import org.junit.Test;
import org.mastodon.collection.RefObjectMap;
import org.mastodon.mamut.fitting.ellipsoid.Ellipsoid;
import org.mastodon.mamut.fitting.util.ArtificialData;
import org.mastodon.mamut.model.Spot;
import org.mastodon.mamut.plugin.MamutPluginAppModel;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

/**
 * Tests the {@link FitEllipsoidPlugin}.
 */
public class FitEllipsoidPluginTest
{

	private static final double ACCEPTED_RELATIVE_AXES_DIFFERENCE = 0.2;

	private static final int ACCEPTED_CENTER_DISTANCE = 2;

	@Test
	public void testFitEllipsoidPlugin() {
		ArtificialData data = new ArtificialData();
		StopWatch watch = StopWatch.createAndStart();
		FitEllipsoidPlugin plugin = new FitEllipsoidPlugin();
		plugin.setAppPluginModel( new MamutPluginAppModel( data.getAppModel(), null ) );
		plugin.fitSelectedVertices();
		System.out.println( watch );
		int success = countCorrectEllipsoids( data );
		assertEquals( "Not all ellipsoids were fitted correctly.", data.getAppModel().getModel().getGraph().vertices().size(), success );
	}

	private static int countCorrectEllipsoids( ArtificialData data )
	{
		int success = 0;
		RefObjectMap< Spot, Ellipsoid > expectedEllipsoids = data.getExpectedEllipsoids();
		for( Spot spot : data.getAppModel().getModel().getGraph().vertices() ) {
			Ellipsoid actualEllipsoid = asEllipsoid( spot );
			Ellipsoid expectedEllipsoid = expectedEllipsoids.get( spot );
			boolean equal = isEllipsoidEqual( expectedEllipsoid, actualEllipsoid );
			if( equal )
				success++;
		}
		return success;
	}

	private static Ellipsoid asEllipsoid( Spot spot )
	{
		double[][] covariance = new double[3][3];
		spot.getCovariance( covariance );
		double[] center = spot.positionAsDoubleArray();
		return new Ellipsoid( center, covariance, null, null, null );
	}

	private static boolean isEllipsoidEqual( Ellipsoid expectedEllipsoid, Ellipsoid fittedEllipsoid )
	{
		double centerDistance = LinAlgHelpers.distance( expectedEllipsoid.getCenter(), fittedEllipsoid.getCenter() );
		double[] expectedAxis = expectedEllipsoid.getRadii().clone();
		double[] fittedAxis = fittedEllipsoid.getRadii().clone();
		Arrays.sort( expectedAxis );
		Arrays.sort( fittedAxis );
		double v = relativeDifference( expectedAxis[ 0 ], fittedAxis[ 0 ] );
		double v1 = relativeDifference( expectedAxis[ 1 ], fittedAxis[ 1 ] );
		double v2 = relativeDifference( expectedAxis[ 2 ], fittedAxis[ 2 ] );
		boolean b = ( centerDistance <= ACCEPTED_CENTER_DISTANCE )
				&& ( v <= ACCEPTED_RELATIVE_AXES_DIFFERENCE )
				&& ( v1 <= ACCEPTED_RELATIVE_AXES_DIFFERENCE )
				&& ( v2 <= ACCEPTED_RELATIVE_AXES_DIFFERENCE );
		if ( b )
			return b;
		return b;
	}

	private static double relativeDifference( double expected, double actual )
	{
		return Math.abs( 1 - actual / expected );
	}
}
