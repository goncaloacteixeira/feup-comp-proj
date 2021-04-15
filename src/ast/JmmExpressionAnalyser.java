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
public class JmmExpressionAnalyser extends PreorderJmmVisitor<Map.Entry<String, String>, Map.Entry<String, String>> {
    private final JmmSymbolTable table;
    private final List<Report> reports;
    private String scope;
    private JmmMethod currentMethod;

    public JmmExpressionAnalyser(JmmSymbolTable table, List<Report> reports) {
        this.table = table;
        this.reports = reports;

        // DATA -> <return type, result (expression eval)>

        // TODO - Fazer os assignments às variáveis neste visitor uma vez que é possivel fazer isso com preorder

        addVisit("BinaryOperation", this::dealWithBinaryOperation);
        addVisit("IntegerLiteral", this::dealWithPrimitive);
        addVisit("BooleanLiteral", this::dealWithPrimitive);
        addVisit("Variable", this::dealWithVariable);
        addVisit("Assignment", this::dealWithAssignment);

        // keep track of scopes, TODO - mais scopes, como acessos ou invocação de métodos
        addVisit("ClassDeclaration", this::dealWithClassDeclaration);
        addVisit("MainMethod", this::dealWithMainDeclaration);
        addVisit("ClassMethod", this::dealWithMethodDeclaration);
        addVisit("AccessExpression", this::dealWithAccessExpression);
        addVisit("MethodCall", this::dealWithMethodCall);
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

    private Map.Entry<String, String> dealWithBinaryOperation(JmmNode node, Map.Entry<String, String> data) {
        JmmNode left = node.getChildren().get(0);
        JmmNode right = node.getChildren().get(1);

        Map.Entry<String, String> leftReturn = visit(left);
        Map.Entry<String, String> rightReturn = visit(right);

        Map.Entry<String, String> dataReturn = Map.entry("int", "null");

        if (!leftReturn.getKey().equals("int")) {
            dataReturn = Map.entry("error", "null");
            // para não apresentar o mesmo erro mais do que uma vez
            if (leftReturn.getKey().equals("error"))
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(left.get("line")), Integer.parseInt(left.get("col")), "Left Member not integer"));
        }
        if (!rightReturn.getKey().equals("int")) {
            dataReturn = Map.entry("error", "null");
            // para não apresentar o mesmo erro mais do que uma vez
            if (rightReturn.getKey().equals("error"))
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(right.get("line")), Integer.parseInt(right.get("col")), "Right Member not integer"));
        }


        if (dataReturn.getKey().equals("int")) {
            try {
                String result = expressionEval(leftReturn.getValue(), rightReturn.getValue(), node.get("operation"));
                dataReturn = Map.entry(dataReturn.getKey(), result);
            } catch (DivisionByZero divisionByZero) {
                dataReturn = Map.entry("error", "null");
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Division by Zero"));
            } catch (UnsupportedOperation unsupportedOperation) {
                dataReturn = Map.entry("error", "null");
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Operation not supported"));
            }
        }

        System.out.println(node + " result: " + dataReturn.getValue());
        return dataReturn;
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


    private Map.Entry<String, String> dealWithPrimitive(JmmNode node, Map.Entry<String, String> data) {
        String return_type;
        String result;

        switch (node.getKind()) {
            case "IntegerLiteral":
                return_type = "int";
                result = node.get("value");
                break;
            case "BooleanLiteral":
                return_type = "boolean";
                result = node.get("value");
                break;
            default:
                return_type = "error";
                result = "null";
                break;
        }

        return Map.entry(return_type, result);
    }

    private Map.Entry<String, String> dealWithVariable(JmmNode node, Map.Entry<String, String> data) {
        Map.Entry<Symbol, String> field = null;

        if (scope.equals("CLASS")) {
            field = table.getField(node.get("name"));
        } else if (scope.equals("METHOD") && currentMethod != null) {
            field = currentMethod.getField(node.get("name"));
        }

        //TODO - ver se a variavel foi inicializada
        //LocalVariables dao coco quando temos funções overloaded

        //ver se a variavel se refere a um acesso, ou a um metodo
        if (field == null && table.getImports().contains(node.get("name"))){
            return Map.entry("access", "null");
        }else if (field == null && node.get("name").equals("this")){
            return Map.entry("method", "null");
        }

        if (field == null) {
            return Map.entry("error", "null");
        } else {
            return Map.entry(field.getKey().getType().getName() + (field.getKey().getType().isArray() ? "[]" : ""), "1");
        }
    }


    private Map.Entry<String, String> dealWithClassDeclaration(JmmNode node, Map.Entry<String, String> data) {
        scope = "CLASS";
        return null;
    }


    private Map.Entry<String, String> dealWithMethodDeclaration(JmmNode node, Map.Entry<String, String> data) {
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

    private Map.Entry<String, String> dealWithMainDeclaration(JmmNode node, Map.Entry<String, String> data) {
        scope = "METHOD";

        try {
            currentMethod = table.getMethod("main", Arrays.asList(new Type("String", true)), new Type("void", false));
        } catch (Exception e) {
            currentMethod = null;
            e.printStackTrace();
        }

        return null;
    }

    private Map.Entry<String, String> dealWithAccessExpression(JmmNode node, Map.Entry<String, String> space){
        JmmNode target = node.getChildren().get(0);
        JmmNode method = node.getChildren().get(1);

        Map.Entry<String, String> targetReturn = visit(target);
        Map.Entry<String, String> methodReturn = visit(method);

        if (targetReturn.getKey().equals("error")){
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Unknown target: " + target.get("name")));
            return null;

        }else if (targetReturn.getKey().equals("method")){

            if (methodReturn != null && methodReturn.getKey().equals("error") && table.getSuper() == null){
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "No such method: " + method.get("value")));
            }

        } else if (!targetReturn.getKey().equals("access")){
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Target cannot be primitive: " + target.get("value")));
        }

        return null;
    }

    private Map.Entry<String, String> dealWithMethodCall(JmmNode node, Map.Entry<String, String> space) {
        List<JmmNode> children = node.getChildren();
        List<Type> params = getParametersList(children);
        Type returnType = table.getReturnType(node.get("value"));

        try {
            table.getMethod(node.get("value"), params, returnType);
        } catch (NoSuchMethod noSuchMethod) {
            return Map.entry("error", "noSuchMethod");
        }
        return null;
    }

    public List<Type> getParametersList(List<JmmNode> children){
        //TODO - visitor para os parametros em vez disto maybe
        List<Type> params = new ArrayList<>();
        for(JmmNode child : children){
            switch (child.getKind()){
                case "IntegerLiteral":
                    params.add(new Type("int", false));
                    break;
                case "BooleanLiteral":
                    params.add(new Type("boolean", false));
                    break;
                case "Variable":
                    Map.Entry<String,String> var = visit(child);
                    params.add(new Type(var.getKey(), Boolean.parseBoolean(var.getValue())));
                    break;
                default:
                    break;
            }
        }
        return params;
    }

    private Map.Entry<String, String> dealWithAssignment(JmmNode node, Map.Entry<String, String> space) {
        return null;
    }

}
