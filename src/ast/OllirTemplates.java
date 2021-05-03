package ast;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OllirTemplates {
    public static String classTemplate(String name, String extended) {
        if (extended == null) return classTemplate(name);

        StringBuilder ollir = new StringBuilder();
        ollir.append(String.format("%s extends %s", name, extended)).append(openBrackets());
        return ollir.toString();
    }

    public static String classTemplate(String name) {
        StringBuilder ollir = new StringBuilder();
        ollir.append(name).append(openBrackets());
        return ollir.toString();
    }

    public static String constructor(String name) {
        StringBuilder ollir = new StringBuilder();
        ollir.append(".construct ").append(name).append("().V").append(openBrackets());
        ollir.append("invokespecial(this, \"<init>\").V;");
        ollir.append(closeBrackets());
        return ollir.toString();
    }

    public static String method(String name, List<String> parameters, String returnType, boolean isStatic) {
        StringBuilder ollir = new StringBuilder(".method public ");

        if (isStatic) ollir.append("static ");

        // parameters
        ollir.append(name).append("(");
        ollir.append(String.join(", ", parameters));
        ollir.append(")");

        // return type
        ollir.append(returnType);

        ollir.append(openBrackets());

        return ollir.toString();
    }

    public static String method(String name, List<String> parameters, String returnType) {
        return method(name, parameters, returnType, false);
    }

    public static String openBrackets() {
        return " {\n";
    }

    public static String closeBrackets() {
        return "\n}";
    }

    public static String type(Type type) {
        StringBuilder ollir = new StringBuilder();

        if (type.isArray()) ollir.append(".array");

        if ("int".equals(type.getName())) {
            ollir.append(".i32");
        } else if ("void".equals(type.getName())) {
            ollir.append(".V");
        } else if ("boolean".equals(type.getName())) {
            ollir.append(".bool");
        } else {
            ollir.append(".").append(type.getName());
        }

        return ollir.toString();
    }

    public static String binary(String leftSide, String rightSide, String operation, Type operationType) {
        return String.format("%s %s%s %s", leftSide, operation, OllirTemplates.type(operationType), rightSide);
    }

    public static String variable(Symbol variable) {
        StringBuilder param = new StringBuilder(variable.getName());

        param.append(type(variable.getType()));

        return param.toString();
    }

    private static Symbol escapeVariable(Symbol variable) {
        if (variable.getName().charAt(0) == '$') {
            return new Symbol(variable.getType(), "dollar_" + variable.getName().substring(1));
        } else if (variable.getName().charAt(0) == '_') {
            return new Symbol(variable.getType(), "under_" + variable.getName().substring(1));
        } else if (variable.getName().equals("ret") || variable.getName().equals("array")) {
            return new Symbol(variable.getType(), "escaped_" + variable.getName());
        }
        return variable;
    }

    public static String variable(Symbol variable, String parameter) {
        variable = escapeVariable(variable);

        if (parameter == null) return variable(variable);
        return parameter + "." + variable(variable);
    }

    public static String arrayaccess(Symbol variable, String parameter, String index) {
        variable = escapeVariable(variable);

        if (parameter == null)
            return String.format("%s[%s]%s", variable.getName(), index, type(new Type(variable.getType().getName(), false)));
        return String.format("%s.%s[%s]%s", parameter, variable.getName(), index, type(new Type(variable.getType().getName(), false)));
    }

    public static String assignmentType(String operator) {
        switch (operator) {
            case "+":
            case "-":
            case "/":
            case "*":
                return ".i32";
            case "&&":
            case "<":
            case "!":
            case ">=":
                return ".bool";
            default:
                return ".error";
        }
    }

    public static String ret(Type ret, String exp) {
        return String.format("ret%s %s;", OllirTemplates.type(ret), exp);
    }

    public static String invokestatic(String target, String method, Type returnType, String parameters) {
        if (parameters.equals(""))
            return String.format("invokestatic(%s, \"%s\")%s", target, method, type(returnType));
        return String.format("invokestatic(%s, \"%s\", %s)%s", target, method, parameters, type(returnType));
    }

    public static String invokevirtual(String var, String method, Type returnType, String parameters) {
        if (parameters.equals(""))
            return String.format("invokevirtual(%s, \"%s\")%s", var != null ? var : "this", method, type(returnType));
        return String.format("invokevirtual(%s, \"%s\", %s)%s", var != null ? var : "this", method, parameters, type(returnType));
    }

    public static String invokevirtual(String method, Type returnType, String parameters) {
        return invokevirtual(null, method, returnType, parameters);
    }

    public static String invokespecial(String var, String method, Type returnType, String parameters) {
        if (parameters.equals(""))
            return String.format("invokespecial(%s, \"%s\")%s", var != null ? var : "this", method, type(returnType));
        return String.format("invokespecial(%s, \"%s\", %s)%s", var != null ? var : "this", method, parameters, type(returnType));
    }

    public static String invokespecial(String method, Type returnType, String parameters) {
        return invokespecial(null, method, returnType, parameters);
    }

    public static String arraylength(String variable) {
        return String.format("arraylength(%s).i32", variable);
    }

    public static String putfield(String variable, String value) {
        return String.format("putfield(this, %s, %s).V", variable, value);
    }

    public static String getfield(Symbol variable) {
        return String.format("getfield(this, %s)%s", variable(variable), type(variable.getType()));
    }

    public static String field(Symbol variable) {
        return String.format(".field public %s;", variable(variable));
    }

    public static String arrayinit(String size) {
        return String.format("new(array, %s).array.i32", size);
    }

    public static String objectinit(String objectClass) {
        return String.format("new(%s).%s", objectClass, objectClass);
    }

    public static String objectinstance(Symbol variable) {
        return String.format("invokespecial(%s,\"<init>\").V;", variable(variable));
    }
}
