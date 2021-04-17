package ast;

import ast.exceptions.*;
import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Counts the occurrences of each node kind.
 *
 * @author JBispo
 */
public class JmmExpressionAnalyser extends PreorderJmmVisitor<Boolean, Map.Entry<String, String>> {
    private final JmmSymbolTable table;
    private final List<Report> reports;
    private String scope;
    private JmmMethod currentMethod;

    public JmmExpressionAnalyser(JmmSymbolTable table, List<Report> reports) {
        this.table = table;
        this.reports = reports;

        // DATA -> <return type, result (expression eval)>

        addVisit("BinaryOperation", this::dealWithBinaryOperation);
        addVisit("RelationalExpression", this::dealWithRelationalExpression);
        addVisit("AndExpression", this::dealWithAndExpression);
        addVisit("NotExpression", this::dealWithNotExpression);
        addVisit("IfCondition", this::dealConditionalExpression);
        addVisit("WhileCondition", this::dealConditionalExpression);
        addVisit("IntegerLiteral", this::dealWithPrimitive);
        addVisit("BooleanLiteral", this::dealWithPrimitive);
        addVisit("ArrayInit", this::dealWithArrayInit);
        addVisit("ArrayAccess", this::dealWithArrayAccess);
        addVisit("Variable", this::dealWithVariable);
        addVisit("Assignment", this::dealWithAssignment);

        addVisit("ClassDeclaration", this::dealWithClassDeclaration);
        addVisit("MainMethod", this::dealWithMainDeclaration);
        addVisit("ClassMethod", this::dealWithMethodDeclaration);
        addVisit("AccessExpression", this::dealWithAccessExpression);
        addVisit("MethodCall", this::dealWithMethodCall);
        addVisit("Length", this::dealWithMethodCall);
        addVisit("NewObject", this::dealWithNewObject);

        addVisit("Return", this::dealWithReturn);
    }


