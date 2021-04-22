package ast;

import ast.exceptions.NoSuchMethod;
import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.utilities.StringLines;

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
    private int if_label_sequence = 1;

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

        addVisit("VarDeclaration", this::dealWithVarDeclaration);

        addVisit("RelationalExpression", this::dealWithBinaryOperation);
        addVisit("AndExpression", this::dealWithAndExpression);
        addVisit("NotExpression", this::dealWithNotExpression);

        addVisit("IfStatement", this::dealWithIfStatement);
        addVisit("ElseStatement", this::dealWithElseStatement);
        addVisit("IfCondition", this::dealWithCondition);

        addVisit("While", this::dealWithWhile);
        addVisit("WhileCondition", this::dealWithCondition);

        addVisit("AccessExpression", this::dealWithAccessExpression);
        addVisit("MethodCall", this::dealWithMethodCall);
        addVisit("Length", this::dealWithMethodCall);

        addVisit("ArrayInit", this::dealWithArrayInit);
        addVisit("NewObject", this::dealWithNewObject);
        addVisit("ArrayAccess", this::dealWithArrayAccess);

        setDefaultVisit(this::defaultVisit);
    }

    private List<Object> dealWithClass(JmmNode node, List<Object> data) {
        scope = "CLASS";

        StringBuilder ollir = new StringBuilder();

        ollir.append(OllirTemplates.classTemplate(table.getClassName()));

        List<String> classBody = new ArrayList<>();

        for (JmmNode child : node.getChildren()) {
            String ollirChild = (String) visit(child, Collections.singletonList("CLASS")).get(0);
            if (ollirChild != null && !ollirChild.equals("DEFAULT_VISIT"))
                classBody.add(ollirChild);
        }

        ollir.append(String.join("\n\n", classBody));

        ollir.append(OllirTemplates.closeBrackets());
        return Collections.singletonList(ollir.toString());
    }

    private List<Object> dealWithVarDeclaration(JmmNode node, List<Object> data) {
        if (visited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        visited.add(node);

        if ("CLASS".equals(data.get(0))) {
            Map.Entry<Symbol, Boolean> variable = table.getField(node.get("identifier"));
            return Arrays.asList(OllirTemplates.field(variable.getKey()));
        }
        return Arrays.asList("DEFAULT_VISIT");
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

        scope = "METHOD";

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
        boolean classField = false;

        if ((variable = currentMethod.getField(node.get("variable"))) == null) {
            variable = table.getField(node.get("variable"));
            classField = true;
        }
        String name = !classField ? currentMethod.isParameter(variable.getKey()) : null;

        String ollirVariable;
        String ollirType;

        StringBuilder ollir = new StringBuilder();

        ollirVariable = OllirTemplates.variable(variable.getKey(), name);
        ollirType = OllirTemplates.type(variable.getKey().getType());

        List<Object> visitResult;

        // ARRAY ACCESS
        if (node.getChildren().size() > 1) {
            String target = (String) visit(node.getChildren().get(0)).get(0);
            String[] parts = target.split("\n");
            if (parts.length > 1) {
                for (int i = 0; i < parts.length - 1; i++) {
                    ollir.append(parts[i]).append("\n");
                }
            }

            if (!classField) {
                ollirVariable = OllirTemplates.arrayaccess(variable.getKey(), name, parts[parts.length - 1]);
                ollirType = OllirTemplates.type(new Type(variable.getKey().getType().getName(), false));
            } else {
                String temp = "temporary" + temp_sequence++ + ollirType;
                ollir.append(String.format("%s :=%s %s;\n", temp, ollirType, parts[parts.length - 1]));

                ollirVariable = temp;
            }

            visitResult = visit(node.getChildren().get(1), Arrays.asList(classField ? "FIELD" : "ASSIGNMENT", variable.getKey()));
        } else {
            visitResult = visit(node.getChildren().get(0), Arrays.asList(classField ? "FIELD" : "ASSIGNMENT", variable.getKey()));
        }


        String result = (String) visitResult.get(0);
        String[] parts = result.split("\n");
        if (parts.length > 1) {
            for (int i = 0; i < parts.length - 1; i++) {
                ollir.append(parts[i]).append("\n");
            }
            if (!classField) {
                ollir.append(String.format("%s :=%s %s;", ollirVariable, ollirType, parts[parts.length - 1]));
            } else {
                if (visitResult.size() > 1 && (visitResult.get(1).equals("ARRAY_INIT") || visitResult.get(1).equals("OBJECT_INIT"))) {
                    String temp = "temporary" + temp_sequence++ + ollirType;
                    ollir.append(String.format("%s :=%s %s;\n", temp, ollirType, parts[parts.length - 1]));
                    ollir.append(OllirTemplates.putfield(ollirVariable, temp));
                } else {
                    ollir.append(OllirTemplates.putfield(ollirVariable, parts[parts.length - 1]));
                }
                ollir.append(";");
            }
        } else {
            if (!classField) {
                ollir.append(String.format("%s :=%s %s;", ollirVariable, ollirType, result));
            } else {
                if (visitResult.size() > 1 && (visitResult.get(1).equals("ARRAY_INIT") || visitResult.get(1).equals("OBJECT_INIT"))) {
                    String temp = "temporary" + temp_sequence++ + ollirType;
                    ollir.append(String.format("%s :=%s %s;\n", temp, ollirType, result));
                    ollir.append(OllirTemplates.putfield(ollirVariable, temp));
                } else {
                    ollir.append(OllirTemplates.putfield(ollirVariable, result));
                }
                ollir.append(";");
            }
        }

        if (visitResult.size() > 1 && visitResult.get(1).equals("OBJECT_INIT")) {
            ollir.append("\n").append(OllirTemplates.objectinstance(variable.getKey()));
        }

        return Collections.singletonList(ollir.toString());
    }

    private List<Object> dealWithPrimitive(JmmNode node, List<Object> data) {
        if (visited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        visited.add(node);

        String value;
        String type;

        switch (node.getKind()) {
            case "IntegerLiteral":
                value = node.get("value") + ".i32";
                type = ".i32";
                break;
            case "BooleanLiteral":
                value = (node.get("value").equals("true") ? "1" : "0") + ".bool";
                type = ".bool";
                break;
            default:
                value = "";
                type = "";
                break;
        }

        if (data.get(0).equals("RETURN")) {
            String temp = "temporary" + temp_sequence++ + type;
            value = String.format("%s :=%s %s;\n%s", temp, type, value, temp);
        }


        return Collections.singletonList(value);
    }

    private List<Object> dealWithBinaryOperation(JmmNode node, List<Object> data) {
        if (visited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        visited.add(node);

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

        if (data == null) {
            return Arrays.asList("DEFAULT_VISIT");
        }
        if (data.get(0).equals("RETURN") || data.get(0).equals("FIELD")) {
            Symbol variable = new Symbol(new Type("int", false), "temporary" + temp_sequence++);
            ollir.append(String.format("%s :=.i32 %s %s.i32 %s;\n", OllirTemplates.variable(variable), leftSide, node.get("operation"), rightSide));
            ollir.append(OllirTemplates.variable(variable));
        } else {
            ollir.append(String.format("%s %s.i32 %s", leftSide, node.get("operation"), rightSide));
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

            if (data.get(0).equals("CONDITION")) {
                return Arrays.asList(String.format("%s ==.bool 1.bool", OllirTemplates.variable(field.getKey(), name)), field.getKey(), name);
            } else {
                return Arrays.asList(OllirTemplates.variable(field.getKey(), name), field.getKey(), name);
            }
        }

        return Collections.singletonList("ACCESS");
    }

    private List<Object> dealWithReturn(JmmNode node, List<Object> data) {
        if (visited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        visited.add(node);

        StringBuilder ollir = new StringBuilder();

        List<Object> visit = visit(node.getChildren().get(0), Arrays.asList("RETURN"));

        String result = (String) visit.get(0);
        String[] parts = result.split("\n");
        if (parts.length > 1) {
            for (int i = 0; i < parts.length - 1; i++) {
                ollir.append(parts[i]).append("\n");
            }
            ollir.append(OllirTemplates.ret(currentMethod.getReturnType(), parts[parts.length - 1]));
        } else {
            ollir.append(OllirTemplates.ret(currentMethod.getReturnType(), result));
        }

        return Collections.singletonList(ollir.toString());
    }

    private List<Object> dealWithAndExpression(JmmNode node, List<Object> data) {
        if (visited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        visited.add(node);

        JmmNode left = node.getChildren().get(0);
        JmmNode right = node.getChildren().get(1);

        String leftReturn = (String) visit(left, Collections.singletonList("BINARY")).get(0);
        String rightReturn = (String) visit(right, Collections.singletonList("BINARY")).get(0);

        String[] leftStmts = leftReturn.split("\n");
        String[] rightStmts = rightReturn.split("\n");

        StringBuilder ollir = new StringBuilder();

        String leftSide;
        String rightSide;

        leftSide = binaryOperations(leftStmts, ollir, new Type("boolean", false));
        rightSide = binaryOperations(rightStmts, ollir, new Type("boolean", false));


        if (data.get(0).equals("RETURN")) {
            Symbol variable = new Symbol(new Type("boolean", false), "temporary" + temp_sequence++);
            ollir.append(String.format("%s :=.bool %s %s.i32 %s;\n", OllirTemplates.variable(variable), leftSide, node.get("operation"), rightSide));
            ollir.append(OllirTemplates.variable(variable));
        } else {
            ollir.append(String.format("%s %s.bool %s", leftSide, node.get("operation"), rightSide));
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

        if (parts.length > 1) {
            for (int i = 0; i < parts.length - 1; i++) {
                ollir.append(parts[i]).append("\n");
            }
        }

        String[] expressionParts;
        if ((expressionParts = last.split("<")).length == 2) {
            ollir.append(String.format("%s>=%s", expressionParts[0], expressionParts[1]));
        } else if ((expressionParts = last.split(">=")).length == 2) {
            ollir.append(String.format("%s<%s", expressionParts[0], expressionParts[1]));
        } else if (expression.equals("0.bool")) {
            ollir.append("1.bool");
        } else if (expression.equals("1.bool")) {
            ollir.append("0.bool");
        } else {
            ollir.append(String.format("%s :=.bool %s;\n", OllirTemplates.variable(new Symbol(new Type("boolean", false), "temporary" + temp_sequence)), last));
            ollir.append(String.format("%s !.bool %s",
                    OllirTemplates.variable(new Symbol(new Type("boolean", false), "temporary" + temp_sequence)),
                    OllirTemplates.variable(new Symbol(new Type("boolean", false), "temporary" + temp_sequence++))));
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

        ollir.append(String.format("if (%s) goto ifbody%d;\n", ifConditionParts[ifConditionParts.length - 1], if_label_sequence));

        ollir.append(visit(node.getParent().getChildren().get(1), Collections.singletonList("ELSE")).get(0));
        ollir.append("\n");
        ollir.append(String.format("goto endif;\n", if_label_sequence));

        ollir.append(String.format("ifbody%d:\n", if_label_sequence));
        List<String> ifBody = new ArrayList<>();
        for (int i = 1; i < node.getChildren().size(); i++) {
            ifBody.add((String) visit(node.getChildren().get(i), Collections.singletonList("IF")).get(0));
        }
        ollir.append(String.join("\n", ifBody)).append("\n");
        ollir.append(String.format("goto endif%d;\n", if_label_sequence));
        ollir.append(String.format("endif%d:\n", if_label_sequence));

        if_label_sequence++;

        return Collections.singletonList(ollir.toString());
    }

    private List<Object> dealWithElseStatement(JmmNode node, List<Object> data) {
        if (visited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        visited.add(node);

        StringBuilder ollir = new StringBuilder();

        ollir.append(String.format("else%d:\n", if_label_sequence));

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

    private List<Object> dealWithCondition(JmmNode node, List<Object> data) {
        if (visited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        visited.add(node);

        return visit(node.getChildren().get(0), Collections.singletonList("CONDITION"));
    }

    private List<Object> dealWithAccessExpression(JmmNode node, List<Object> data) {
        if (visited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        visited.add(node);

        JmmNode target = node.getChildren().get(0);
        JmmNode method = node.getChildren().get(1);

        StringBuilder ollir = new StringBuilder();

        List<Object> targetReturn = visit(target, Arrays.asList("ACCESS"));
        List<Object> methodReturn = visit(method, Arrays.asList("ACCESS", ollir));
        Symbol variable;
        boolean auxiliary = data.get(0).equals("BINARY") || data.get(0).equals("RETURN") || data.get(0).equals("CONDITION");

        Type returnType;


        if (targetReturn.get(0).equals("ACCESS")) {
            // Static Method or This Access
            if (target.get("name").equals("this")) {
                if (!auxiliary) {
                    if (methodReturn.get(0).equals("method")) {
                        Symbol assignment = null;
                        if (data.get(0).equals("FIELD") || data.get(0).equals("ASSIGNMENT")) {
                            assignment = (Symbol) data.get(1);
                        }

                        if (assignment != null) {
                            ollir.append(String.format("%s",
                                    OllirTemplates.invokevirtual(
                                            (String) methodReturn.get(1),
                                            assignment.getType(),
                                            (String) methodReturn.get(2))));
                            returnType = assignment.getType();
                        } else {
                            ollir.append(String.format("%s",
                                    OllirTemplates.invokevirtual(
                                            (String) methodReturn.get(1),
                                            new Type("void", false),
                                            (String) methodReturn.get(2))));

                            returnType = new Type("void", false);
                        }
                    }
                    else {
                        ollir.append(String.format("%s",
                                OllirTemplates.invokevirtual(
                                        ((JmmMethod) methodReturn.get(1)).getName(),
                                        ((JmmMethod) methodReturn.get(1)).getReturnType(),
                                        (String) methodReturn.get(2))));

                        returnType = ((JmmMethod) methodReturn.get(1)).getReturnType();
                    }
                } else {
                    variable = new Symbol(((JmmMethod) methodReturn.get(1)).getReturnType(), "temporary" + temp_sequence++);

                    ollir.append(String.format("%s :=%s %s;\n",
                            OllirTemplates.variable(variable),
                            OllirTemplates.type(variable.getType()),
                            OllirTemplates.invokevirtual(
                                    ((JmmMethod) methodReturn.get(1)).getName(),
                                    ((JmmMethod) methodReturn.get(1)).getReturnType(),
                                    (String) methodReturn.get(2))));

                    if (data.get(0).equals("CONDITION")) {
                        ollir.append(String.format("%s ==.bool 1.bool", OllirTemplates.variable(variable)));
                    } else {
                        ollir.append(OllirTemplates.variable(variable));
                    }
                    returnType = ((JmmMethod) methodReturn.get(1)).getReturnType();
                }
            } else {
                if (!auxiliary) {
                    Symbol assignment = null;
                    if (data.get(0).equals("FIELD") || data.get(0).equals("ASSIGNMENT")) {
                        assignment = (Symbol) data.get(1);
                    }

                    if (assignment != null) {
                        ollir.append(String.format("%s",
                                OllirTemplates.invokestatic(target.get("name"),
                                        (String) methodReturn.get(1),
                                        assignment.getType(),
                                        (String) methodReturn.get(2))));
                        returnType = assignment.getType();
                    } else {
                        ollir.append(String.format("%s",
                                OllirTemplates.invokestatic(target.get("name"),
                                        (String) methodReturn.get(1),
                                        new Type("void", false),
                                        (String) methodReturn.get(2))));
                        returnType = new Type("void", false);
                    }
                } else {
                    variable = new Symbol(((JmmMethod) methodReturn.get(1)).getReturnType(), "temporary" + temp_sequence++);

                    ollir.append(String.format("%s :=%s %s;\n",
                            OllirTemplates.variable(variable),
                            OllirTemplates.type(variable.getType()),
                            OllirTemplates.invokestatic(
                                    target.get("name"),
                                    (String) methodReturn.get(1),
                                    variable.getType(),
                                    (String) methodReturn.get(2))));
                    returnType = variable.getType();

                    if (data.get(0).equals("CONDITION")) {
                        ollir.append(String.format("%s ==.bool 1.bool", OllirTemplates.variable(variable)));
                    } else {
                        ollir.append(OllirTemplates.variable(variable));
                    }
                }
            }
        } else {
            // Instance or Class Method on a variable
            if (methodReturn.get(0).equals("length")) {
                if (auxiliary) {
                    variable = new Symbol(new Type("int", false), "temporary" + temp_sequence++);
                    ollir.append(String.format("%s :=.i32 %s;\n",
                            OllirTemplates.variable(variable),
                            OllirTemplates.arraylength((String) targetReturn.get(0))));

                    ollir.append(OllirTemplates.variable(variable));
                } else {
                    ollir.append(OllirTemplates.arraylength((String) targetReturn.get(0)));
                }
                returnType = new Type("int", false);
            } else if (!methodReturn.get(0).equals("method") && !methodReturn.get(0).equals("class_method")) {
                Symbol targetVariable = (Symbol) targetReturn.get(1);
                String[] parts = ((String) methodReturn.get(0)).split("\n");
                if (parts.length > 1) {
                    for (int i = 0; i < parts.length - 1; i++) {
                        ollir.append(parts[i]).append("\n");
                    }
                }
                if (auxiliary) {
                    variable = new Symbol(new Type("int", false), "temporary" + temp_sequence++);
                    ollir.append(String.format("%s :=.i32 %s;\n",
                            OllirTemplates.variable(variable),
                            OllirTemplates.arrayaccess(targetVariable,
                                    (String) targetReturn.get(2),
                                    parts[parts.length - 1]))
                    );
                    ollir.append(OllirTemplates.variable(variable));
                } else {
                    ollir.append(OllirTemplates.arrayaccess(targetVariable, (String) targetReturn.get(2), parts[parts.length - 1]));
                }
                returnType = new Type("int", false);
            } else {
                if (methodReturn.get(0).equals("method")) {
                    Symbol assignment = null;
                    if (targetReturn.get(0).equals("FIELD") || targetReturn.get(0).equals("ASSIGNMENT")) {
                        assignment = (Symbol) targetReturn.get(1);
                    }

                    if (!auxiliary) {
                        if (assignment != null) {
                            ollir.append(String.format("%s",
                                    OllirTemplates.invokevirtual(
                                            (String) methodReturn.get(1),
                                            assignment.getType(),
                                            (String) methodReturn.get(2))));
                            returnType = assignment.getType();
                        } else {
                            ollir.append(String.format("%s",
                                    OllirTemplates.invokevirtual(
                                            (String) methodReturn.get(1),
                                            new Type("void", false),
                                            (String) methodReturn.get(2))));

                            returnType = new Type("void", false);
                        }

                    } else {
                        if (assignment != null) {
                            variable = new Symbol(assignment.getType(), "temporary" + temp_sequence++);

                            ollir.append(String.format("%s :=%s %s;\n",
                                    OllirTemplates.variable(variable),
                                    OllirTemplates.type(variable.getType()),
                                    OllirTemplates.invokevirtual(
                                            (String) methodReturn.get(1),
                                            variable.getType(),
                                            (String) methodReturn.get(2))));
                            if (data.get(0).equals("CONDITION")) {
                                ollir.append(String.format("%s ==.bool 1.bool", OllirTemplates.variable(variable)));
                            } else {
                                ollir.append(OllirTemplates.variable(variable));
                            }
                            returnType = new Type("bool", false);
                        } else {
                            returnType = new Type("void", false);
                        }
                    }
                } else {
                    if (!auxiliary) {
                        ollir.append(String.format("%s",
                                OllirTemplates.invokevirtual(
                                        ((JmmMethod) methodReturn.get(1)).getName(),
                                        ((JmmMethod) methodReturn.get(1)).getReturnType(),
                                        (String) methodReturn.get(2))));
                    } else {
                        variable = new Symbol(((JmmMethod) methodReturn.get(1)).getReturnType(), "temporary" + temp_sequence++);

                        ollir.append(String.format("%s :=%s %s;\n",
                                OllirTemplates.variable(variable),
                                OllirTemplates.type(variable.getType()),
                                OllirTemplates.invokevirtual(
                                        ((JmmMethod) methodReturn.get(1)).getName(),
                                        ((JmmMethod) methodReturn.get(1)).getReturnType(),
                                        (String) methodReturn.get(2))));

                        if (data.get(0).equals("CONDITION")) {
                            ollir.append(String.format("%s ==.bool 1.bool", OllirTemplates.variable(variable)));
                        } else {
                            ollir.append(OllirTemplates.variable(variable));
                        }
                    }
                    returnType = ((JmmMethod) methodReturn.get(1)).getReturnType();
                }
            }
        }

        if (data.get(0).equals("METHOD") || data.get(0).equals("IF")) {
            ollir.append(";");
        }


        return Arrays.asList(ollir.toString(), returnType);
    }

    private List<Object> dealWithMethodCall(JmmNode node, List<Object> data) {
        if (visited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        visited.add(node);

        if (node.getKind().equals("Length")) {
            return Arrays.asList("length");
        }

        StringBuilder ollir = (StringBuilder) data.get(1);

        List<JmmNode> children = node.getChildren();
        Map.Entry<List<Type>, String> params = getParametersList(children, ollir);
        Type returnType = table.getReturnType(node.get("value"));

        try {
            JmmMethod method = table.getMethod(node.get("value"), params.getKey(), returnType);
            return Arrays.asList("class_method", method, params.getValue());
        } catch (NoSuchMethod noSuchMethod) {
            return Arrays.asList("method", node.get("value"), params.getValue());
        }
    }

    private Map.Entry<List<Type>, String> getParametersList(List<JmmNode> children, StringBuilder ollir) {
        List<Type> params = new ArrayList<>();

        List<String> paramsOllir = new ArrayList<>();

        for (JmmNode child : children) {
            Type type;
            String var;
            String[] statements;
            String result;
            switch (child.getKind()) {
                case "IntegerLiteral":
                    type = new Type("int", false);
                    paramsOllir.add(String.format("%s%s", child.get("value"), OllirTemplates.type(type)));
                    params.add(type);
                    break;
                case "BooleanLiteral":
                    type = new Type("boolean", false);
                    paramsOllir.add(String.format("%s%s", child.get("value"), OllirTemplates.type(type)));
                    params.add(type);
                    break;
                case "Variable":
                    List<Object> variable = visit(child, Arrays.asList("PARAM"));

                    params.add(((Symbol) variable.get(1)).getType());
                    paramsOllir.add((String) variable.get(0));
                    break;
                case "AccessExpression":
                    List<Object> accessExpression = visit(child, Arrays.asList("PARAM"));
                    statements = ((String) accessExpression.get(0)).split("\n");
                    if (statements.length > 1) {
                        for (int i = 0; i < statements.length - 1; i++) {
                            ollir.append(statements[i]).append("\n");
                        }
                    }
                    ollir.append(String.format("%s%s :=%s %s;\n",
                            "temporary" + temp_sequence,
                            OllirTemplates.type((Type) accessExpression.get(1)),
                            OllirTemplates.type((Type) accessExpression.get(1)),
                            statements[statements.length - 1]));

                    paramsOllir.add("temporary" + temp_sequence++ + OllirTemplates.type((Type) accessExpression.get(1)));

                    params.add((Type) accessExpression.get(1));
                    break;
                case "RelationalExpression":
                case "AndExpression":
                    var = (String) visit(child, Arrays.asList("PARAM")).get(0);
                    statements = var.split("\n");
                    result = binaryOperations(statements, ollir, new Type("boolean", false));
                    params.add(new Type("boolean", false));

                    paramsOllir.add(result);
                    break;
                case "BinaryOperation":
                    var = (String) visit(child, Arrays.asList("PARAM")).get(0);
                    statements = var.split("\n");
                    result = binaryOperations(statements, ollir, new Type("int", false));
                    params.add(new Type("int", false));

                    paramsOllir.add(result);
                    break;
                default:
                    break;
            }
        }
        return Map.entry(params, String.join(", ", paramsOllir));
    }

    private List<Object> dealWithArrayInit(JmmNode node, List<Object> data) {
        if (visited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        visited.add(node);

        StringBuilder ollir = new StringBuilder();

        String size = (String) visit(node.getChildren().get(0), Collections.singletonList("RETURN")).get(0);

        String[] sizeParts = size.split("\n");
        if (sizeParts.length > 1) {
            for (int i = 0; i < sizeParts.length - 1; i++) {
                ollir.append(sizeParts[i]).append("\n");
            }
        }

        ollir.append(OllirTemplates.arrayinit(sizeParts[sizeParts.length - 1]));

        return Arrays.asList(ollir.toString(), "ARRAY_INIT");
    }

    private List<Object> dealWithNewObject(JmmNode node, List<Object> data) {
        if (visited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        visited.add(node);

        return Arrays.asList(OllirTemplates.objectinit(node.get("value")), "OBJECT_INIT");
    }

    private List<Object> dealWithArrayAccess(JmmNode node, List<Object> data) {
        if (visited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        visited.add(node);

        String visit = (String) visit(node.getChildren().get(0), Arrays.asList("RETURN")).get(0);

        return Arrays.asList(visit);
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

        if (nodeResult.size() > 1) {
            List<Object> result = new ArrayList<>();
            result.add(nodeResult.get(0));
            result.addAll(nodeResult.subList(1, nodeResult.size()));

            return result;
        } else {
            return Arrays.asList(content.toString());
        }


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

                ollir.append(String.format("%s :=%s %s;\n", OllirTemplates.variable(new Symbol(type, String.format("temporary%d", temp_sequence))), OllirTemplates.assignmentType(m.group(1)), last));
                finalStmt = OllirTemplates.variable(new Symbol(type, String.format("temporary%d", temp_sequence++)));
            } else {
                finalStmt = last;
            }
        } else {
            if (statements[0].split("(/|\\+|-|\\*|&&|<|!|>=)(\\.i32|\\.bool)").length == 2) {
                Pattern p = Pattern.compile("(/|\\+|-|\\*|&&|<|!|>=)(\\.i32|\\.bool)");
                Matcher m = p.matcher(statements[0]);
                m.find();

                ollir.append(String.format("%s :=%s %s;\n", OllirTemplates.variable(new Symbol(type, String.format("temporary%d", temp_sequence))), OllirTemplates.assignmentType(m.group(1)), statements[0]));
                finalStmt = OllirTemplates.variable(new Symbol(type, String.format("temporary%d", temp_sequence++)));
            } else {
                finalStmt = statements[0];
            }
        }
        return finalStmt;
    }
}
