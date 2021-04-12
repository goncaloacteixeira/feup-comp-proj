import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.ArrayList;
import java.util.List;

public class JmmMethod {
    private String name;
    private Type returnType;
    private final List<Symbol> parameters = new ArrayList<>();
    private final List<Symbol> localVariables = new ArrayList<>();

    public JmmMethod(String name, Type returnType) {
        this.name = name;
        this.returnType = returnType;
    }

    public void addLocalVariable(Symbol variable) {
        localVariables.add(variable);
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

    public List<Symbol> getParameters() {
        return parameters;
    }

    public List<Symbol> getLocalVariables() {
        return localVariables;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("JmmMethod").append("\n");

        builder.append("Name: ").append(name).append(" | Return: ").append(returnType).append("\n");

        builder.append("Parameters").append("\n");
        for (Symbol param : this.parameters)
            builder.append("\t").append(param).append("\n");

        builder.append("Local Variables").append("\n");
        for (Symbol localVariable : this.localVariables) {
            builder.append("\t").append(localVariable).append("\n");
        }


        return builder.toString();
    }
}
