package ast;

import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.utilities.StringLines;

import java.util.Arrays;
import java.util.List;

public class OllirVisitor extends PreorderJmmVisitor<String, String> {
    private final JmmSymbolTable table;
    private JmmMethod currentMethod;
    private final List<Report> reports;
    private String scope;

    public OllirVisitor(JmmSymbolTable table, List<Report> reports) {
        super(OllirVisitor::reduce);

        this.table = table;
        this.reports = reports;

        addVisit("ClassDeclaration", this::dealWithClass);
        addVisit("MainMethod", this::dealWithMainDeclaration);
        addVisit("VarDeclaration", this::dealWithVarDeclaration);

        setDefaultVisit(this::defaultVisit);
    }

    private String dealWithClass(JmmNode node, String data) {
        scope = "CLASS";

        StringBuilder ollir = new StringBuilder(table.getClassName()).append(" {").append("\n");

        ollir.append(".construct ").append(table.getClassName()).append("().V {").append("\n");
        ollir.append("invokespecial(this, \"<init>\").V;").append("\n");
        ollir.append("}");

        for (JmmNode child : node.getChildren()) {
            String ollirChild = visit(child, "CLASS");
            if (ollirChild != null && !ollirChild.equals("DEFAULT_VISIT"))
                ollir.append("\n").append(ollirChild);
        }

        ollir.append("}");
        return ollir.toString();
    }

    private String dealWithMainDeclaration(JmmNode node, String data) {
        if (!scope.equals(data)) return "";
        scope = "METHOD";

        try {
            currentMethod = table.getMethod("main", Arrays.asList(new Type("String", true)), new Type("void", false));
        } catch (Exception e) {
            currentMethod = null;
            e.printStackTrace();
        }

        StringBuilder builder = new StringBuilder(".method public static main(args.array.String).V");
        builder.append("{\n");

        for (JmmNode child : node.getChildren()) {
            String ollirChild = visit(child, "METHOD");
            if (ollirChild != null && !ollirChild.equals("DEFAULT_VISIT"))
                if (ollirChild.equals("")) continue;
                builder.append(ollirChild).append("\n");
        }

        builder.append("}\n");

        return builder.toString();
    }

    private String dealWithVarDeclaration(JmmNode node, String data) {
        if (!scope.equals(data)) return "";
        return "sum.i32 :=.i32 0.i32;";
    }

    private String defaultVisit(JmmNode node, String data) {
        return "DEFAULT_VISIT";
    }

    private static String reduce(String nodeResult, List<String> childrenResults) {
        var content = new StringBuilder();

        if (!nodeResult.equals("DEFAULT_VISIT"))
            content.append(nodeResult);

        for (var childResult : childrenResults) {
            if (!childResult.equals("DEFAULT_VISIT"))
                content.append(String.join("\n", StringLines.getLines(childResult)));
        }

        return content.toString();
    }

}
