package ast;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;

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

    public static String getType(Type type) {
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

    public static String variable(Symbol variable) {
        StringBuilder param = new StringBuilder(variable.getName());

        param.append(getType(variable.getType()));

        return param.toString();
    }


}
