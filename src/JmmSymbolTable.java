import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JmmSymbolTable implements SymbolTable {
    private final List<String> imports = new ArrayList<>();
    private String className;
    private String superClassName;
    private final List<Symbol> fields = new ArrayList<>();
    private final Map<String, JmmMethod> methods = new HashMap<>();

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
        fields.add(field);
    }

    public void addMethod(String name, Type returnType) {
        methods.put(name, new JmmMethod(name, returnType));
    }

    public void updateImport(String prefix, String suffix) {
        for (int i = 0 ; i < imports.size() ; i++) {
            if (imports.get(i).equals(prefix)) {
                imports.set(i, imports.get(i) + "." + suffix);
            }
        }
    }

    @Override
    public String toString() {
        return "JmmSymbolTable{" +
                "imports=" + imports +
                ", className='" + className + '\'' +
                ", superClassName='" + superClassName + '\'' +
                ", fields=" + fields +
                ", methods=" + methods +
                '}';
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
        return fields;
    }

    @Override
    public List<String> getMethods() {
        return new ArrayList<>(methods.keySet());
    }

    @Override
    public Type getReturnType(String methodName) {
        return methods.get(methodName).getReturnType();
    }

    @Override
    public List<Symbol> getParameters(String methodName) {
        return methods.get(methodName).getParameters();
    }

    @Override
    public List<Symbol> getLocalVariables(String methodName) {
        return methods.get(methodName).getLocalVariables();
    }
}
