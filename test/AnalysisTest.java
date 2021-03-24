import org.junit.Before;
import org.junit.Test;
import pt.up.fe.comp.MainAnalysis;
import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.JmmParserResult;
import pt.up.fe.comp.jmm.analysis.JmmAnalysis;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;

import java.io.IOException;
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
        String code = Utils.getJmmCode("MonteCarloPi.jmm");

        JmmParserResult parserResult = TestUtils.parse(code);

        assertEquals("Program", parserResult.getRootNode().getKind());

        JmmSemanticsResult semanticsResult = new Main().analyse(parserResult);

        System.out.println(semanticsResult.getSymbolTable());
    }
}
