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
        addVisit("ArrayInit", this::dealWithArrayInit);
        addVisit("ArrayAccess", this::dealWithArrayAccess);
        addVisit("Variable", this::dealWithVariable);
        addVisit("Assignment", this::dealWithAssignment);

        // keep track of scopes, TODO - mais scopes, como acessos ou invocação de métodos
        addVisit("ClassDeclaration", this::dealWithClassDeclaration);
        addVisit("MainMethod", this::dealWithMainDeclaration);
        addVisit("ClassMethod", this::dealWithMethodDeclaration);
        addVisit("AccessExpression", this::dealWithAccessExpression);
        addVisit("MethodCall", this::dealWithMethodCall);
        addVisit("Length", this::dealWithMethodCall);
    }


    private Map.Entry<String, String> dealWithArrayAccess(JmmNode node, Map.Entry<String, String> data) {
        JmmNode index = node.getChildren().get(0);
        Map.Entry<String, String> indexReturn = visit(index);

        if (!indexReturn.getKey().equals("int")) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(index.get("line")), Integer.parseInt(index.get("col")), "Array access index is not an Integer: " + index));
            return Map.entry("error", "null");
        }

        return Map.entry("index", indexReturn.getValue());
    }

    private Map.Entry<String, String> dealWithArrayInit(JmmNode node, Map.Entry<String, String> data) {
        JmmNode size = node.getChildren().get(0);
        Map.Entry<String, String> sizeReturn = visit(size);

        if (!sizeReturn.getKey().equals("int")) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(size.get("line")), Integer.parseInt(size.get("col")), "Array init size is not an Integer: " + size));
            return Map.entry("error", "null");
        }

        int sizeValue = Integer.parseInt(sizeReturn.getValue());
        StringBuffer array = new StringBuffer(sizeValue);
        array.append(sizeReturn.getValue());
        for (int i = 0; i < sizeValue; i++){
            array.append(",0");
        }

        return Map.entry("int []", array.toString());
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
            if (field == null) {
                field = table.getField(node.get("name"));
            }
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
            return Map.entry(field.getKey().getType().getName() + (field.getKey().getType().isArray() ? " []" : ""), field.getValue() != null ? field.getValue() : "null");
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
        }else if (targetReturn.getKey().equals("method")){
            if (methodReturn != null && methodReturn.getKey().equals("error") && table.getSuper() == null){
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "No such method: " + method.get("value")));
            } else {
                return Map.entry("int", "1");
            }
        } else if (targetReturn.getKey().equals("int") || targetReturn.getKey().equals("boolean")){
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Target cannot be primitive: " + target));
        } else if (targetReturn.getKey().equals("int []") && methodReturn.getKey().equals("index")) {
            String value = getArrayValue(targetReturn.getValue(), Integer.parseInt(methodReturn.getValue()));
            if (value == null) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Array index out of bounds: " + target));
            } else {
                return Map.entry("int", value);
            }
        } else if (targetReturn.getKey().equals("int []") && methodReturn.getKey().equals("length")) {
            return Map.entry("int", targetReturn.getValue().split(",",2)[0]);
        }

        // TODO - verificar valor do resultado, e tipo de valor (int, boolean, int[])
        return Map.entry("error", "null");
    }

    private String getArrayValue(String array, int index) {
        String[] parts = array.split(",");

        if (Integer.parseInt(parts[0]) <= index) {
            return null;
        }
        return parts[index + 1];
    }

    private String updateArray(String array, int index, String new_value) {
        String[] parts = array.split(",");

        if (Integer.parseInt(parts[0]) <= index) {
            return null;
        }

        parts[index + 1] = new_value;
        return String.join(",", parts);
    }

    private Map.Entry<String, String> dealWithMethodCall(JmmNode node, Map.Entry<String, String> space) {
        // TODO - verificação do length
        if (node.getKind().equals("Length")) {
            return Map.entry("length", "null");
        }

        List<JmmNode> children = node.getChildren();
        List<Type> params = getParametersList(children);
        Type returnType = table.getReturnType(node.get("value"));

        try {
            table.getMethod(node.get("value"), params, returnType);
        } catch (NoSuchMethod noSuchMethod) {
            return Map.entry("error", "noSuchMethod");
        }

        // TODO - Fazer este visitor retornar para cima o resultado da chamada
        return Map.entry("int", "1");
    }

    private List<Type> getParametersList(List<JmmNode> children){
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
                case "AccessExpression":
                case "BinaryOperation":
                    Map.Entry<String,String> var = visit(child);
                    String[] type = var.getKey().split(" ");
                    params.add(new Type(type[0], type.length == 2));
                    break;
                default:
                    break;
            }
        }
        return params;
    }

    private Map.Entry<String, String> dealWithAssignment(JmmNode node, Map.Entry<String, String> space) {
        List<JmmNode> children = node.getChildren();

        if (children.size() == 1) {
            Map.Entry<String, String> assignment = visit(node.getChildren().get(0));

            if (!currentMethod.updateField(node.get("variable"), assignment.getValue())) {
                table.updateField(node.get("variable"), assignment.getValue());
            }
        } else {
            Map.Entry<String, String> index = visit(node.getChildren().get(0));
            Map.Entry<String, String> value = visit(node.getChildren().get(1));

            Map.Entry<Symbol, String> array;
            if ((array = currentMethod.getField(node.get("variable"))) == null) {
                array = table.getField(node.get("variable"));
            }

            String updated_array = this.updateArray(array.getValue(), Integer.parseInt(index.getValue()), value.getValue());
            if (updated_array == null) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Array index out of bounds: " + node.get("variable") + "[" + index.getValue() + "]"));
                return null;
            }

            if (!currentMethod.updateField(node.get("variable"), updated_array)) {
                table.updateField(node.get("variable"), updated_array);
            }
        }

        return null;
    }
}
