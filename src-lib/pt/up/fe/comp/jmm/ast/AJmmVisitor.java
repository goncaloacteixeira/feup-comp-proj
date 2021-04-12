package pt.up.fe.comp.jmm.ast;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.specs.util.SpecsCheck;

public abstract class AJmmVisitor<D, R> implements JmmVisitor<D, R> {

    private final Map<String, BiFunction<JmmNode, D, R>> visitMap;
    private BiFunction<JmmNode, D, R> defaultVisit;

    public AJmmVisitor(Map<String, BiFunction<JmmNode, D, R>> visitMap, BiFunction<JmmNode, D, R> defaultVisit) {
        this.visitMap = visitMap;
        this.defaultVisit = defaultVisit;
    }

    public AJmmVisitor() {
        this(new HashMap<>(), null);
        setDefaultVisit(this::defaultVisit);
    }

    /**
     * Default visit does nothing, just returns null.
     * Because Visit is BiFunction< JmmNode, D, R >, this method needs to have parameters and a return
     * 
     * @param node      Not usable here
     * @param data      Not usable here
     * @return          always null
     */
    private R defaultVisit(JmmNode node, D data) {
        return null;
    }

    @Override
    public void addVisit(String kind, BiFunction<JmmNode, D, R> method) {
        this.visitMap.put(kind, method);
    }

    @Override
    public void setDefaultVisit(BiFunction<JmmNode, D, R> defaultVisit) {
        this.defaultVisit = defaultVisit;
    }

    // protected R visitDefault(JmmNode jmmNode, D data) {
    // if (defaultVisit == null) {
    // throw new RuntimeException("No default visitor is set, could not visit node " + jmmNode);
    // }
    //
    // return defaultVisit.apply(jmmNode, data);
    // }

    /**
     * 
     * @param kind  the kind of the node (e.g. MethodDeclaration, ClassDeclaration, etc.)
     * @return      the visit method to use, or default if no visit method was found
     */
    protected BiFunction<JmmNode, D, R> getVisit(String kind) {
        var visitMethod = visitMap.get(kind);

        if (visitMethod == null) {
            SpecsCheck.checkNotNull(defaultVisit,
                    () -> "No default visitor is set, could not visit node of kind " + kind);

            visitMethod = defaultVisit;
        }

        return visitMethod;
    }

    /**
     * Given a Node, gets the type of visit ({@code BiFunction<JmmNode, D, R>}) from its kind and calls it with apply() with the parameters
     * The JmmNode must not be null.
     * @param jmmNode   the node to be visited
     * @param data      Information to be passed to the BiFunction
     * @return          Return Data from the BiFunction
     */
    @Override
    public R visit(JmmNode jmmNode, D data) {
        SpecsCheck.checkNotNull(jmmNode, () -> "Node should not be null");

        return getVisit(jmmNode.getKind()).apply(jmmNode, data);
    }
}