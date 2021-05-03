package ast;

import ast.exceptions.NoSuchMethod;
import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.*;

public class JmmSymbolTable implements SymbolTable {
    private final List<String> imports = new ArrayList<>();
    private String className;
    private String superClassName;
    // Map from Symbol to Value -> null if the field is not initialized yet
    private final Map<Symbol, Boolean> fields = new HashMap<>();
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
        fields.put(field, false);
    }

    public boolean fieldExists(String name) {
        for (Symbol field : this.fields.keySet()) {
            if (field.getName().equals(name))
                return true;
        }
        return false;
    }

    public JmmMethod getMethod(String name, List<Type> params, Type returnType) throws NoSuchMethod {
        for (JmmMethod method : methods) {
            if (method.getName().equals(name) && returnType.equals(method.getReturnType()) && params.size() == method.getParameters().size()) {
                if (JmmMethod.matchParameters(params, method.getParameterTypes())) {
                    return method;
                }
            }
        }

        throw new NoSuchMethod(name);
    }

    public Map.Entry<Symbol, Boolean> getField(String name) {
        for (Map.Entry<Symbol, Boolean> field : this.fields.entrySet()) {
            if (field.getKey().getName().equals(name))
                return field;
        }
        return null;
    }

    public boolean initializeField(Symbol symbol) {
        if (this.fields.containsKey(symbol)) {
            this.fields.put(symbol, true);
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
        for (Map.Entry<Symbol, Boolean> field : fields.entrySet())
            builder.append("\t").append(field.getKey()).append(" Initialized: ").append(field.getValue()).append("\n");

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
        for (JmmMethod method : methods){
            if(method.getName().equals(methodName)){
                return method.getReturnType();
            }
        }
        return null;
    }

    @Override
    public List<Symbol> getParameters(String methodName) {
        for (JmmMethod method : this.methods){
            if (method.getName().equals(methodName)){
                return method.getParameters();
            }
        }
        return null;
    }

    @Override
    public List<Symbol> getLocalVariables(String methodName) {
        return null;
    }
}
