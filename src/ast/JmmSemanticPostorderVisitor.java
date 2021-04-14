package ast;

import ast.exceptions.DivisionByZero;
import ast.exceptions.UnsupportedOperation;
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
        addVisit("IntegerLiteral", this::dealWithPrimitive);
        addVisit("BooleanLiteral", this::dealWithPrimitive);
        addVisit("Variable", this::dealWithVariable);
    }

    private Void dealArrayAccess(JmmNode node, Void space) {
        JmmNode index = node.getChildren().get(0);

        if ((index.getKind().equals("BinaryOperation") && index.get("operation_result").equals("error")) || !index.getKind().equals("IntegerLiteral")) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(index.get("line")), Integer.parseInt(index.get("col")), "Array access index is not an integer: " + index));
        }

        return null;
    }

    private Void dealArrayInit(JmmNode node, Void space) {
        JmmNode size = node.getChildren().get(0);

        if ((size.getKind().equals("BinaryOperation") && size.get("operation_result").equals("error")) || !size.getKind().equals("IntegerLiteral")) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(size.get("line")), Integer.parseInt(size.get("col")), "Array init size is not an integer: " + size));
        }

        return null;
    }

    private Void dealWithBinaryOperation(JmmNode node, Void space) {
        JmmNode left = node.getChildren().get(0);
        JmmNode right = node.getChildren().get(1);

        if (!left.get("return_type").equals("int")) {
            node.put("return_type", "error");
            // para não apresentar o mesmo erro mais do que uma vez
            if (!(left.getAttributes().contains("return_type") && left.get("return_type").equals("error")))
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(left.get("line")), Integer.parseInt(left.get("col")), "Left Member not integer"));
        }
        if (!right.get("return_type").equals("int")) {
            node.put("return_type", "error");
            // para não apresentar o mesmo erro mais do que uma vez
            if (!(right.getAttributes().contains("return_type") && right.get("return_type").equals("error")))
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(right.get("line")), Integer.parseInt(right.get("col")), "Right Member not integer"));
        }

        // TODO - fazer verificação se for divisão p/ 0

        if (!node.getAttributes().contains("return_type")) {
            try {
                String result = expressionEval(left.get("result"), right.get("result"), node.get("operation"));
                node.put("return_type", "int");
                node.put("result", result);
            } catch (DivisionByZero divisionByZero) {
                node.put("return_type", "error");
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Division by Zero"));
            } catch (UnsupportedOperation unsupportedOperation) {
                node.put("return_type", "error");
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Operation not supported"));
            }
        }

        System.out.println(node);
        return null;
    }


    private String expressionEval(String a, String b, String operation) throws DivisionByZero, UnsupportedOperation {
        switch (operation) {
            case "*":
                return String.valueOf(Integer.parseInt(a) * Integer.parseInt(b));
            case "/":
                if (Integer.parseInt(b) == 0) {
                    throw new DivisionByZero();
                }
                return String.valueOf(Integer.parseInt(a) / Integer.parseInt(b));
            case "+":
                return String.valueOf(Integer.parseInt(a) + Integer.parseInt(b));
            case "-":
                return String.valueOf(Integer.parseInt(a) - Integer.parseInt(b));
            default:
                throw new UnsupportedOperation();
        }
    }


    private Void dealWithPrimitive(JmmNode node, Void space) {
        String return_type;

        switch (node.getKind()) {
            case "IntegerLiteral":
                return_type = "int";
                node.put("result", node.get("value"));
                break;
            case "BooleanLiteral":
                return_type = "boolean";
                break;
            default:
                return_type = "error";
                break;
        }

        node.put("return_type", return_type);

        return null;
    }

    private Void dealWithVariable(JmmNode node, Void space) {
        String return_type;

        node.put("return_type", "int");
        node.put("result", "1");

        return null;

    }
}
