package ast;

import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.ast.PostorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.stream.Collectors;

/**
 * Counts the occurrences of each node kind.
 *
 * @author JBispo
 */
public class JmmSemanticPostorderVisitor extends PostorderJmmVisitor<Void, Void> {
    private final JmmSymbolTable table;
    private final List<Report> reports;

    public JmmSemanticPostorderVisitor(JmmSymbolTable table, List<Report> reports) {
        this.table = table;
        this.reports = reports;


        addVisit("BinaryOperation", this::dealWithBinaryOperation);
        addVisit("ArrayAccess", this::dealArrayAccess);
        addVisit("Program", this::dealProgram);
    }

    private Void dealArrayAccess(JmmNode node, Void space) {
        System.out.println(node);
        return null;
    }

    private Void dealProgram(JmmNode node, Void space) {
        System.out.println(node);
        return null;
    }

    private Void dealWithBinaryOperation(JmmNode node, Void space) {
        JmmNode left = node.getChildren().get(0);
        JmmNode right = node.getChildren().get(1);

        if (left.getKind().equals("BinaryOperation") /* access expression, method invocation */) {
            if (left.get("operation_result").equals("error")) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(left.get("line")), Integer.parseInt(left.get("col")), "Left Member is not Integer: " + left));
                node.put("operation_result", "error");
            }
        } else {
            if (!left.getKind().equals("IntegerLiteral")) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(left.get("line")), Integer.parseInt(left.get("col")), "Left Member is not Integer: " + left));
                node.put("operation_result", "error");
            }
        }

        if (right.getKind().equals("BinaryOperation") /* access expression, method invocation */) {
            if (right.get("operation_result").equals("error")) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(right.get("line")), Integer.parseInt(right.get("col")), "Right Member is not Integer: " + right));
                node.put("operation_result", "error");
            }
        } else {
            if (!right.getKind().equals("IntegerLiteral")) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(right.get("line")), Integer.parseInt(right.get("col")), "Right Member is not Integer: " + right));
                node.put("operation_result", "error");
            } else {
                if (node.get("operation").equals("/") && right.get("value").equals("0")) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(right.get("line")), Integer.parseInt(right.get("col")), "Division by 0: " + right));
                    node.put("operation_result", "error");
                }
            }
        }

        if (!node.getAttributes().contains("operation_result")) {
            node.put("operation_result", "ok");
        }

        return null;
    }
}
