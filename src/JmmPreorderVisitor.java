import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.specs.util.utilities.StringLines;

import java.util.List;
import java.util.stream.Collectors;

public class JmmPreorderVisitor extends PreorderJmmVisitor<String, String> {
    private final JmmSymbolTable table;
    private String scope;
    private final List<Report> reports;

    public JmmPreorderVisitor(JmmSymbolTable table, List<Report> reports) {
        super(JmmPreorderVisitor::reduce);
        this.table = table;
        this.reports = reports;

        addVisit("ImportDeclaration", this::dealWithImport);
        addVisit("ImportAux", this::dealWithImportAux);
        addVisit("ClassDeclaration", this::dealWithClassDeclaration);
        addVisit("MainMethod", this::dealWithMainDeclaration);
        addVisit("ClassMethod", this::dealWithMethodDeclaration);
        addVisit("Param", this::dealWithParameter);
        addVisit("VarDeclaration", this::dealWithVarDeclaration);

        setDefaultVisit(this::defaultVisit);
    }

    private String dealWithImport(JmmNode node, String space) {
        table.addImport(node.get("value"));
        return space + "IMPORT";
    }

    private String dealWithImportAux(JmmNode node, String space) {
        List<String> imports = table.getImports();
        String lastImport = imports.get(imports.size() - 1);
        String newImport = lastImport + '.' + node.get("value");
        imports.set(imports.size() - 1, newImport);

        return space + "IMPORT_AUX";
    }

    private String dealWithClassDeclaration(JmmNode node, String space) {
        table.setClassName(node.get("name"));
        try {
            table.setSuperClassName(node.get("extends"));
        } catch (NullPointerException ignored) {

        }

        scope = "CLASS";
        return space + "CLASS";
    }

    private String dealWithVarDeclaration(JmmNode node, String space) {
        Symbol field = new Symbol(JmmSymbolTable.getType(node, "type"), node.get("identifier"));

        if (scope.equals("CLASS")) {
            if (table.fieldExists(field.getName())) {
                this.reports.add(new Report(
                        ReportType.ERROR, Stage.SEMANTIC,
                        Integer.parseInt(node.get("line")),
                        Integer.parseInt(node.get("col")),
                        "Variable already declared: " + field.getName()));
                return space + "ERROR";
            }
            table.addField(field);
        } else {
            if (table.getCurrentMethod().fieldExists(field.getName())) {
                this.reports.add(new Report(
                        ReportType.ERROR,
                        Stage.SEMANTIC,
                        Integer.parseInt(node.get("line")),
                        Integer.parseInt(node.get("col")),
                        "Variable already declared: " + field.getName()));
                return space + "ERROR";
            }
            table.getCurrentMethod().addLocalVariable(field);
        }

        return space + "VARDECLARATION";
    }

    private String dealWithMethodDeclaration(JmmNode node, String space) {
        scope = "METHOD";
        table.addMethod(node.get("name"), JmmSymbolTable.getType(node, "return"));
        return space + "METHOD";
    }

    private String dealWithParameter(JmmNode node, String space) {
        if (scope.equals("METHOD")) {
            Symbol field = new Symbol(JmmSymbolTable.getType(node, "type"), node.get("value"));
            table.getCurrentMethod().addParameter(field);
        } else if (scope.equals("MAIN")) {
            Symbol field = new Symbol(new Type("String", true), node.get("value"));
            table.getCurrentMethod().addParameter(field);
        }

        return space + "PARAM";
    }

    private String dealWithMainDeclaration(JmmNode node, String space) {
        scope = "MAIN";

        table.addMethod("main", new Type("void", false));

        return space + "MAIN";
    }

    /**
     * Prints node information and appends space
     *
     * @param node  Node to be visited
     * @param space Info passed down from other nodes
     * @return New info to be returned
     */
    private String defaultVisit(JmmNode node, String space) {
        String content = space + node.getKind();
        String attrs = node.getAttributes()
                .stream()
                .filter(a -> !a.equals("line"))
                .map(a -> a + "=" + node.get(a))
                .collect(Collectors.joining(", ", "[", "]"));

        content += ((attrs.length() > 2) ? attrs : "");

        return content;
    }

    private static String reduce(String nodeResult, List<String> childrenResults) {
        var content = new StringBuilder();

        content.append(nodeResult).append("\n");

        for (var childResult : childrenResults) {
            var childContent = StringLines.getLines(childResult).stream()
                    .map(line -> " " + line + "\n")
                    .collect(Collectors.joining());

            content.append(childContent);
        }

        return content.toString();
    }
}
