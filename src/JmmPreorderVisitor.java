import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.specs.util.utilities.StringLines;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.stream.Collectors;

public class JmmPreorderVisitor extends PreorderJmmVisitor<String, String> {
    private final JmmSymbolTable table;
    private String scope;

    public JmmPreorderVisitor(JmmSymbolTable table) {
        super(JmmPreorderVisitor::reduce);
        this.table = table;

        addVisit("ImportDeclaration", this::dealWithImport);
        addVisit("ImportAux", this::dealWithImportAux);
        addVisit("ClassDeclaration", this::dealClassDeclaration);
        addVisit("ClassMethod", this::dealMethodDeclaration);
        addVisit("Param", this::dealParameter);

        addVisit("VarDeclaration", this::dealVarDeclaration);

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

    private String dealClassDeclaration(JmmNode node, String space) {
        table.setClassName(node.get("name"));
        try {
            table.setSuperClassName(node.get("extends"));
        } catch (NullPointerException e) {
            System.out.println("Does not extends");
        }

        scope = "CLASS";

        return space + "CLASS";
    }

    private String dealVarDeclaration(JmmNode node, String space) {
        switch (scope) {
            case "CLASS":
                table.addField(new Symbol(JmmSymbolTable.getType(node, "type"), node.get("identifier")));
                break;
            case "METHOD":
                Symbol field = new Symbol(JmmSymbolTable.getType(node, "type"), node.get("identifier"));
                table.getCurrentMethod().addLocalVariable(field);
                break;
            default:
                break;
        }

        return defaultVisit(node, space);
    }

    private String dealMethodDeclaration(JmmNode node, String space) {
        scope = "METHOD";
        table.addMethod(node.get("name"), JmmSymbolTable.getType(node, "return"));
        return space + "METHOD";
    }

    private String dealParameter(JmmNode node, String space) {
        Symbol field = new Symbol(JmmSymbolTable.getType(node, "type"), node.get("value"));
        table.getCurrentMethod().addParameter(field);

        return defaultVisit(node, space);
    }

    /**
     * Prints node information and appends space
     * @param node Node to be visited
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
