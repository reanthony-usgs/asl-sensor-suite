package asl.sensor.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.util.Pair;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.Before;
import org.junit.Test;
import asl.sensor.input.InstrumentResponse;
import asl.sensor.input.TransferFunction;
import asl.sensor.input.Unit;
import asl.sensor.utils.ReportingUtils;

public class InstrumentResponseTest {

  public static String folder = TestUtils.DL_DEST_LOCATION + TestUtils.SUBPAGE;
  public static final DateTimeFormatter DATE_TIME_FORMAT = InstrumentResponse.RESP_DT_FORMAT;

  @Before
  public void getReferencedData() {

    // place in sprockets folder under 'from-sensor-test/[test-name]'
    String refSubfolder = TestUtils.SUBPAGE + "resp-parse/";
    String[] filenames = new String[]{
        "RESP.XX.NS087..BHZ.STS1.20.2400",
        "multiepoch.txt"
    };
    for (String fileID : filenames) {
      try {
        TestUtils.downloadTestData(refSubfolder, fileID, refSubfolder, fileID);
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
  }

  @Test
  public void testFileParse() {
    String filename = folder + "resp-parse/RESP.XX.NS087..BHZ.STS1.20.2400";

    try {
      InstrumentResponse ir = new InstrumentResponse(filename);

      assertEquals(TransferFunction.LAPLACIAN, ir.getTransferFunction() );

      double nml = Double.parseDouble("3.948580E+03");
      assertEquals( nml, ir.getNormalization(), 0.0001 );

      double nmf = Double.parseDouble("3.000000E-01");
      assertEquals( nmf, ir.getNormalizationFrequency(), 0.0001 );

      double[] gn = {2.400000e+03, 2.400000e+03, 1.000000e+00};
      int maxStage = ir.getNumStages();
      assertEquals(maxStage, gn.length);
      for (int i = 0; i < maxStage; ++i) {
        assertEquals(gn[i], ir.getGain()[i], 1.);
      }
      //assertTrue( gnL.equals(ir.getGain() ) );

      assertEquals( Unit.VELOCITY, ir.getUnits() );

      List<Complex> zrs = new ArrayList<Complex>();
      zrs.add( new Complex(0.000000e+00, 0.000000e+00) );
      zrs.add( new Complex(0.000000e+00, 0.000000e+00) );
      assertEquals( zrs, ir.getZeros() );

      List<Complex> pls = new ArrayList<Complex>();
      pls.add( new Complex(-2.221000e-01,  2.221000e-01) );
      pls.add( new Complex(-2.221000e-01, -2.221000e-01) );
      pls.add( new Complex(-3.918000e+01,  4.912000e+01) );
      pls.add( new Complex(-3.918000e+01, -4.912000e+01) );
      assertEquals( pls, ir.getPoles() );

    } catch (IOException e) {
      fail("Unexpected error trying to read response file");
    }

  }

  @Test
  public void testMultiEpoch() {

    String filename = folder + "resp-parse/multiepoch.txt";
    try {
      List<Pair<Instant, Instant>> eps = InstrumentResponse.getRespFileEpochs(filename);
      assertTrue( eps.size() > 1 );
    } catch (IOException e) {
      fail();
      e.printStackTrace();
    }

  }

  @Test
  public void listsAllEpochs() {

    // epoch 1 ends same time epoch 2 begins
    String[] times = {"2016,193,00:00:00", "2016,196,00:00:00", "2016,224,00:00:00"};
    Instant[] insts = new Instant[times.length];
    for (int i = 0; i < times.length; ++i) {
      insts[i] = LocalDateTime.parse(times[i], DATE_TIME_FORMAT).toInstant(ZoneOffset.UTC);
    }
    List<Pair<Instant, Instant>> compareTo = new ArrayList<Pair<Instant, Instant>>();
    compareTo.add(new Pair<Instant, Instant>(insts[0], insts[1]));
    compareTo.add(new Pair<Instant, Instant>(insts[1], insts[2]));

    String filename = folder + "resp-parse/multiepoch.txt";
    try{
      List<Pair<Instant, Instant>> eps = InstrumentResponse.getRespFileEpochs(filename);
      for (int i = 0; i < eps.size(); ++i) {
        Pair<Instant, Instant> inst = eps.get(i);
        Pair<Instant, Instant> base = compareTo.get(i);
        assertEquals(inst.getFirst(), base.getFirst());
        assertEquals(inst.getSecond(), base.getSecond());
      }
    } catch (IOException e) {
      fail();
      e.printStackTrace();
    }
  }

  @Test
  public void testStringOutput() {

    String filename = folder + "resp-parse/RESP.XX.NS087..BHZ.STS1.20.2400";

    try {
      InstrumentResponse ir = new InstrumentResponse(filename);

      System.out.println(ir);

      PDDocument pdf = new PDDocument();

      ReportingUtils.textToPDFPage( ir.toString(), pdf );

      String currentDir = System.getProperty("user.dir");
      String testResultFolder = currentDir + "/testResultImages/";
      File dir = new File(testResultFolder);
      if ( !dir.exists() ) {
        dir.mkdir();
      }

      String testResult = testResultFolder + "response-report.pdf";
      pdf.save( new File(testResult) );
      pdf.close();

    } catch (IOException e) {
      fail("Unexpected error trying to read response file");
    }
  }

  @Test
  public void vectorCreationRespectsDuplicatePoles() {

    InstrumentResponse ir;
    try {
      ir =
          InstrumentResponse.loadEmbeddedResponse("STS-5A_Q330HR_BH_40");
    } catch (IOException e) {
      fail();
      e.printStackTrace();
      return;
    }

    List<Complex> initPoles = new ArrayList<Complex>( ir.getPoles() );
    RealVector rv = ir.polesToVector(false, 100.);
    Complex c = new Complex(-20, 0);
    // poles at indices 2 and 3 are duplicated, have zero imaginary component
    // set them to a new value to test array resetting with diff. values
    initPoles.set(2, c);
    initPoles.set(3, c);
    // build new vector, is it the same?
    List<Complex> endPoles =
        ir.buildResponseFromFitVector( rv.toArray(), false, 0 ).getPoles();

    assertTrue( initPoles.size() == endPoles.size() );

  }

}
