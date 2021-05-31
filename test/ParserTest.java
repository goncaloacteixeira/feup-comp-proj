import org.junit.Before;
import org.junit.Test;
import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.JmmParserResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.SpecsIo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ParserTest {
    private final List<String> validFiles = Arrays.asList(
            "fixtures/public/FindMaximum.jmm",
            "fixtures/public/HelloWorld.jmm",
            "fixtures/public/Lazysort.jmm",
            "fixtures/public/Life.jmm",
            "fixtures/public/MonteCarloPi.jmm",
            "fixtures/public/QuickSort.jmm",
            "fixtures/public/Simple.jmm",
            "fixtures/public/TicTacToe.jmm",
            "fixtures/public/WhileAndIF.jmm"
    );

    private final List<String> syntacticalErrorFiles = Arrays.asList(
            "fixtures/public/fail/syntactical/BlowUp.jmm",
            "fixtures/public/fail/syntactical/CompleteWhileTest.jmm",
            "fixtures/public/fail/syntactical/LengthError.jmm",
            "fixtures/public/fail/syntactical/MissingRightPar.jmm",
            "fixtures/public/fail/syntactical/MultipleSequential.jmm",
            "fixtures/public/fail/syntactical/NestedLoop.jmm"
    );


    @Test
    public void unitTest() {
        System.out.println("Unit Test");
        String code = SpecsIo.getResource("fixtures/public/HelloWorld.jmm");

        JmmParserResult parserResult = TestUtils.parse(code);

        assertEquals("Program", parserResult.getRootNode().getKind());
        System.out.println("\nREPORTS");
        for (Report report : parserResult.getReports()) {
            System.out.println(report);
        }
    }

    /**
     * Call this method to generate the JSON or add @Test
     * @throws IOException  error while writing
     */
    public void generateJSON() throws IOException {
        System.out.println("Unit Test");
        for (String filename : validFiles) {
            String code = SpecsIo.getResource(filename);
            String astJson = TestUtils.parse(code).getRootNode().toJson();
            File jmm = new File(filename);
            int i = jmm.getName().lastIndexOf('.');
            String name = jmm.getName().substring(0,i);
            File json = new File(name + ".json");

            FileOutputStream fos = new FileOutputStream(json);
            fos.write(astJson.getBytes(StandardCharsets.UTF_8));
            fos.close();
        }
    }

    @Test
    public void testParser() {
        System.out.println("\nTesting Valid Files");
        for (String filename : this.validFiles) {
            System.out.print("Testing: " + filename);
            String code = SpecsIo.getResource(filename);
            assertEquals("Program", TestUtils.parse(code).getRootNode().getKind());
            System.out.print("  - PASSED\n");
        }
	}

	@Test
    public void testSyntacticalErrors() {
        System.out.println("\nTesting Syntactical Errors");
        for (String filename : this.syntacticalErrorFiles) {
            System.out.print("Testing: " + filename);
            String code = SpecsIo.getResource(filename);
            JmmParserResult result = TestUtils.parse(code);
            TestUtils.mustFail(result.getReports());
            System.out.print("  - PASSED\n");
        }
    }
}
