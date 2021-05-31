import org.junit.Before;
import org.junit.Test;
import pt.up.fe.comp.MainAnalysis;
import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.JmmParserResult;
import pt.up.fe.comp.jmm.analysis.JmmAnalysis;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.specs.util.SpecsIo;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class AnalysisTest {
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

    private final List<String> semanticErrorFiles = Arrays.asList(
            "fixtures/public/fail/semantic/arr_index_not_int.jmm",
            "fixtures/public/fail/semantic/arr_size_not_int.jmm",
            "fixtures/public/fail/semantic/badArguments.jmm",
            "fixtures/public/fail/semantic/binop_incomp.jmm",
            "fixtures/public/fail/semantic/funcNotFound.jmm",
            "fixtures/public/fail/semantic/simple_length.jmm",
            "fixtures/public/fail/semantic/var_exp_incomp.jmm",
            "fixtures/public/fail/semantic/var_lit_incomp.jmm",
            "fixtures/public/fail/semantic/var_undef.jmm"
    );

    @Test
    public void unitTest() {
        System.out.println("Unit Test");

        String code = SpecsIo.getResource("fixtures/public/HelloWorld.jmm");

        // QuickSort.jmm
        JmmParserResult parserResult = TestUtils.parse(code);

        assertEquals("Program", parserResult.getRootNode().getKind());

        JmmSemanticsResult semanticsResult = TestUtils.analyse(parserResult);

        System.out.println(semanticsResult.getSymbolTable());
        System.out.println(semanticsResult.getReports());
    }

    @Test
    public void testAnalysis() {
        System.out.println("\nTesting Valid Files in test/public");
        for (String filename : this.validFiles) {
            System.out.print("Testing: " + filename);
            String code = SpecsIo.getResource(filename);
            JmmSemanticsResult result = TestUtils.analyse(code);
            TestUtils.noErrors(result.getReports());
            System.out.print("  - PASSED\n");
        }
    }

    @Test
    public void testSemanticErrors() {
        System.out.println("\nTesting Semantic Errors");
        for (String filename : this.semanticErrorFiles) {
            System.out.print("Testing: " + filename);
            String code = SpecsIo.getResource(filename);
            JmmSemanticsResult result = TestUtils.analyse(code);
            TestUtils.mustFail(result.getReports());
            System.out.print("  - PASSED\n");
        }
    }
}
