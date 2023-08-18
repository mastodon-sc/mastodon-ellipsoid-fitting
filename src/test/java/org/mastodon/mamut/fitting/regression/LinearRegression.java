package org.mastodon.mamut.fitting.regression;

import org.ejml.factory.SingularMatrixException;
import org.ejml.simple.SimpleMatrix;

public class LinearRegression
{
    private SimpleMatrix parameters;

    public LinearRegression fit( SimpleMatrix x, SimpleMatrix y )
    {
        try
        {
            parameters = x.transpose().mult( x ).invert().mult( x.transpose() ).mult( y );
        }
        catch ( SingularMatrixException e )
        {
            System.out.println( "[WARN] not enough points to determine regression! Need at least " + x.numRows() );
        }
        return this;
    }

    public SimpleMatrix getParameters()
    {
        return parameters;
    }
}
