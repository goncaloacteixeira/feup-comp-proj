package ast;

import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.*;

public class JmmSymbolTable implements SymbolTable {
    private final List<String> imports = new ArrayList<>();
    private String className;
    private String superClassName;
    private final Map<Symbol, String> fields = new HashMap<>();
    private final List<JmmMethod> methods = new ArrayList<>();
    private JmmMethod currentMethod;

    public static Type getType(JmmNode node, String attribute) {
        Type type;
        if (node.get(attribute).equals("int[]"))
            type = new Type("int", true);
        else if (node.get(attribute).equals("int"))
            type = new Type("int", false);
        else
            type = new Type(node.get(attribute), false);

        return type;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public void setSuperClassName(String superClassName) {
        this.superClassName = superClassName;
    }

    public void addImport(String importStatement) {
        imports.add(importStatement);
    }

    public void addField(Symbol field) {
        fields.put(field, null);
    }

    public boolean fieldExists(String name) {
        for (Symbol field : this.fields.keySet()) {
            if (field.getName().equals(name))
                return true;
        }
        return false;
    }


    public void addMethod(String name, Type returnType) {
        currentMethod = new JmmMethod(name, returnType);
        methods.add(currentMethod);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("SYMBOL TABLE\n");
        builder.append("Imports").append("\n");
        for (String importStmt : imports)
            builder.append("\t").append(importStmt).append("\n");

        builder.append("Class Name: ").append(className).append(" | Extends: ").append(superClassName).append("\n");

        builder.append("--- Local Variables ---").append("\n");
        for (Symbol field : fields.keySet())
            builder.append("\t").append(field).append("\n");

        builder.append("--- Methods ---").append("\n");
        for (JmmMethod method : this.methods) {
            builder.append(method);
            builder.append("---------").append("\n");
        }

        return builder.toString();
    }

    @Override
    public List<String> getImports() {
        return imports;
    }

    @Override
    public String getClassName() { return className; }

    @Override
    public String getSuper() {
        return superClassName;
    }

    @Override
    public List<Symbol> getFields() {
        return new ArrayList<>(this.fields.keySet());
    }

    @Override
    public List<String> getMethods() {
        List<String> methods = new ArrayList<>();
        for (JmmMethod method : this.methods) {
            methods.add(method.getName());
        }

        return methods;
    }

    public JmmMethod getCurrentMethod() {
        return currentMethod;
    }

    @Override
    public Type getReturnType(String methodName) {
        return null;
    }

    @Override
    public List<Symbol> getParameters(String methodName) {
        return null;
    }

    @Override
    public List<Symbol> getLocalVariables(String methodName) {
        return null;
    }
}
