package ast;

import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.specs.util.utilities.StringLines;

import java.util.*;

public class OllirVisitor extends PreorderJmmVisitor<String, String> {
    private final JmmSymbolTable table;
    private JmmMethod currentMethod;
    private final List<Report> reports;
    private String scope;
    private Set<JmmNode> visited = new HashSet<>();

    public OllirVisitor(JmmSymbolTable table, List<Report> reports) {
        super(OllirVisitor::reduce);

        this.table = table;
        this.reports = reports;

        addVisit("ClassDeclaration", this::dealWithClass);
        addVisit("MainMethod", this::dealWithMainDeclaration);
        addVisit("ClassMethod", this::dealWithMethodDeclaration);
        addVisit("Assignment", this::dealWithAssignment);
        addVisit("IntegerLiteral", this::dealWithPrimitive);
        addVisit("BooleanLiteral", this::dealWithPrimitive);
        addVisit("BinaryOperation", this::dealWithBinaryOperation);

        setDefaultVisit(this::defaultVisit);
    }

    private String dealWithClass(JmmNode node, String data) {
        scope = "CLASS";

        StringBuilder ollir = new StringBuilder();

        ollir.append(OllirTemplates.constructor(table.getClassName()));

        for (JmmNode child : node.getChildren()) {
            String ollirChild = visit(child, "CLASS");
            if (ollirChild != null && !ollirChild.equals("DEFAULT_VISIT"))
                ollir.append("\n").append(ollirChild);
        }

        ollir.append(OllirTemplates.closeBrackets());
        return ollir.toString();
    }

    private String dealWithMainDeclaration(JmmNode node, String data) {
        if (visited.contains(node)) return "DEFAULT_VISIT";
        visited.add(node);

        scope = "METHOD";

        try {
            currentMethod = table.getMethod("main", Arrays.asList(new Type("String", true)), new Type("void", false));
        } catch (Exception e) {
            currentMethod = null;
            e.printStackTrace();
        }

        StringBuilder builder = new StringBuilder(OllirTemplates.method(
                "main",
                currentMethod.parametersToOllir(),
                OllirTemplates.getType(currentMethod.getReturnType()),
                true));

        List<String> body = new ArrayList<>();

        for (JmmNode child : node.getChildren()) {
            String ollirChild = visit(child, "METHOD");
            if (ollirChild != null && !ollirChild.equals("DEFAULT_VISIT"))
                if (ollirChild.equals("")) continue;
            body.add(ollirChild);
        }

        builder.append(String.join("\n", body));

        builder.append(OllirTemplates.closeBrackets());

        return builder.toString();
    }

    private String dealWithMethodDeclaration(JmmNode node, String data) {
        if (visited.contains(node)) return "DEFAULT_VISIT";
        visited.add(node);

        List<Type> params = JmmMethod.parseParameters(node.get("params"));

        try {
            currentMethod = table.getMethod(node.get("name"), params, JmmSymbolTable.getType(node, "return"));
        } catch (Exception e) {
            currentMethod = null;
            e.printStackTrace();
        }

        StringBuilder builder = new StringBuilder(OllirTemplates.method(
                currentMethod.getName(),
                currentMethod.parametersToOllir(),
                OllirTemplates.getType(currentMethod.getReturnType())));

        List<String> body = new ArrayList<>();

        for (JmmNode child : node.getChildren()) {
            String ollirChild = visit(child, "METHOD");
            if (ollirChild != null && !ollirChild.equals("DEFAULT_VISIT"))
                if (ollirChild.equals("")) continue;
            body.add(ollirChild);
        }

        builder.append(String.join("\n", body));

        builder.append(OllirTemplates.closeBrackets());

        return builder.toString();
    }

    private String dealWithAssignment(JmmNode node, String data) {
        if (visited.contains(node)) return "DEFAULT_VISIT";
        visited.add(node);

        Map.Entry<Symbol, Boolean> variable;
        if ((variable = currentMethod.getField(node.get("variable"))) == null) {
            variable = table.getField(node.get("variable"));
        }

        StringBuilder ollir = new StringBuilder();
        ollir.append(OllirTemplates.variable(variable.getKey()));

        ollir.append(" := ");

        ollir.append(visit(node.getChildren().get(0), "ASSIGNMENT"));

        ollir.append(";");

        return ollir.toString();
    }

    private String dealWithPrimitive(JmmNode node, String data) {
        if (visited.contains(node)) return "DEFAULT_VISIT";
        visited.add(node);

        switch (node.getKind()) {
            case "IntegerLiteral":
                return ".i32 " + node.get("value") + ".i32";
            case "BooleanLiteral":
                return ".bool " + node.get("value") + ".bool";
            default:
                return null;
        }
    }

    private String dealWithBinaryOperation(JmmNode node, String data) {
        if (visited.contains(node)) return "DEFAULT_VISIT";
        visited.add(node);

        JmmNode left = node.getChildren().get(0);
        JmmNode right = node.getChildren().get(1);

        String leftReturn = visit(left, "BINARY_OP");
        String rightReturn = visit(right, "BINARY_OP");

        return String.format("%s %s %s", leftReturn, node.get("operation"), rightReturn);
    }

    private String defaultVisit(JmmNode node, String data) {
        return "DEFAULT_VISIT";
    }

    private static String reduce(String nodeResult, List<String> childrenResults) {
        var content = new StringBuilder();

        if (!nodeResult.equals("DEFAULT_VISIT"))
            content.append(nodeResult);

        for (var childResult : childrenResults) {
            if (!childResult.equals("DEFAULT_VISIT"))
                content.append(String.join("\n", StringLines.getLines(childResult)));
        }

        return content.toString();
    }

}
