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
                stringBuilder += "<init>()V\naload_0\ninvokespecial java/lang/Object.<init>()V\nreturn\n.end method\n";
                continue;
            }
            if (method.isStaticMethod()) {
                stringBuilder += "static main([Ljava/lang/String;)V\n";
            }
            else {
                stringBuilder += method.getMethodName() + "(";

                List<String> convertedParams = new ArrayList<>();
                for (Element element: method.getParams()) {
                    convertedParams.add(convertElementType(element.getType().getTypeOfElement()));
                }
                stringBuilder += String.join(",", convertedParams);

                stringBuilder += ")" + this.convertElementType(method.getReturnType().getTypeOfElement()) + "\n";
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

            if (method.getReturnType().getTypeOfElement() == ElementType.VOID) {
                stringBuilder += "return";
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
        else if (instruction instanceof PutFieldInstruction) {
            return dealWithPutFieldInstruction((PutFieldInstruction) instruction, varTable);
        }
        else if (instruction instanceof GetFieldInstruction) {
            return dealWithGetFieldInstruction((GetFieldInstruction) instruction, varTable);
        }
        else if (instruction instanceof ReturnInstruction) {
            return dealWithReturnInstruction((ReturnInstruction) instruction, varTable);
        }
        else {
            //TODO
            return "Deu coco nas Instructions";
        }
    }

    public String dealWithAssignment(AssignInstruction inst, HashMap<String, Descriptor> varTable) {
        String stringBuilder = dealWithInstruction(inst.getRhs(), varTable);
        Operand op = (Operand) inst.getDest();

        int virtualReg = varTable.get(op.getName()).getVirtualReg();
        if (virtualReg > 3) {
            return stringBuilder + "istore " + virtualReg + "\n";
        }
        else {
            return stringBuilder + "istore_" + virtualReg + "\n";
        }
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
                case BOOLEAN:
                    int virtualReg = varTable.get(operand.getName()).getVirtualReg();
                    if (virtualReg > 3) {
                        return "iload " + virtualReg + "\n";
                    }
                    else {
                        return "iload_" + virtualReg + "\n";
                    }

            }
        }
        return "Deu coco nos Elements";
    }

    public String dealWithOperation(Operation op) {
        switch (op.getOpType()) {
            case ADD:
                return "iadd\n";
        }
        return "Deu coco nas Ops";
    }

    public String dealWithPutFieldInstruction(PutFieldInstruction instruction, HashMap<String, Descriptor> varTable) {
        Operand obj = (Operand)instruction.getFirstOperand();
        Operand var = (Operand)instruction.getSecondOperand();
        Element value = instruction.getThirdOperand();

        String stringBuilder = this.dealWithElement(value, varTable);
        return stringBuilder + "putfield " + obj.getName() + "/" + var.getName() + " " + convertElementType(var.getType().getTypeOfElement()) + "\n";
    }

    public String dealWithGetFieldInstruction(GetFieldInstruction instruction, HashMap<String, Descriptor> varTable) {
        //TODO null ??????????
        /*Operand obj = (Operand)instruction.getFirstOperand();
        Operand var = (Operand)instruction.getSecondOperand();

        // TODO aload_0 when THIS
        String jasminCode = "aload_" + varTable.get(obj.getName()).getVirtualReg() + "\n";
        return jasminCode + "getfield " + convertElementType(var.getType().getTypeOfElement()) + " " + var.getName() + "\n";*/
        return "";
    }

    public String dealWithReturnInstruction(ReturnInstruction instruction, HashMap<String, Descriptor> varTable) {
        //if(!instruction.hasReturnValue()) return "return";
        String returnString = "";

        // TODO switch to instruction.getElementType
        switch (instruction.getOperand().getType().getTypeOfElement()) {
            case INT32:
            case BOOLEAN:
                returnString = dealWithElement(instruction.getOperand(), varTable);
                returnString += "ireturn";
                break;
            case ARRAYREF:
            case OBJECTREF:
                returnString = dealWithElement(instruction.getOperand(), varTable);
                returnString  += "areturn";
                break;
            default:
                break;
        }

        return returnString;
    }

    public String convertElementType(ElementType type) {
        switch (type) {
            case INT32:
                return "I";
            case BOOLEAN:
                return "Z";
            case ARRAYREF:
                return "[I";
            case OBJECTREF:
                return "OBJ";
        }

        return "";
    }
}
