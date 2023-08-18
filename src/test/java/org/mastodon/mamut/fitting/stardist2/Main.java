package org.mastodon.mamut.fitting.stardist2;

import io.bioimage.modelrunner.engine.EngineInfo;
import io.bioimage.modelrunner.model.Model;
import io.bioimage.modelrunner.tensor.Tensor;
import io.scif.img.ImgOpener;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Cast;
import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;
import net.imglib2.view.Views;
import org.ejml.simple.SimpleMatrix;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main
{
    public static void main( String[] args ) throws Exception
    {
        String framework = "tensorflow_saved_model_bundle";
        String version = "1.15.0";
        String dir = "dl-engines";

//        EngineManagement.installEngineWithArgsInDir(framework, version, true, true, dir);

        EngineInfo einfo = EngineInfo.defineDLEngine( framework, version, dir );
        String modelFolder = "dl-engines/stardist-model";
        Model model = Model.createDeepLearningModel( modelFolder, modelFolder, einfo );

        // List that will contain the input tensors
        List< Tensor< ? > > inputTensors = new ArrayList<>();
        // List that will contain the output tensors
        List< Tensor< ? > > outputTensors = new ArrayList<>();

        RandomAccessibleInterval< ? extends RealType< ? > > img = loadImg( "dl-engines/stardist-data/stardist_single.xml" );
        long[] dimensions = new long[ 5 ];
        dimensions[ 3 ] = 1; // data dimension (probability vs distance)
        dimensions[ 4 ] = 1; // ray dimension
        for ( int i = 0; i < 3; i++ )
            dimensions[ i ] = img.dimension( i );
        //noinspection deprecation
        Img< FloatType > data = new ArrayImgFactory< FloatType >().create( dimensions, new FloatType() );

        Cursor< FloatType > cursor = data.localizingCursor();
        RandomAccess< ? extends RealType< ? extends RealType< ? > > > source = img.randomAccess();
        while ( cursor.hasNext() )
        {
            cursor.fwd();
            cursor.get().set( source.setPositionAndGet( cursor ).getRealFloat() );
        }

        String axes = "xyzbc";
        inputTensors.add( Tensor.build( "input", axes, data ) );

        outputTensors.add( Tensor.buildEmptyTensor( "output", axes ) );

        model.loadModel();
        model.runModel( inputTensors, outputTensors );
        model.closeModel();

        Pair< RandomAccessibleInterval< FloatType >, RandomAccessibleInterval< FloatType > > result =
                splitPrediction( Cast.unchecked( outputTensors.get( 0 ) ) );
        Candidates cand = new Candidates( result.getA(), result.getB() );
        ArrayList< SimpleMatrix > ellipsoids = new ArrayList< SimpleMatrix >(); // cand.getPolygons().size x 9

        for ( List< SimpleMatrix > surface : cand.getPolygons() )
        {
            SimpleMatrix lrX = new SimpleMatrix( surface.size(), 9 ); // 9 parameters are required to describe an ellipsoid.
            SimpleMatrix lrY = new SimpleMatrix( surface.size(), 1 );
            lrY.set( 1 ); // normalization for the ellipsoid eq.

            for ( int row = 0; row < surface.size(); row++ )
            {
                SimpleMatrix point = surface.get( row );

                double x = point.get( 0 );
                double y = point.get( 1 );
                double z = point.get( 2 );

                lrX.set( row, 0, x * x );
                lrX.set( row, 1, y * y );
                lrX.set( row, 2, z * z );
                lrX.set( row, 3, x * y );
                lrX.set( row, 4, x * z );
                lrX.set( row, 5, y * z );
                lrX.set( row, 6, x );
                lrX.set( row, 7, y );
                lrX.set( row, 8, z );

            }
            // ax^2 + by^2 + cz^2 + dxy + exz - fyz + gx + hy - iz - 1 == 0
            SimpleMatrix ellipsoid = new LinearRegression().fit( lrX, lrY ).getParameters().transpose();
            ellipsoids.add( ellipsoid );
        }

        ellipsoids.forEach( System.out::println );
    }

    @SuppressWarnings("SameParameterValue")
    private static RandomAccessibleInterval< ? extends RealType< ? > > loadImg( String location )
    {
        if ( location.endsWith( ".xml" ) )
        {
            File xmlFile = new File( location );
            try
            {
                Element elem = new SAXBuilder().build( xmlFile ).getRootElement();
                SpimData spimData = new XmlIoSpimData().fromXml( elem, xmlFile );
                return Cast.unchecked( spimData.getSequenceDescription().getImgLoader().getSetupImgLoader( 0 )
                        .getImage( 0 ) );
            }
            catch ( IOException | JDOMException | SpimDataException e )
            {
                throw new RuntimeException( e );
            }
        }
        else
        {
            return new ImgOpener().openImgs( location, new UnsignedShortType() ).get( 0 );
        }
    }

    private static Pair< RandomAccessibleInterval< FloatType >, RandomAccessibleInterval< FloatType > > splitPrediction(
            Tensor< FloatType > prediction
    )
    {
        RandomAccessibleInterval< FloatType > rai = prediction.getData();
        long[] shape = Arrays.stream( prediction.getShape() ).asLongStream().toArray();
        shape[ 4 ]--;

        final RandomAccessibleInterval< FloatType > probRAI = Views.hyperSlice( rai, 3, 0 );
        final RandomAccessibleInterval< FloatType > distRAI = Views.offsetInterval( rai, new long[] { 0, 0, 0, 1, 0 }, shape );

        return new ValuePair<>( probRAI, distRAI );
    }
}
