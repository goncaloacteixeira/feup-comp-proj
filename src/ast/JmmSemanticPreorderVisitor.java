package ast;

import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.utilities.StringLines;

import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

public class JmmSemanticPreorderVisitor extends PreorderJmmVisitor<String, Boolean> {
    private final JmmSymbolTable table;
    private final Stack<String> scope;
    private final List<Report> reports;

    public JmmSemanticPreorderVisitor(JmmSymbolTable table, List<Report> reports) {
        this.table = table;
        this.reports = reports;
        this.scope = new Stack<>();

        addVisit("BinaryOperation", this::dealWithBinaryOperation);
    }

    private boolean dealWithBinaryOperation(JmmNode node, String space) {
        System.out.println("OP: " + node);

        switch (node.get("operation")) {
            case "+":
                verifyInt(node);
                break;
            default:
                break;
        }

        return true;
    }

    private void verifyInt(JmmNode node) {
        List<JmmNode> children = node.getChildren();

        System.out.println("LEFT: " + children.get(0));
        System.out.println("RIGHT: " + children.get(1));


    }
}
