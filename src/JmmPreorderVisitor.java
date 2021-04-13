import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.specs.util.utilities.StringLines;

import java.util.List;
import java.util.stream.Collectors;

public class JmmPreorderVisitor extends PreorderJmmVisitor<String, String> {
    private final JmmSymbolTable table;

    public JmmPreorderVisitor(JmmSymbolTable table) {
        super(JmmPreorderVisitor::reduce);
        this.table = table;

        addVisit("ImportDeclaration", this::dealWithImport);
        addVisit("ImportAux", this::dealWithImportAux);
        addVisit("ClassDeclaration", this::dealClassDeclaration);
        setDefaultVisit(this::defaultVisit);
    }

    private String dealWithImport(JmmNode node, String space) {
        table.addImport(node.get("value"));
        return defaultVisit(node, space);
    }

    private String dealWithImportAux(JmmNode node, String space) {
        String parent = node.getParent().get("value");
        table.updateImport(parent, node.get("value"));
        return defaultVisit(node, space);
    }

    private String dealClassDeclaration(JmmNode node, String space) {
        table.setClassName(node.get("name"));
        table.setSuperClassName(node.get("extends"));

        return defaultVisit(node, space);
    }


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