    private Map.Entry<String, String> dealWithArrayAccess(JmmNode node, Boolean data) {
        JmmNode index = node.getChildren().get(0);
        Map.Entry<String, String> indexReturn = visit(index, true);

        if (!indexReturn.getKey().equals("int")) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(index.get("line")), Integer.parseInt(index.get("col")), "Array access index is not an Integer: " + index));
            return Map.entry("error", "null");
        }

        return Map.entry("index", indexReturn.getValue());
    }

    private Map.Entry<String, String> dealWithArrayInit(JmmNode node, Boolean data) {
        JmmNode size = node.getChildren().get(0);
        Map.Entry<String, String> sizeReturn = visit(size, true);

        if (!sizeReturn.getKey().equals("int")) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(size.get("line")), Integer.parseInt(size.get("col")), "Array init size is not an Integer: " + size));
            return Map.entry("error", "null");
        }

        return Map.entry("int []", "null");
    }

    private Map.Entry<String, String> dealConditionalExpression(JmmNode node, Boolean data) {
        JmmNode condition = node.getChildren().get(0);
        Map.Entry<String, String> conditionReturn = visit(condition, true);

        Map.Entry<String, String> dataReturn = Map.entry("boolean", "null");

        if (!conditionReturn.getKey().equals("boolean")) {
            dataReturn = Map.entry("error", "null");
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(condition.get("line")), Integer.parseInt(condition.get("col")), "Conditional expression not boolean"));
        }

        return dataReturn;
    }

    private Map.Entry<String, String> dealWithRelationalExpression(JmmNode node, Boolean data) {
        JmmNode left = node.getChildren().get(0);
        JmmNode right = node.getChildren().get(1);

        Map.Entry<String, String> leftReturn = visit(left, true);
        Map.Entry<String, String> rightReturn = visit(right, true);

        Map.Entry<String, String> dataReturn = Map.entry("boolean", "null");

        if (!leftReturn.getValue().equals("true") && left.getKind().equals("Variable")) {
            dataReturn = Map.entry("error", "null");
            if (data != null) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(left.get("line")), Integer.parseInt(left.get("col")), "Left Member not initialized: " + left));
            }
        }
        if (!rightReturn.getValue().equals("true") && right.getKind().equals("Variable")) {
            dataReturn = Map.entry("error", "null");
            if (data != null) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(right.get("line")), Integer.parseInt(right.get("col")), "Right Member not initialized: " + right));
            }
        }

        if (!leftReturn.getKey().equals("int")) {
            dataReturn = Map.entry("error", "null");
            if (data != null) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(left.get("line")), Integer.parseInt(left.get("col")), "Left Member not integer: " + left));
            }
        }
        if (!rightReturn.getKey().equals("int")) {
            dataReturn = Map.entry("error", "null");
            if (data != null) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(right.get("line")), Integer.parseInt(right.get("col")), "Right Member not integer: " + right));
            }
        }

        if (dataReturn.getKey().equals("boolean")) {
            return dataReturn;
        } else {
            return Map.entry("error", "null");
        }
    }

    private Map.Entry<String, String> dealWithAndExpression(JmmNode node, Boolean data) {
        JmmNode left = node.getChildren().get(0);
        JmmNode right = node.getChildren().get(1);

        Map.Entry<String, String> leftReturn = visit(left, true);
        Map.Entry<String, String> rightReturn = visit(right, true);

        Map.Entry<String, String> dataReturn = Map.entry("boolean", "null");

        if (!leftReturn.getValue().equals("true") && left.getKind().equals("Variable")) {
            dataReturn = Map.entry("error", "null");
            if (data != null) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(left.get("line")), Integer.parseInt(left.get("col")), "Left Member not initialized: " + left));
            }
        }
        if (!rightReturn.getValue().equals("true") && right.getKind().equals("Variable")) {
            dataReturn = Map.entry("error", "null");
            if (data != null) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(right.get("line")), Integer.parseInt(right.get("col")), "Right Member not initialized: " + right));
            }
        }

        if (!leftReturn.getKey().equals("boolean")) {
            dataReturn = Map.entry("error", "null");
            if (data != null) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(left.get("line")), Integer.parseInt(left.get("col")), "Left Member not boolean: " + left));
            }
        }
        if (!rightReturn.getKey().equals("boolean")) {
            dataReturn = Map.entry("error", "null");
            if (data != null) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(right.get("line")), Integer.parseInt(right.get("col")), "Right Member not boolean: " + right));
            }
        }

        if (dataReturn.getKey().equals("boolean")) {
            return dataReturn;
        } else {
            return Map.entry("error", "null");
        }
    }

    private Map.Entry<String, String> dealWithNotExpression(JmmNode node, Boolean data) {
        JmmNode bool = node.getChildren().get(0);

        Map.Entry<String, String> boolReturn = visit(bool, true);

        Map.Entry<String, String> dataReturn = Map.entry("boolean", "null");

        if (!boolReturn.getValue().equals("true") && bool.getKind().equals("Variable")) {
            dataReturn = Map.entry("error", "null");
            if (data != null) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(bool.get("line")), Integer.parseInt(bool.get("col")), "Member not initialized: " + bool));
            }
        }

        if (!boolReturn.getKey().equals("boolean")) {
            dataReturn = Map.entry("error", "null");
            if (data != null) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(bool.get("line")), Integer.parseInt(bool.get("col")), "Operator '!' cannot be applied to " + boolReturn.getKey().replace(" ","")));
            }
        }

        if (dataReturn.getKey().equals("boolean")) {
            return dataReturn;
        } else {
            return Map.entry("error", "null");
        }
    }

    private Map.Entry<String, String> dealWithBinaryOperation(JmmNode node, Boolean data) {
        JmmNode left = node.getChildren().get(0);
        JmmNode right = node.getChildren().get(1);

        Map.Entry<String, String> leftReturn = visit(left, true);
        Map.Entry<String, String> rightReturn = visit(right, true);

        Map.Entry<String, String> dataReturn = Map.entry("int", "null");

        if (!leftReturn.getValue().equals("true") && left.getKind().equals("Variable")) {
            dataReturn = Map.entry("error", "null");
            if (data != null) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(left.get("line")), Integer.parseInt(left.get("col")), "Left Member not initialized: " + left));
            }
        }
        if (!rightReturn.getValue().equals("true") && right.getKind().equals("Variable")) {
            dataReturn = Map.entry("error", "null");
            if (data != null) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(right.get("line")), Integer.parseInt(right.get("col")), "Right Member not initialized: " + right));
            }
        }

        if (!leftReturn.getKey().equals("int")) {
            dataReturn = Map.entry("error", "null");
            if (data != null) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(left.get("line")), Integer.parseInt(left.get("col")), "Left Member not integer: " + left));
            }
        }
        if (!rightReturn.getKey().equals("int")) {
            dataReturn = Map.entry("error", "null");
            if (data != null) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(right.get("line")), Integer.parseInt(right.get("col")), "Right Member not integer: " + right));
            }
        }

        if (dataReturn.getKey().equals("int")) {
            return dataReturn;
        } else {
            return Map.entry("error", "null");
        }
    }


    private Map.Entry<String, String> dealWithPrimitive(JmmNode node, Boolean data) {
        String return_type;

        switch (node.getKind()) {
            case "IntegerLiteral":
                return_type = "int";
                break;
            case "BooleanLiteral":
                return_type = "boolean";
                break;
            default:
                return_type = "error";
                break;
        }

        return Map.entry(return_type, "true");
    }

    private Map.Entry<String, String> dealWithVariable(JmmNode node, Boolean data) {
        Map.Entry<Symbol, Boolean> field = null;

        if (scope.equals("CLASS")) {
            field = table.getField(node.get("name"));
        } else if (scope.equals("METHOD") && currentMethod != null) {
            field = currentMethod.getField(node.get("name"));
            if (field == null) {
                field = table.getField(node.get("name"));
            }
        }

        if (field == null && table.getImports().contains(node.get("name"))) {
            return Map.entry("access", "true");
        } else if (field == null && node.get("name").equals("this")) {
            return Map.entry("method", "true");
        }

        if (field == null) {
            return Map.entry("error", "null");
        } else {
            return Map.entry(field.getKey().getType().getName() + (field.getKey().getType().isArray() ? " []" : ""), field.getValue() ? "true" : "null");
        }
    }


    private Map.Entry<String, String> dealWithClassDeclaration(JmmNode node, Boolean data) {
        scope = "CLASS";
        return null;
    }


    private Map.Entry<String, String> dealWithMethodDeclaration(JmmNode node, Boolean data) {
        scope = "METHOD";

        List<Type> params = JmmMethod.parseParameters(node.get("params"));

        try {
            currentMethod = table.getMethod(node.get("name"), params, JmmSymbolTable.getType(node, "return"));
        } catch (Exception e) {
            currentMethod = null;
            e.printStackTrace();
        }

        return null;
    }

    private Map.Entry<String, String> dealWithMainDeclaration(JmmNode node, Boolean data) {
        scope = "METHOD";

        try {
            currentMethod = table.getMethod("main", Arrays.asList(new Type("String", true)), new Type("void", false));
        } catch (Exception e) {
            currentMethod = null;
            e.printStackTrace();
        }

        return null;
    }

    private Map.Entry<String, String> dealWithAccessExpression(JmmNode node, Boolean requested) {
        JmmNode target = node.getChildren().get(0);
        JmmNode method = node.getChildren().get(1);

        Map.Entry<String, String> targetReturn = visit(target, true);
        Map.Entry<String, String> methodReturn = visit(method, true);

        if (targetReturn.getKey().equals("error")) {
            if (requested != null || !node.getParent().getKind().equals("Assignment"))
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Invalid Access"));
            return Map.entry("error", "null");
        }

        if (methodReturn.getKey().equals("error")) {
            if (targetReturn.getKey().equals("access")) {
                return Map.entry("access", "null");
            } else {
                if (requested != null || !node.getParent().getKind().equals("Assignment"))
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "No such method"));
                return Map.entry("error", "null");
            }
        }

        else if (targetReturn.getKey().equals("method") || targetReturn.getKey().equals(table.getClassName())) {
            return Map.entry(methodReturn.getValue(), "null");
        } else if (targetReturn.getKey().equals("access")) {
            return Map.entry("access", "null");
        } else if (targetReturn.getKey().equals("int") || targetReturn.getKey().equals("boolean")) {
            if (requested != null) reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Target cannot be primitive"));
            return Map.entry("error", "null");
        } else if (targetReturn.getKey().equals("int []")) {
            if (methodReturn.getKey().equals("length")) {
                return Map.entry("int", "length");
            }
            if (methodReturn.getKey().equals("index")) {
                return Map.entry("int", "index");
            }
        } else if (target.get("name").equals(table.getClassName())) {
            return Map.entry(methodReturn.getValue(), "index");
        } else if (table.getImports().contains(target.get("name"))) {
            return Map.entry("access", "null");
        }

        return Map.entry("error", "null");
    }

    private Map.Entry<String, String> dealWithMethodCall(JmmNode node, Boolean space) {
        if (node.getKind().equals("Length")) {
            return Map.entry("length", "null");
        }

        List<JmmNode> children = node.getChildren();
        List<Type> params = getParametersList(children);
        Type returnType = table.getReturnType(node.get("value"));

        try {
            table.getMethod(node.get("value"), params, returnType);
        } catch (NoSuchMethod noSuchMethod) {
            if (this.table.getSuper() == null) {
                return Map.entry("error", "noSuchMethod");
            } else {
                return Map.entry("method", "access");
            }
        }

        return Map.entry("method", returnType.getName() + (returnType.isArray() ? " []" : ""));
    }

    private List<Type> getParametersList(List<JmmNode> children) {
        //TODO - visitor para os parametros em vez disto maybe
        List<Type> params = new ArrayList<>();
        for (JmmNode child : children) {
            switch (child.getKind()) {
                case "IntegerLiteral":
                    params.add(new Type("int", false));
                    break;
                case "BooleanLiteral":
                    params.add(new Type("boolean", false));
                    break;
                case "Variable":
                case "AccessExpression":
                case "BinaryOperation":
                    Map.Entry<String, String> var = visit(child, true);
                    String[] type = var.getKey().split(" ");
                    params.add(new Type(type[0], type.length == 2));
                    break;
                default:
                    break;
            }
        }
        return params;
    }

    private Map.Entry<String, String> dealWithAssignment(JmmNode node, Boolean space) {
        List<JmmNode> children = node.getChildren();

        if (children.size() == 1) {
            Map.Entry<String, String> assignment = visit(node.getChildren().get(0), true);

            if (assignment.getKey().equals("error")) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Undeclared Variable: " + node.getChildren().get(0)));
                return null;
            }

            Map.Entry<Symbol, Boolean> variable;
            if ((variable = currentMethod.getField(node.get("variable"))) == null) {
                variable = table.getField(node.get("variable"));
            }

            // IF assignment is related to an access to an imported static method
            if (assignment.getKey().equals("access")) {
                variable.setValue(true);
                return null;
            }

            String[] parts = assignment.getKey().split(" ");

            // Matching Types
            if (variable.getKey().getType().getName().equals(parts[0])) {
                if (!(parts.length == 2 && variable.getKey().getType().isArray()) && !(parts.length == 1 && !variable.getKey().getType().isArray())) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Mismatched types: " + variable.getKey().getType() + " and " + assignment.getKey()));
                    return null;
                }
            } else {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Mismatched types: " + variable.getKey().getType() + " and " + assignment.getKey()));
                return null;
            }

            if (!currentMethod.initializeField(variable.getKey())) {
                table.initializeField(variable.getKey());
            }

        } else {
            Map.Entry<Symbol, Boolean> array;
            if ((array = currentMethod.getField(node.get("variable"))) == null) {
                array = table.getField(node.get("variable"));
            }

            Map.Entry<Symbol, Boolean> variable;
            if ((variable = currentMethod.getField(node.get("variable"))) == null) {
                variable = table.getField(node.get("variable"));
            }

            if (!array.getKey().getType().equals(variable.getKey().getType())) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Mismatched types: " + variable.getKey().getType() + " and " + array.getKey().getType()));
            } else {
                if (!currentMethod.initializeField(variable.getKey())) {
                    table.initializeField(variable.getKey());
                }
            }
        }

        return null;
    }

    private Map.Entry<String, String> dealWithReturn(JmmNode node, Boolean space) {
        String returnType = visit(node.getChildren().get(0), true).getKey();

        if (returnType.equals("access")) {
            return null;
        }

        String[] parts = returnType.split(" ");
        if (parts.length == 2 && currentMethod.getReturnType().isArray()) {
            if (!parts[0].equals(currentMethod.getReturnType().getName())) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Return type mismatch"));
            }
            return null;
        } else if (parts.length == 1 && !currentMethod.getReturnType().isArray()) {
            if (!parts[0].equals(currentMethod.getReturnType().getName())) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Return type mismatch"));
            }
            return null;
        } else {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Return type mismatch"));
        }

        return null;
    }

    private Map.Entry<String, String> dealWithNewObject(JmmNode node, Boolean data) {
        return Map.entry(node.get("value"), "object");
    }
}
