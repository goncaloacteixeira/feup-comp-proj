import org.junit.Before;
import org.junit.Test;
import pt.up.fe.comp.MainAnalysis;
import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.JmmParserResult;
import pt.up.fe.comp.jmm.analysis.JmmAnalysis;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class AnalysisTest {
    private List<String> validFiles;
    private List<String> syntacticalErrorFiles;
    private List<String> semanticErrorFiles;

    @Before
    public void setup() {
        validFiles = Utils.getValidFiles();
        syntacticalErrorFiles = Utils.getSyntacticalErrorFiles();
        semanticErrorFiles = Utils.getSemanticErrorFiles();
    }

    @Test
    public void unitTest() throws IOException {
        System.out.println("Unit Test");
        String code = Utils.getJmmCode("QuickSort.jmm");
        // QuickSort.jmm
        JmmParserResult parserResult = TestUtils.parse(code);

        assertEquals("Program", parserResult.getRootNode().getKind());

        JmmSemanticsResult semanticsResult = TestUtils.analyse(parserResult);

        System.out.println(semanticsResult.getSymbolTable());
        System.out.println(semanticsResult.getReports());
    }

    @Test
    public void testAnalysis() throws IOException {
        System.out.println("\nTesting Valid Files in test/public");
        for (String filename : this.validFiles) {
            System.out.print("Testing: " + filename);
            String code = Utils.getJmmCode(filename);
            System.setOut(new PrintStream(new Utils.NullOutputStream()));
            assertEquals("Program", TestUtils.analyse(code).getRootNode().getKind());
            System.setOut(Utils.realSystemOut);
            System.out.print("  - PASSED\n");
        }
    }

    @Test
    public void testSemanticErrors() throws IOException {
        System.out.println("\nTesting Semantic Errors");
        for (String filename : this.semanticErrorFiles) {
            System.out.print("Testing: " + filename);
            String code = Utils.getJmmCode(filename);
            System.setOut(new PrintStream(new Utils.NullOutputStream()));
            JmmSemanticsResult result = TestUtils.analyse(code);
            System.setOut(Utils.realSystemOut);
            TestUtils.mustFail(result.getReports());
            System.out.print("  - PASSED\n");
        }
    }
}
