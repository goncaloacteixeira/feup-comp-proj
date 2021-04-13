package pt.up.fe.comp.jmm.ast.examples;

import java.util.stream.Collectors;

import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;

public class ExampleVisitor extends AJmmVisitor<String, String> {
    private final String identifierAttribute;

    /**
     * Constructor of a Visitor
     * TODO Dunno what the 2 parameters are for..
     * @param identifierType        Kind of the Node
     * @param identifierAttribute   Name of the attribute
     */
    public ExampleVisitor(String identifierType, String identifierAttribute) {
        this.identifierAttribute = identifierAttribute;

        addVisit(identifierType, this::dealWithIdentifier); // Method reference
        setDefaultVisit(this::defaultVisit); // Method reference
    }

    /**
     * BiFunction< JmmNode, D, R > to be used with this.identifierType
     * @param node      Node that will be analysed
     * @param space     Information passed to the method
     * @return          Information returned from the method
     */
    public String dealWithIdentifier(JmmNode node, String space) {
        if (node.get(identifierAttribute).equals("this")) {
            return space + "THIS_ACCESS\n";
        }
        return defaultVisit(node, space);
    }

    /**
     * Default BiFunction< JmmNode, D, R > used to visit a Node. Visits every child of that node also
     * @param node      JMMNode to be visited
     * @param space     Inital data to be passed to the method
     * @return          String with the content of the visit
     */
    private String defaultVisit(JmmNode node, String space) {
        String content = space + node.getKind();
        String attrs = node.getAttributes()
                .stream()
                .filter(a -> !a.equals("line"))
                .map(a -> a + "=" + node.get(a))
                .collect(Collectors.joining(", ", "[", "]"));

        content += ((attrs.length() > 2) ? attrs : "") + "\n";
        for (JmmNode child : node.getChildren()) {
            content += visit(child, space + " ");
        }
        return content;
    }

}
