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

    public String dealWithClass() {
        String stringBuilder = ".class " + classUnit.getClassName() + "\n.super java/lang/Object\n";
        return stringBuilder + dealWithMethods();
    }

    public String dealWithMethods() {
        //TODO limit stack
        //TODO limit locals
        String stringBuilder = "";
        for (Method method : classUnit.getMethods()) {
            int localCount = 0;
            stringBuilder += "\n.method public ";
            if (method.isConstructMethod()) {
                stringBuilder += "<init>()V\naload_0\ninvokenonvirtual java/lang/Object/<init>()V\nreturn\n.end method\n";
                continue;
            }
            if (method.isStaticMethod()) {
                stringBuilder += "static main([Ljava/lang/String;)V\n";
            }
            else {
                stringBuilder += method.getMethodName() + "(";

                stringBuilder += String.join(",", getConvertedParams(method.getParams()));

                stringBuilder += ")\n";
            }

            HashMap<String, Descriptor> varTable = OllirAccesser.getVarTable(method);

            for (Map.Entry<String, Descriptor> entry : varTable.entrySet()) {
                if (entry.getValue().getScope().equals(VarScope.LOCAL))
                    localCount++;
            }

            stringBuilder += ".limit stack 100\n";
            stringBuilder += ".limit locals " + localCount + "\n";


            for (Instruction instruction : method.getInstructions()) {
                stringBuilder += dealWithInstruction(instruction, varTable);
            }

            switch (method.getReturnType().getTypeOfElement()) {
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

            stringBuilder += "\n.end method\n";

        }
        return stringBuilder;
    }

    public String dealWithInstruction(Instruction instruction, HashMap<String, Descriptor> varTable) {
        if (instruction instanceof AssignInstruction) {
            return dealWithAssignment((AssignInstruction) instruction, varTable);
        }
        else if (instruction instanceof SingleOpInstruction) {
            return dealWithSingleOpInstruction((SingleOpInstruction) instruction, varTable);
        }
        else if (instruction instanceof BinaryOpInstruction) {
            return dealWithBinaryOpInstruction((BinaryOpInstruction) instruction, varTable);
        }
        else {
            //TODO
            return "Deu coco nas Instructions";
        }
    }

    public String dealWithAssignment(AssignInstruction inst, HashMap<String, Descriptor> varTable) {
        String stringBuilder = dealWithInstruction(inst.getRhs(), varTable);
        Operand op = (Operand) inst.getDest();
        return stringBuilder + "istore_" + varTable.get(op.getName()).getVirtualReg() + "\n";
    }

    public String dealWithSingleOpInstruction(SingleOpInstruction instruction, HashMap<String, Descriptor> varTable) {
        return dealWithElement(instruction.getSingleOperand(), varTable);
    }

    public String dealWithBinaryOpInstruction(BinaryOpInstruction instruction, HashMap<String, Descriptor> varTable) {
        String stringBuilder = dealWithElement(instruction.getLeftOperand(), varTable);
        stringBuilder += dealWithElement(instruction.getRightOperand(), varTable);
        return stringBuilder + dealWithOperation(instruction.getUnaryOperation());
    }

    public String dealWithElement(Element element, HashMap<String, Descriptor> varTable) {
        if (element instanceof LiteralElement) {
            return "iconst_" + ((LiteralElement) element).getLiteral() + "\n";
        }
        else if (element instanceof Operand) {
            Operand operand = (Operand) element;
            switch (operand.getType().getTypeOfElement()) {
                case INT32:
                    return "iload_" + varTable.get(operand.getName()).getVirtualReg() + "\n";
            }
        }
        return "DEu coco nos Elements";
    }

    public String dealWithOperation(Operation op) {
        switch (op.getOpType()) {
            case ADD:
                return "iadd\n";
        }
        return "Deu coco nas Ops";
    }

    public List<String> getConvertedParams(List<Element> params) {
        List<String> convertedParams = new ArrayList<>();
        for (Element element : params) {
            switch (element.getType().getTypeOfElement()) {
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
