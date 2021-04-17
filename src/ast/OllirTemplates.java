package ast;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.Arrays;
import java.util.List;

public class OllirTemplates {
    public static String constructor(String name) {
        StringBuilder ollir = new StringBuilder();

        ollir.append(name).append(openBrackets());
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
        }
        else if ("boolean".equals(type.getName())) {
            ollir.append(".bool");
        }
        else {
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

    public static String variable(Symbol variable, String parameter) {
        if (parameter == null) return variable(variable);
        return parameter + "." + variable(variable);
    }

    public static String ifHeader(String condition) {
        String[] parts;
        if ((parts = condition.split("<")).length == 2) {
            return String.format("if (%s >=%s) goto else;\n", parts[0], parts[1]);
        }

        return String.format("if (%s) goto else;\n", condition);
    }

    public static String ret(Type ret, String exp) {
        return String.format("ret%s %s;", OllirTemplates.type(ret), exp);
    }


}
