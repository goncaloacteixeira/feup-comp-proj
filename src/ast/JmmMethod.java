package ast;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JmmMethod {
    private String name;
    private Type returnType;
    private final List<Symbol> parameters = new ArrayList<>();
    private final Map<Symbol, String> localVariables = new HashMap<>();

    public JmmMethod(String name, Type returnType) {
        this.name = name;
        this.returnType = returnType;
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
        this.parameters.add(param);
    }

    public boolean fieldExists(String field) {
        for (Symbol localVariable : this.localVariables.keySet()) {
            if (localVariable.getName().equals(field))
                return true;
        }
        return false;
    }

    public List<Symbol> getParameters() {
        return parameters;
    }

    public List<Symbol> getLocalVariables() {
        return new ArrayList<>(this.localVariables.keySet());
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("ast.JmmMethod").append("\n");

        builder.append("Name: ").append(name).append(" | Return: ").append(returnType).append("\n");

        builder.append("Parameters").append("\n");
        for (Symbol param : this.parameters)
            builder.append("\t").append(param).append("\n");

        builder.append("Local Variables").append("\n");
        for (Symbol localVariable : this.localVariables.keySet()) {
            builder.append("\t").append(localVariable).append("\n");
        }


        return builder.toString();
    }
}
