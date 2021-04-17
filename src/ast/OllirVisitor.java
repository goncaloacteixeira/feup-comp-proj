package ast;

import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.utilities.StringLines;

import java.util.*;

/**
 * Data -> {scope, extra_data1, extra_data2}
 */
public class OllirVisitor extends PreorderJmmVisitor<List<Object>, String> {
    private final JmmSymbolTable table;
    private JmmMethod currentMethod;
    private final List<Report> reports;
    private String scope;
    private Set<JmmNode> visited = new HashSet<>();

    private int temp_sequence = 1;

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
        addVisit("Variable", this::dealWithVariable);
        addVisit("Return", this::dealWithReturn);

        setDefaultVisit(this::defaultVisit);
    }

    private String dealWithClass(JmmNode node, List<Object> data) {
        scope = "CLASS";

        StringBuilder ollir = new StringBuilder();

        ollir.append(OllirTemplates.constructor(table.getClassName()));

        for (JmmNode child : node.getChildren()) {
            String ollirChild = visit(child, Arrays.asList("CLASS"));
            if (ollirChild != null && !ollirChild.equals("DEFAULT_VISIT"))
                ollir.append("\n").append(ollirChild);
        }

        ollir.append(OllirTemplates.closeBrackets());
        return ollir.toString();
    }

    private String dealWithMainDeclaration(JmmNode node, List<Object> data) {
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
                OllirTemplates.type(currentMethod.getReturnType()),
                true));

        List<String> body = new ArrayList<>();

        for (JmmNode child : node.getChildren()) {
            String ollirChild = visit(child, Arrays.asList("METHOD"));
            if (ollirChild != null && !ollirChild.equals("DEFAULT_VISIT"))
                if (ollirChild.equals("")) continue;
            body.add(ollirChild);
        }

        builder.append(String.join("\n", body));

        builder.append(OllirTemplates.closeBrackets());

        return builder.toString();
    }

    private String dealWithMethodDeclaration(JmmNode node, List<Object> data) {
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
                OllirTemplates.type(currentMethod.getReturnType())));

        List<String> body = new ArrayList<>();

        for (JmmNode child : node.getChildren()) {
            String ollirChild = visit(child, Arrays.asList("METHOD"));
            if (ollirChild != null && !ollirChild.equals("DEFAULT_VISIT"))
                if (ollirChild.equals("")) continue;
            body.add(ollirChild);
        }

        builder.append(String.join("\n", body));

        builder.append(OllirTemplates.closeBrackets());

        return builder.toString();
    }

    private String dealWithAssignment(JmmNode node, List<Object> data) {
        if (visited.contains(node)) return "DEFAULT_VISIT";
        visited.add(node);

        Map.Entry<Symbol, Boolean> variable;
        if ((variable = currentMethod.getField(node.get("variable"))) == null) {
            variable = table.getField(node.get("variable"));
        }

        StringBuilder ollir = new StringBuilder();

        ollir.append(visit(node.getChildren().get(0), Arrays.asList("ASSIGNMENT", variable.getKey())));

        return ollir.toString();
    }

    private String dealWithPrimitive(JmmNode node, List<Object> data) {
        if (visited.contains(node)) return "DEFAULT_VISIT";
        visited.add(node);

        String value;
        Type type;

        switch (node.getKind()) {
            case "IntegerLiteral":
                value = node.get("value") + ".i32";
                type = new Type("int", false);
                break;
            case "BooleanLiteral":
                value = node.get("value") + ".bool";
                type = new Type("boolean", false);
                break;
            default:
                value = "";
                type = new Type("void", false);
                break;
        }

        if (data.get(0).equals("ASSIGNMENT")) {
            Symbol variable = (Symbol) data.get(1);

            String name = currentMethod.isParameter(variable);

            return String.format("%s := %s %s;", OllirTemplates.variable(variable, name), OllirTemplates.type(type), value);
        } else {
            return value;
        }
    }

    private String dealWithBinaryOperation(JmmNode node, List<Object> data) {
        if (visited.contains(node)) return "DEFAULT_VISIT";
        visited.add(node);

        Symbol variable = (data.get(0).equals("ASSIGNMENT")) ? (Symbol) data.get(1) : null;
        String name = (variable != null) ? currentMethod.isParameter(variable) : null;

        JmmNode left = node.getChildren().get(0);
        JmmNode right = node.getChildren().get(1);

        String leftReturn;
        String rightReturn;

        if (variable != null) {
            if (left.getKind().equals("BinaryOperation") && right.getKind().equals("BinaryOperation")) {
                leftReturn = visit(left, Arrays.asList("ASSIGNMENT", new Symbol(new Type("int", false), String.format("temporary%d", temp_sequence++))));
                rightReturn = visit(right, Arrays.asList("ASSIGNMENT", new Symbol(new Type("int", false), String.format("temporary%d", temp_sequence++))));

                String aux1 = leftReturn.split(" := ")[0];
                String aux2 = rightReturn.split(" := ")[0];

                StringBuilder ollir = new StringBuilder();
                ollir.append(leftReturn).append("\n");
                ollir.append(rightReturn).append("\n");

                ollir.append(String.format("%s := %s;", OllirTemplates.variable(variable, name),
                        OllirTemplates.binary(new Type("int", false), aux1, new Type("int", false), aux2, node.get("operation"))));

                return ollir.toString();
            } else if (left.getKind().equals("BinaryOperation") && !right.getKind().equals("BinaryOperation")) {
                leftReturn = visit(left, Arrays.asList("ASSIGNMENT", new Symbol(new Type("int", false), String.format("temporary%d", temp_sequence++))));
                rightReturn = visit(right, Arrays.asList("BINARY_OP"));

                String aux1 = leftReturn.split(" := ")[0];

                StringBuilder ollir = new StringBuilder();
                ollir.append(leftReturn).append("\n");

                ollir.append(String.format("%s := %s;", OllirTemplates.variable(variable, name),
                        OllirTemplates.binary(new Type("int", false), aux1, new Type("int", false), rightReturn, node.get("operation"))));

                return ollir.toString();
            } else if (!left.getKind().equals("BinaryOperation") && right.getKind().equals("BinaryOperation")) {
                leftReturn = visit(left, Arrays.asList("BINARY_OP"));
                rightReturn = visit(right, Arrays.asList("ASSIGNMENT", new Symbol(new Type("int", false), String.format("temporary%d", temp_sequence++))));

                String aux1 = rightReturn.split(" := ")[0];

                StringBuilder ollir = new StringBuilder();
                ollir.append(rightReturn).append("\n");

                ollir.append(String.format("%s := %s;", OllirTemplates.variable(variable, name),
                        OllirTemplates.binary(new Type("int", false), leftReturn, new Type("int", false), aux1, node.get("operation"))));

                return ollir.toString();
            } else {
                leftReturn = visit(left, Arrays.asList("BINARY_OP"));
                rightReturn = visit(right, Arrays.asList("BINARY_OP"));

                return String.format("%s := %s;", OllirTemplates.variable(variable, name),
                        OllirTemplates.binary(new Type("int", false), leftReturn, new Type("int", false), rightReturn, node.get("operation")));
            }
        } else {
            leftReturn = visit(left, Arrays.asList("BINARY_OP"));
            rightReturn = visit(right, Arrays.asList("BINARY_OP"));

            return String.format("%s %s %s", leftReturn, node.get("operation"), rightReturn);
        }
    }

    private String dealWithVariable(JmmNode node, List<Object> data) {
        if (visited.contains(node)) return "DEFAULT_VISIT";
        visited.add(node);

        Map.Entry<Symbol, Boolean> field = null;

        if (scope.equals("CLASS")) {
            field = table.getField(node.get("name"));
        } else if (scope.equals("METHOD") && currentMethod != null) {
            field = currentMethod.getField(node.get("name"));
            if (field == null) {
                field = table.getField(node.get("name"));
            }
        }

        if (field != null) {
            String name = currentMethod.isParameter(field.getKey());
            return OllirTemplates.variable(field.getKey(), name);
        }

        return "";
    }

    private String dealWithReturn(JmmNode node, List<Object> data) {
        if (visited.contains(node)) return "DEFAULT_VISIT";
        visited.add(node);

        String exp = visit(node.getChildren().get(0), Arrays.asList("RETURN"));

        return OllirTemplates.ret(currentMethod.getReturnType(), exp);
    }

    private String defaultVisit(JmmNode node, List<Object> data) {
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
