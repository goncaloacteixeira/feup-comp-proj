import org.junit.Before;
import org.junit.Test;
import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.JmmParserResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.stringparser.ParserResult;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ParserTest {
    private List<String> validFiles;
    private List<String> syntacticalErrorFiles;

    @Before
    public void setup() {
        validFiles = Utils.getValidFiles();
        syntacticalErrorFiles = Utils.getSyntacticalErrorFiles();
    }

    @Test
    public void unitTest() throws IOException {
        System.out.println("Unit Test");
        String code = Utils.getJmmCode("MonteCarloPi.jmm");

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
        System.setOut(new PrintStream(new Utils.NullOutputStream()));
        for (String filename : validFiles) {
            String code = Utils.getJmmCode(filename);
            String astJson = TestUtils.parse(code).getRootNode().toJson();
            File jmm = new File(filename);
            int i = jmm.getName().lastIndexOf('.');
            String name = jmm.getName().substring(0,i);
            File json = new File(name + ".json");

            FileOutputStream fos = new FileOutputStream(json);
            fos.write(astJson.getBytes(StandardCharsets.UTF_8));
            fos.close();
        }
        System.setOut(Utils.realSystemOut);
    }

    @Test
    public void testParser() throws IOException {
        System.out.println("\nTesting Valid Files");
        for (String filename : this.validFiles) {
            System.out.print("Testing: " + filename);
            String code = Utils.getJmmCode(filename);
            System.setOut(new PrintStream(new Utils.NullOutputStream()));
            assertEquals("Program", TestUtils.parse(code).getRootNode().getKind());
            System.setOut(Utils.realSystemOut);
            System.out.print("  - PASSED\n");
        }
	}

	@Test
    public void testSyntacticalErrors() throws IOException {
        System.out.println("\nTesting Syntactical Errors");
        for (String filename : this.syntacticalErrorFiles) {
            System.out.print("Testing: " + filename);
            String code = Utils.getJmmCode(filename);
            System.setOut(new PrintStream(new Utils.NullOutputStream()));
            JmmParserResult result = TestUtils.parse(code);
            System.setOut(Utils.realSystemOut);
            TestUtils.mustFail(result.getReports());
            System.out.print("  - PASSED\n");
        }
    }
}
