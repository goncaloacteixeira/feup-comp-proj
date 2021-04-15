package ast;

import ast.exceptions.WrongArgumentType;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.*;

public class JmmMethod {
    private String name;
    private Type returnType;
    private final List<Map.Entry<Symbol, String>> parameters = new ArrayList<>();

    // Map from Symbol to Value -> null if the field is not initialized yet
    private final Map<Symbol, String> localVariables = new HashMap<>();

    public JmmMethod(String name, Type returnType) {
        this.name = name;
        this.returnType = returnType;
    }

    public List<Type> getParameterTypes() {
        List<Type> params = new ArrayList<>();

        for (Map.Entry<Symbol, String> parameter : parameters) {
            params.add(parameter.getKey().getType());
        }
        return params;
    }


    public void addLocalVariable(Symbol variable) {
        localVariables.put(variable, null);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Type getReturnType() {
        return returnType;
    }

    public void setReturnType(Type returnType) {
        this.returnType = returnType;
    }

    public void addParameter(Symbol param) {
        this.parameters.add(Map.entry(param, "param"));
    }

    public boolean fieldExists(String field) {
        for (Symbol localVariable : this.localVariables.keySet()) {
            if (localVariable.getName().equals(field))
                return true;
        }
        return false;
    }

    public Map.Entry<Symbol, String> getField(String name) {
        for (Map.Entry<Symbol, String> field : this.localVariables.entrySet()) {
            if (field.getKey().getName().equals(name))
                return field;
        }

        for (Map.Entry<Symbol, String> param : this.parameters) {
            if (param.getKey().getName().equals(name))
                return param;
        }

        return null;
    }

    public List<Symbol> getParameters() {
        List<Symbol> params = new ArrayList<>();
        for (Map.Entry<Symbol, String> param : this.parameters) {
            params.add(param.getKey());
        }
        return params;
    }

    public List<Symbol> getLocalVariables() {
        return new ArrayList<>(this.localVariables.keySet());
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("ast.JmmMethod").append("\n");

        builder.append("Name: ").append(name).append(" | Return: ").append(returnType).append("\n");

        builder.append("Parameters").append("\n");
        for (Map.Entry<Symbol, String> param : this.parameters)
            builder.append("\t").append(param.getKey()).append("\n");

        builder.append("Local Variables").append("\n");
        for (Symbol localVariable : this.localVariables.keySet()) {
            builder.append("\t").append(localVariable).append("\n");
        }

        return builder.toString();
    }

    public static boolean matchParameters(List<Type> types1, List<Type> types2) {
        for (int i = 0; i < types1.size(); i++) {
            if (!types1.get(i).equals(types2.get(i))) {
                return false;
            }
        }
        return true;
    }

    public static List<Type> parseParameters(String params) {
        if (params.equals("")) return new ArrayList<>();

        String[] typesString = params.split(",");

        List<Type> types = new ArrayList<>();

        for (String s : typesString) {
            String[] aux = s.split(" ");
            types.add(new Type(aux[0], aux.length == 2));
        }

        return types;
    }
}
