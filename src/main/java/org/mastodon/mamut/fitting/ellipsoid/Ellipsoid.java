/*-
 * #%L
 * mastodon-ellipsoid-fitting
 * %%
 * Copyright (C) 2015 - 2021 Tobias Pietzsch, Jean-Yves Tinevez
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

import net.imglib2.util.LinAlgHelpers;

public class Ellipsoid extends HyperEllipsoid
{
	/**
	 * Construct 3D ellipsoid. Some of the parameters may be null. The center
	 * parameter is always required. Moreover, either
	 * <ul>
	 * <li>covariance or</li>
	 * <li>precision or</li>
	 * <li>axes and radii</li>
	 * </ul>
	 * must be provided.
	 *
	 * @param center
	 *            coordinates of center. must not be {@code null}.
	 * @param covariance
	 * @param precision
	 * @param axes
	 * @param radii
	 */
	public Ellipsoid( final double[] center, final double[][] covariance, final double[][] precision, final double[][] axes, final double[] radii )
	{
		super( center, covariance, precision, axes, radii );
	}

	@Override
	public String toString()
	{
		return "center = " +
				LinAlgHelpers.toString( getCenter() )
				+ "\nradii = " +
				LinAlgHelpers.toString( getRadii() )
				+ "\naxes = " +
				LinAlgHelpers.toString( getAxes() )
				+ "\nprecision = " +
				LinAlgHelpers.toString( getPrecision() );
	}
}
