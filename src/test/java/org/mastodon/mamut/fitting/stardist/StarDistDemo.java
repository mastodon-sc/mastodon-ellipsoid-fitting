package org.mastodon.mamut.fitting.stardist;

import io.bioimage.modelrunner.engine.EngineInfo;
import io.bioimage.modelrunner.engine.installation.EngineManagement;
import io.bioimage.modelrunner.model.Model;
import io.bioimage.modelrunner.tensor.Tensor;
import io.bioimage.modelrunner.versionmanagement.DeepLearningVersion;
import io.bioimage.modelrunner.versionmanagement.InstalledEngines;
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
import net.imglib2.view.Views;
import org.ejml.simple.SimpleMatrix;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.mastodon.mamut.fitting.regression.LinearRegression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Requires:
 * <ul>
 *     <li>The <a href="https://www.tensorflow.org/install/source#gpu">requirements</a> for the latest supported tensorflow1 engine (=tensorflow_gpu version 1.15.0), i.e.</li>
 *     <ul>
 *          <li>Installed <a href="https://developer.nvidia.com/cuda-toolkit-archive">cuda toolkit (10.0)</a></li>
 *          <li>Installed <a href="https://developer.nvidia.com/rdp/cudnn-archive">cuDNN (7.4)</a></li>
 *     </ul>
 * </ul>
 */
public class StarDistDemo
{
	private static final String ENGINE = "tensorflow_saved_model_bundle";

	private static final String VERSION = "1.15.0";

	private static final String ENGINE_DIRECTORY = "dl-engines";

	private static final String MODEL_DIRECTORY = ENGINE_DIRECTORY + File.separator + "stardist-model";

	private static final String DATA_DIRECTORY = ENGINE_DIRECTORY + File.separator + "stardist-data";

	private static final boolean CPU = true;

	private static final boolean GPU = false;

	private static final Logger logger = LoggerFactory.getLogger( StarDistDemo.class );

	public static void main( String[] args ) throws Exception
	{
		EngineInfo engineInfo = setupEngine();
		Model model = Model.createDeepLearningModel( MODEL_DIRECTORY, MODEL_DIRECTORY, engineInfo );
		// AvailableEngines.getForCurrentOS().getVersions().forEach( System.out::println );
		RandomAccessibleInterval< ? extends RealType< ? > > image = loadImage( DATA_DIRECTORY + File.separator + "stardist_single.xml" );

		List< Tensor< ? > > prediction = processImage( image, model );

		RandomAccessibleInterval< FloatType > distances = getDistances( Cast.unchecked( prediction.get( 0 ) ) );
		RandomAccessibleInterval< FloatType > probabilities = getProbabilities( Cast.unchecked( prediction.get( 0 ) ) );
		StarDist starDist = new StarDist( distances, probabilities );
		/*
		for ( List< SimpleMatrix > starConvexShape : starDist.getStarConvexShapes() )
		{
			System.out.println( "-- Star Convex Shape: --" );
			starConvexShape.forEach( simpleMatrix -> System.out.println( "Point: " + simpleMatrix.toString() ) );
		}

		 */

		// computeEllipsoids( candidates );
	}

	private static List< Tensor< ? > > processImage( RandomAccessibleInterval< ? extends RealType< ? > > image, Model model )
			throws Exception
	{
		String axes = "xyzbc";
		List< Tensor< ? > > inputTensors = getInputTensors( image, axes );
		List< Tensor< ? > > outputTensors = Collections.singletonList( Tensor.buildEmptyTensor( "output", axes ) );

		model.loadModel();
		model.runModel( inputTensors, outputTensors );
		model.closeModel();

		return outputTensors;
	}

	private static EngineInfo setupEngine()
	{
		List< DeepLearningVersion > installedEngines =
				InstalledEngines.checkEngineWithArgsInstalledForOS( ENGINE, VERSION, CPU, GPU, ENGINE_DIRECTORY );
		if ( installedEngines.isEmpty() )
			installEngine();

		EngineInfo engineInfo = EngineInfo.defineDLEngine( ENGINE, VERSION, ENGINE_DIRECTORY );
		if ( engineInfo == null || !engineInfo.isEngineInstalled() )
			throw new RuntimeException( "Engine not properly installed: " + ENGINE + ", version: '"
					+ VERSION + "', directory: '" + ENGINE_DIRECTORY + "'" );
		return engineInfo;
	}

	private static List< Tensor< ? > > getInputTensors( final RandomAccessibleInterval< ? extends RealType< ? > > image, final String axes )
	{
		// set dimensions and 2 to the previous 3 (5 in total)
		// data dimension (probability vs distance)
		// ray dimension
		Img< FloatType > data = setDimensions( image, 5 );
		List< Tensor< ? > > inputTensors = new ArrayList<>();
		inputTensors.add( Tensor.build( "input", axes, data ) );
		return inputTensors;
	}

	private static Img< FloatType > setDimensions( RandomAccessibleInterval< ? extends RealType< ? > > image, int numDimensions )
	{
		long[] newDimensions = new long[ numDimensions ];
		for ( int i = 0; i < image.numDimensions(); i++ )
			newDimensions[ i ] = image.dimension( i );
		for ( int i = image.numDimensions(); i < numDimensions; i++ )
			newDimensions[ i ] = 1;

		// TODO use non-deprecated factory
		// noinspection deprecation
		Img< FloatType > data = new ArrayImgFactory< FloatType >().create( newDimensions, new FloatType() );

		// copy image data into data after the dimensions have been added
		Cursor< FloatType > cursor = data.localizingCursor();
		RandomAccess< ? extends RealType< ? extends RealType< ? > > > source = image.randomAccess();
		while ( cursor.hasNext() )
		{
			cursor.fwd();
			cursor.get().set( source.setPositionAndGet( cursor ).getRealFloat() );
		}
		return data;
	}

	private static void installEngine()
	{
		try
		{
			EngineManagement.installEngineWithArgsInDir( ENGINE, VERSION, CPU, GPU, ENGINE_DIRECTORY );
		}
		catch ( IOException | InterruptedException e )
		{
			logger.error(
					"Could not install engine (name: '{}' version: '{}' into: {}). Message: {}", ENGINE,
					VERSION, new File( ENGINE_DIRECTORY ).getAbsolutePath(), e.getMessage()
			);
		}

	}

	@SuppressWarnings("SameParameterValue")
	private static RandomAccessibleInterval< ? extends RealType< ? > > loadImage( String location )
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

	private static RandomAccessibleInterval< FloatType > getProbabilities( Tensor< FloatType > prediction )
	{
		return Views.hyperSlice( prediction.getData(), 3, 0 );
	}

	private static RandomAccessibleInterval< FloatType > getDistances( Tensor< FloatType > prediction )
	{
		long[] tensorShape = Arrays.stream( prediction.getShape() ).asLongStream().toArray();
		// remove the ray dimension
		tensorShape[ 4 ]--;
		return Views.offsetInterval( prediction.getData(), new long[] { 0, 0, 0, 1, 0 }, tensorShape );
	}

	private static void computeEllipsoids( StarDist starDist )
	{
		List< SimpleMatrix > ellipsoids = new ArrayList<>();
		for ( List< SimpleMatrix > surface : starDist.getStarConvexShapes() )
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

		ellipsoids.forEach( simpleMatrix -> System.out.println( "ellipsoid: " + simpleMatrix ) );
	}
}
