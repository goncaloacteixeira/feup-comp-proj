package ast;

import ast.exceptions.NoSuchMethod;
import org.specs.comp.ollir.*;
import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.utilities.StringLines;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Input Data -> {scope, extra_data1, extra_data2, ...}
 * Output Data -> {OLLIR, extra_data1, extra_data2, ...}
 */
public class JasminGenerator {
    private ClassUnit classUnit;
    private String jasminCode;
    private final Set<JmmNode> visited = new HashSet<>();

    public JasminGenerator(ClassUnit classUnit) {
        this.classUnit = classUnit;
    }

    public String dealWithClass(){
        String stringBuilder = "";
        stringBuilder += ".class " + classUnit.getClassName() + "\n";
        stringBuilder += ".method public <init>()V\n" +
                "aload_0\n" +
                "invokenonvirtual java/lang/Object/<init>()V\n" +
                "return\n" +
                ".end method\n";
        return stringBuilder + dealWithMethods();
    }

    public String dealWithMethods() {
        //TODO limit stack
        //TODO limit locals
        String stringBuilder = "";
        for(Method method : classUnit.getMethods()){
            int localCount = 0;
            stringBuilder += ".method public ";
            if (method.isStaticMethod()) {
                stringBuilder += "static main([Ljava/lang/String;)V\n";
            }else{
                stringBuilder += method.getMethodName() + "(";

                stringBuilder += String.join(",", getConvertedParams(method.getParams()));

                stringBuilder += ")\n";
            }

            HashMap<String, Descriptor> varTable = OllirAccesser.getVarTable(method);

            for (Map.Entry<String, Descriptor> entry : varTable.entrySet()){
                if (entry.getValue().getScope().equals(VarScope.LOCAL))
                    localCount++;
            }

            stringBuilder += ".limit stack 100\n";
            stringBuilder += ".limit locals " + localCount + "\n";


            for (Instruction instruction : method.getInstructions()){
                stringBuilder += dealWithInstruction(instruction, varTable) + "\n";
            }

            switch (method.getReturnType().getTypeOfElement()){
                case INT32:
                case BOOLEAN:
                    stringBuilder += "ireturn";
                    break;
                case ARRAYREF:
                case OBJECTREF:
                    stringBuilder += "areturn";
                    break;
                case VOID:
                    stringBuilder += "return";
                    break;
            }

            stringBuilder += "\n.endmethod\n";

        }
        return stringBuilder;
    }

    public String dealWithInstruction(Instruction instruction, HashMap<String, Descriptor> varTable){
        if (instruction instanceof AssignInstruction){
            return dealWithAssignment((AssignInstruction) instruction, varTable);
        } else if (instruction instanceof SingleOpInstruction) {
            return dealWithSingleOpInstruction((SingleOpInstruction) instruction, varTable);
        } else {
            //TODO
            return "Lidate with rest of instructions";
        }
    }

    public String dealWithAssignment(AssignInstruction inst, HashMap<String, Descriptor> varTable){
        String stringBuilder = dealWithInstruction(inst.getRhs(), varTable);
        Operand op = (Operand) inst.getDest();
        return stringBuilder + "istore_" + varTable.get(op.getName()).getVirtualReg() + "\n";
    }

    public String dealWithSingleOpInstruction(SingleOpInstruction instruction, HashMap varTable) {
        LiteralElement operand = (LiteralElement) instruction.getSingleOperand();
        return "iconst_" + operand.getLiteral() + "\n";
    }

    public List<String> getConvertedParams(List<Element> params){
        List<String> convertedParams = new ArrayList<>();
        for(Element element : params){
            switch (element.getType().getTypeOfElement()){
                case INT32:
                    convertedParams.add("I");
                    break;
                case BOOLEAN:
                    convertedParams.add("Z");
                    break;
                case ARRAYREF:
                    convertedParams.add("[I");
                    break;
                case OBJECTREF:
                    convertedParams.add("OBJ");
                    break;
            }
        }
        return convertedParams;
    }
}
