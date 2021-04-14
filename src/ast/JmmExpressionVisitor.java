package ast;

import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;

import java.util.List;
import java.util.Stack;

public class JmmExpressionVisitor extends PreorderJmmVisitor<Void, Void> {
    private final JmmSymbolTable table;
    private final Stack<String> scope;

    public JmmExpressionVisitor(JmmSymbolTable table) {
        this.table = table;
        this.scope = new Stack<>();

        addVisit("RelationalExpression", this::addBooleanReturn);
        addVisit("AndExpression", this::addBooleanReturn);
        addVisit("NotExpression", this::addBooleanReturn);
        addVisit("BooleanLiteral", this::addBooleanReturn);

        addVisit("AdditiveExpression", this::addIntReturn);
        addVisit("MultiplicativeExpression", this::addIntReturn);
        addVisit("IntegerLiteral", this::addIntReturn);
        addVisit("ArrayAccess", this::addIntReturn);
        addVisit("Length", this::addIntReturn);

        addVisit("NewIntArray", this::addObjReturn);
        addVisit("NewObject", this::addObjReturn);


        addVisit("Variable", this::addVarReturn);

    }

    private Void addBooleanReturn(JmmNode node, Void space) {
        node.put("return", "boolean");
        return null;
    }

    private Void addIntReturn(JmmNode node, Void space) {
        node.put("return", "int");
        return null;
    }

    private Void addObjReturn(JmmNode node, Void space) {
        node.put("return", "object");
        return null;
    }

    private Void addVarReturn(JmmNode node, Void space) {
        node.put("return", "object");
        return null;
    }
}
