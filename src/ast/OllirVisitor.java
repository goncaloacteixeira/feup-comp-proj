package ast;

import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.utilities.StringLines;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Input Data -> {scope, extra_data1, extra_data2, ...}
 * Output Data -> {OLLIR, extra_data1, extra_data2, ...}
 */
public class OllirVisitor extends PreorderJmmVisitor<List<Object>, List<Object>> {
    private final JmmSymbolTable table;
    private JmmMethod currentMethod;
    private final List<Report> reports;
    private String scope;
    private final Set<JmmNode> visited = new HashSet<>();

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

        addVisit("RelationalExpression", this::dealWithBinaryOperation);
        addVisit("AndExpression", this::dealWithAndExpression);
        addVisit("NotExpression", this::dealWithNotExpression);

        addVisit("IfStatement", this::dealWithIfStatement);
        addVisit("ElseStatement", this::dealWithElseStatement);
        addVisit("IfCondition", this::dealWithCondition);

        addVisit("While", this::dealWithWhile);
        addVisit("WhileCondition", this::dealWithCondition);

        setDefaultVisit(this::defaultVisit);
    }

    private List<Object> dealWithClass(JmmNode node, List<Object> data) {
        scope = "CLASS";

        StringBuilder ollir = new StringBuilder();

        ollir.append(OllirTemplates.constructor(table.getClassName()));

        for (JmmNode child : node.getChildren()) {
            String ollirChild = (String) visit(child, Collections.singletonList("CLASS")).get(0);
            if (ollirChild != null && !ollirChild.equals("DEFAULT_VISIT"))
                ollir.append("\n\n").append(ollirChild);
        }

        ollir.append(OllirTemplates.closeBrackets());
        return Collections.singletonList(ollir.toString());
    }

    private List<Object> dealWithMainDeclaration(JmmNode node, List<Object> data) {
        if (visited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        visited.add(node);

        scope = "METHOD";

        try {
            currentMethod = table.getMethod("main", Collections.singletonList(new Type("String", true)), new Type("void", false));
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
            String ollirChild = (String) visit(child, Collections.singletonList("METHOD")).get(0);
            if (ollirChild != null && !ollirChild.equals("DEFAULT_VISIT"))
                if (ollirChild.equals("")) continue;
            body.add(ollirChild);
        }

        builder.append(String.join("\n", body));

        builder.append(OllirTemplates.closeBrackets());

        return Collections.singletonList(builder.toString());
    }

    private List<Object> dealWithMethodDeclaration(JmmNode node, List<Object> data) {
        if (visited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
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
            String ollirChild = (String) visit(child, Collections.singletonList("METHOD")).get(0);
            if (ollirChild != null && !ollirChild.equals("DEFAULT_VISIT"))
                if (ollirChild.equals("")) continue;
            body.add(ollirChild);
        }

        builder.append(String.join("\n", body));

        builder.append(OllirTemplates.closeBrackets());

        return Collections.singletonList(builder.toString());
    }

    private List<Object> dealWithAssignment(JmmNode node, List<Object> data) {
        if (visited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        visited.add(node);

        Map.Entry<Symbol, Boolean> variable;
        if ((variable = currentMethod.getField(node.get("variable"))) == null) {
            variable = table.getField(node.get("variable"));
        }

        StringBuilder ollir = new StringBuilder();

        ollir.append(visit(node.getChildren().get(0), Arrays.asList("ASSIGNMENT", variable.getKey())).get(0));

        return Collections.singletonList(ollir.toString());
    }

    private List<Object> dealWithPrimitive(JmmNode node, List<Object> data) {
        if (visited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        visited.add(node);

        String value;
        Type type;

        switch (node.getKind()) {
            case "IntegerLiteral":
                value = node.get("value") + ".i32";
                type = new Type("int", false);
                break;
            case "BooleanLiteral":
                value = (node.get("value").equals("true") ? "1" : "0") + ".bool";
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

            return Collections.singletonList(String.format("%s :=%s %s;", OllirTemplates.variable(variable, name), OllirTemplates.type(type), value));
        } else {
            return Collections.singletonList(value);
        }
    }

    private List<Object> dealWithBinaryOperation(JmmNode node, List<Object> data) {
        if (visited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        visited.add(node);

        Symbol variable = (data.get(0).equals("ASSIGNMENT")) ? (Symbol) data.get(1) : null;
        String name = (variable != null) ? currentMethod.isParameter(variable) : null;

        JmmNode left = node.getChildren().get(0);
        JmmNode right = node.getChildren().get(1);

        String leftReturn = (String) visit(left, Collections.singletonList("BINARY")).get(0);
        String rightReturn = (String) visit(right, Collections.singletonList("BINARY")).get(0);

        String[] leftStmts = leftReturn.split("\n");
        String[] rightStmts = rightReturn.split("\n");

        StringBuilder ollir = new StringBuilder();
        
        String leftSide;
        String rightSide;

        leftSide = binaryOperations(leftStmts, ollir, new Type("int", false));
        rightSide = binaryOperations(rightStmts, ollir, new Type("int", false));

        if (variable == null) {
            ollir.append(String.format("%s %s.i32 %s", leftSide, node.get("operation"), rightSide));
        } else {
            ollir.append(String.format("%s :=.i32 %s;", OllirTemplates.variable(variable, name),
                    OllirTemplates.binary(leftSide, rightSide, node.get("operation"), new Type("int", false))));
        }

        return Collections.singletonList(ollir.toString());
    }

    private List<Object> dealWithVariable(JmmNode node, List<Object> data) {
        if (visited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
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
            return Collections.singletonList(OllirTemplates.variable(field.getKey(), name));
        }

        return Collections.singletonList("");
    }

    private List<Object> dealWithReturn(JmmNode node, List<Object> data) {
        if (visited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        visited.add(node);

        String exp = (String) visit(node.getChildren().get(0), Collections.singletonList("RETURN")).get(0);

        return Collections.singletonList(OllirTemplates.ret(currentMethod.getReturnType(), exp));
    }

    private List<Object> dealWithAndExpression(JmmNode node, List<Object> data) {
        if (visited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        visited.add(node);

        Symbol variable = (data.get(0).equals("ASSIGNMENT")) ? (Symbol) data.get(1) : null;
        String name = (variable != null) ? currentMethod.isParameter(variable) : null;

        JmmNode left = node.getChildren().get(0);
        JmmNode right = node.getChildren().get(1);

        String leftReturn = (String) visit(left, Collections.singletonList("AND")).get(0);
        String rightReturn = (String) visit(right, Collections.singletonList("AND")).get(0);

        String[] leftStmts = leftReturn.split("\n");
        String[] rightStmts = rightReturn.split("\n");

        StringBuilder ollir = new StringBuilder();

        String leftSide;
        String rightSide;

        leftSide = binaryOperations(leftStmts, ollir, new Type("boolean", false));
        rightSide = binaryOperations(rightStmts, ollir, new Type("boolean", false));

        if (variable == null) {
            ollir.append(String.format("%s %s.bool %s", leftSide, node.get("operation"), rightSide));
        } else {
            ollir.append(String.format("%s :=.bool %s;", OllirTemplates.variable(variable, name),
                    OllirTemplates.binary(leftSide, rightSide, node.get("operation"), new Type("boolean", false))));
        }

        return Collections.singletonList(ollir.toString());
    }

    private List<Object> dealWithNotExpression(JmmNode node, List<Object> data) {
        if (visited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        visited.add(node);

        StringBuilder ollir = new StringBuilder();
        String expression = (String) visit(node.getChildren().get(0), Collections.singletonList("NOT")).get(0);
        String[] parts = expression.split("\n");

        String last = parts[parts.length - 1];

        Symbol variable = (data.get(0).equals("ASSIGNMENT")) ? (Symbol) data.get(1) : null;
        String name = (variable != null) ? currentMethod.isParameter(variable) : null;

        if (parts.length > 1) {
            for (int i = 0; i < parts.length - 1; i++) {
                ollir.append(parts[i]).append("\n");
            }
        }

        String[] expressionParts;
        if ((expressionParts = last.split("<")).length == 2) {
            if (variable == null) {
                ollir.append(String.format("%s>=%s", expressionParts[0], expressionParts[1]));
            } else {
                ollir.append(String.format("%s :=.bool %s>=%s;", OllirTemplates.variable(variable, name), expressionParts[0], expressionParts[1]));
            }
        } else if ((expressionParts = last.split(">=")).length == 2) {
            if (variable == null) {
                ollir.append(String.format("%s<%s", expressionParts[0], expressionParts[1]));
            } else {
                ollir.append(String.format("%s :=.bool %s<%s;", OllirTemplates.variable(variable, name), expressionParts[0], expressionParts[1]));
            }
        } else if (expression.equals("0.bool")) {
            if (variable == null) {
                ollir.append("1.bool");
            } else {
                ollir.append(String.format("%s :=.bool 1.bool;", OllirTemplates.variable(variable, name)));
            }
        } else if (expression.equals("1.bool")) {
            if (variable == null) {
                ollir.append("0.bool");
            } else {
                ollir.append(String.format("%s :=.bool 0.bool;", OllirTemplates.variable(variable, name)));
            }
        } else {
            ollir.append(String.format("%s :=.bool %s;\n", OllirTemplates.variable(new Symbol(new Type("boolean", false), "temporary" + temp_sequence)), last));
            if (variable == null) {
                ollir.append(String.format("%s !.bool %s",
                        OllirTemplates.variable(new Symbol(new Type("boolean", false), "temporary" + temp_sequence)),
                        OllirTemplates.variable(new Symbol(new Type("boolean", false), "temporary" + temp_sequence++))));
            } else {
                ollir.append(String.format("%s :=.bool %s !.bool %s;",
                        OllirTemplates.variable(variable, name),
                        OllirTemplates.variable(new Symbol(new Type("boolean", false), "temporary" + temp_sequence)),
                        OllirTemplates.variable(new Symbol(new Type("boolean", false), "temporary" + temp_sequence++))));
            }
        }

        return Collections.singletonList(ollir.toString());
    }

    private List<Object> dealWithIfStatement(JmmNode node, List<Object> data) {
        if (visited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        visited.add(node);

        StringBuilder ollir = new StringBuilder();

        String ifCondition = (String) visit(node.getChildren().get(0), Collections.singletonList("CONDITION")).get(0);

        String[] ifConditionParts = ifCondition.split("\n");
        if (ifConditionParts.length > 1) {
            for (int i = 0; i < ifConditionParts.length - 1; i++) {
                ollir.append(ifConditionParts[i]).append("\n");
            }
        }

        ollir.append(String.format("if (%s) goto ifbody;\n", ifConditionParts[ifConditionParts.length - 1]));

        ollir.append(visit(node.getParent().getChildren().get(1), Collections.singletonList("ELSE")).get(0));
        ollir.append("\n");
        ollir.append("goto endif;\n");

        ollir.append("ifbody:\n");
        List<String> ifBody = new ArrayList<>();
        for (int i = 1; i < node.getChildren().size(); i++) {
            ifBody.add((String) visit(node.getChildren().get(i), Collections.singletonList("IF")).get(0));
        }
        ollir.append(String.join("\n", ifBody)).append("\n");
        ollir.append("goto endif;\n");
        ollir.append("endif:\n");

        return Collections.singletonList(ollir.toString());
    }

    private List<Object> dealWithElseStatement(JmmNode node, List<Object> data) {
        if (visited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        visited.add(node);

        StringBuilder ollir = new StringBuilder();

        ollir.append("else:").append("\n");

        List<String> elseBody = new ArrayList<>();

        for (int i = 0; i < node.getChildren().size(); i++) {
            elseBody.add((String) visit(node.getChildren().get(i), Collections.singletonList("ELSE")).get(0));
        }

        ollir.append(String.join("\n", elseBody));

        return Collections.singletonList(ollir.toString());
    }

    private List<Object> dealWithWhile(JmmNode node, List<Object> data) {
        if (visited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        visited.add(node);

        StringBuilder ollir = new StringBuilder();

        ollir.append("loop:\n");

        String condition = (String) visit(node.getChildren().get(0), Collections.singletonList("WHILE")).get(0);
        String[] conditionParts = condition.split("\n");
        if (conditionParts.length > 1) {
            for (int i = 0; i < conditionParts.length - 1; i++) {
                ollir.append(conditionParts[i]).append("\n");
            }
        }

        ollir.append(String.format("if (%s) goto loopbody;\n", conditionParts[conditionParts.length - 1]));
        ollir.append("end:\ngoto endloop;\n");

        ollir.append("loopbody:\n");
        List<String> body = new ArrayList<>();
        for (int i = 1; i < node.getChildren().size(); i++) {
            body.add((String) visit(node.getChildren().get(i)).get(0));
        }
        ollir.append(String.join("\n", body)).append("\n");

        ollir.append("goto loop;\n");
        ollir.append("endloop:");

        return Collections.singletonList(ollir.toString());
    }

    private  List<Object> dealWithCondition(JmmNode node, List<Object> data) {
        if (visited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        visited.add(node);

        return visit(node.getChildren().get(0), Collections.singletonList("CONDITION"));
    }

    private List<Object> dealWithAccessExpression(JmmNode node, List<Object> data) {
        if (visited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        visited.add(node);
        // TODO
        return visit(node.getChildren().get(0), Collections.singletonList("CONDITION"));
    }

    private List<Object> dealWithMethodCall(JmmNode node, List<Object> data) {
        if (visited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        visited.add(node);
        // TODO
        return visit(node.getChildren().get(0), Collections.singletonList("CONDITION"));
    }


    private List<Object> defaultVisit(JmmNode node, List<Object> data) {
        return Collections.singletonList("DEFAULT_VISIT");
    }

    private static List<Object> reduce(List<Object> nodeResult, List<List<Object>> childrenResults) {
        var content = new StringBuilder();

        if (!nodeResult.get(0).equals("DEFAULT_VISIT"))
            content.append(nodeResult.get(0));

        for (var childResult : childrenResults) {
            if (!childResult.get(0).equals("DEFAULT_VISIT"))
                content.append(String.join("\n", StringLines.getLines((String) childResult.get(0))));
        }

        return Collections.singletonList(content.toString());
    }

    private String binaryOperations(String[] statements, StringBuilder ollir, Type type) {
        String finalStmt;
        if (statements.length > 1) {
            for (int i = 0; i < statements.length - 1; i++) {
                ollir.append(statements[i]).append("\n");
            }
            String last = statements[statements.length - 1];
            if (last.split("(/|\\+|-|\\*|&&|<|!|>=)(\\.i32|\\.bool)").length == 2) {
                Pattern p = Pattern.compile("(/|\\+|-|\\*|&&|<|!|>=)(\\.i32|\\.bool)");
                Matcher m = p.matcher(last);

                m.find();

                ollir.append(String.format("%s :=%s %s;\n", OllirTemplates.variable(new Symbol(type, String.format("temporary%d", temp_sequence))), m.group(2), last));
                finalStmt = OllirTemplates.variable(new Symbol(type, String.format("temporary%d", temp_sequence++)));
            } else {
                finalStmt = last;
            }
        } else {
            if (statements[0].split("(/|\\+|-|\\*|&&|<|!|>=)(\\.i32|\\.bool)").length == 2) {
                Pattern p = Pattern.compile("(/|\\+|-|\\*|&&|<|!|>=)(\\.i32|\\.bool)");
                Matcher m = p.matcher(statements[0]);
                m.find();

                ollir.append(String.format("%s :=%s %s;\n", OllirTemplates.variable(new Symbol(type, String.format("temporary%d", temp_sequence))), m.group(2), statements[0]));
                finalStmt = OllirTemplates.variable(new Symbol(type, String.format("temporary%d", temp_sequence++)));
            } else {
                finalStmt = statements[0];
            }
        }
        return finalStmt;
    }
}
