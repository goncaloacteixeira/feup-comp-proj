import ast.JmmExpressionAnalyser;
import ast.JmmSemanticPreorderVisitor;
import ast.JmmSymbolTable;
import ast.SymbolTableVisitor;
import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.JmmParserResult;
import pt.up.fe.comp.jmm.analysis.JmmAnalysis;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AnalysisStage implements JmmAnalysis {

    /**
     * Executes the Semantic Analysis on a JMMParserResult
     *
     * @param parserResult a JmmParserResult to be analysed
     * @return The Result of the Analysis as a JmmSemanticsResult
     */
    @Override
    public JmmSemanticsResult semanticAnalysis(JmmParserResult parserResult) {

        if (TestUtils.getNumReports(parserResult.getReports(), ReportType.ERROR) > 0) {
            var errorReport = new Report(ReportType.ERROR, Stage.SEMANTIC, -1,
                    "Started semantic analysis but there are errors from previous stage");
            return new JmmSemanticsResult(parserResult, null, Arrays.asList(errorReport));
        }

        if (parserResult.getRootNode() == null) {
            var errorReport = new Report(ReportType.ERROR, Stage.SEMANTIC, -1,
                    "Started semantic analysis but AST root node is null");
            return new JmmSemanticsResult(parserResult, null, Arrays.asList(errorReport));
        }

        JmmNode node = parserResult.getRootNode();

        JmmSymbolTable table = new JmmSymbolTable();
        List<Report> reports = new ArrayList<>();

        System.out.println("Preorder Visitor - Filling Symbol Table...");
        SymbolTableVisitor visitor = new SymbolTableVisitor(table, reports);
        visitor.visit(node, "");
        System.out.println("Symbol Table Filled!");

        System.out.println("Postorder Visitor - Semantic Analysis...");
        JmmExpressionAnalyser expressionsAnalyser = new JmmExpressionAnalyser(table, reports);
        expressionsAnalyser.visit(node, null);
        System.out.println("Semantic Analysis Done!");

        return new JmmSemanticsResult(parserResult, table, reports);
    }

}
