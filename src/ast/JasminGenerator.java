package ast;

import org.specs.comp.ollir.*;
import pt.up.fe.comp.jmm.JmmNode;

import java.util.*;

/**
 * Input Data -> {scope, extra_data1, extra_data2, ...}
 * Output Data -> {OLLIR, extra_data1, extra_data2, ...}
 */
public class JasminGenerator {
    private ClassUnit classUnit;
    private String jasminCode;
    private final Set<JmmNode> visited = new HashSet<>();
    private int conditional;

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
            this.conditional = 0;
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
                for (Element element: method.getParams()) {
                    stringBuilder += convertElementType(element.getType().getTypeOfElement());
                }
                stringBuilder += ")" + this.convertElementType(method.getReturnType().getTypeOfElement()) + "\n";
            }

            HashMap<String, Descriptor> varTable = method.getVarTable();

            stringBuilder += ".limit stack 100\n";
            localCount = varTable.size() + 1;
            stringBuilder += ".limit locals " + localCount + "\n";


            for (Instruction instruction : method.getInstructions()) {
                stringBuilder += dealWithInstruction(instruction, varTable, method.getLabels());
            }

            if (method.getReturnType().getTypeOfElement() == ElementType.VOID) {
                stringBuilder += "return";
            }

            stringBuilder += "\n.end method\n";

        }
        return stringBuilder;
    }

    public String dealWithInstruction(Instruction instruction, HashMap<String, Descriptor> varTable, HashMap<String, Instruction> methodLabels) {
        String stringBuilder = "";
        for (Map.Entry<String, Instruction> entry : methodLabels.entrySet()) {
            if (entry.getValue().equals(instruction)){
                stringBuilder += entry.getKey() + ":\n";
                break;
            }
        }

        if (instruction instanceof AssignInstruction) {
            stringBuilder += dealWithAssignment((AssignInstruction) instruction, varTable);
        }
        else if (instruction instanceof SingleOpInstruction) {
            stringBuilder += dealWithSingleOpInstruction((SingleOpInstruction) instruction, varTable);
        }
        else if (instruction instanceof BinaryOpInstruction) {
            stringBuilder += dealWithBinaryOpInstruction((BinaryOpInstruction) instruction, varTable);
        }
        else if (instruction instanceof CallInstruction) {
            stringBuilder += dealWithCallInstruction((CallInstruction) instruction, varTable);
        }
        else if (instruction instanceof CondBranchInstruction) {
            stringBuilder += dealWithCondBranchInstruction((CondBranchInstruction) instruction, varTable);
        }
        else if (instruction instanceof GotoInstruction) {
            stringBuilder += dealWithGotoInstrutcion((GotoInstruction) instruction, varTable);
        }
        else if (instruction instanceof PutFieldInstruction) {
            stringBuilder += dealWithPutFieldInstruction((PutFieldInstruction) instruction, varTable);
        }
        else if (instruction instanceof GetFieldInstruction) {
            stringBuilder += dealWithGetFieldInstruction((GetFieldInstruction) instruction, varTable);
        }
        else if (instruction instanceof ReturnInstruction) {
            stringBuilder += dealWithReturnInstruction((ReturnInstruction) instruction, varTable);
        }
        else {
            //TODO
            stringBuilder += "Deu esparguete nas Instructions";
        }

        return stringBuilder;
    }

    public String dealWithAssignment(AssignInstruction inst, HashMap<String, Descriptor> varTable) {
        String stringBuilder = dealWithInstruction(inst.getRhs(), varTable, new HashMap<String, Instruction>());
        Operand operand = (Operand) inst.getDest();

        if(!operand.getType().getTypeOfElement().equals(ElementType.OBJECTREF))
            stringBuilder += this.storeElement(operand, varTable);

        return stringBuilder;
    }

    public String dealWithSingleOpInstruction(SingleOpInstruction instruction, HashMap<String, Descriptor> varTable) {
        return loadElement(instruction.getSingleOperand(), varTable);
    }

    public String dealWithBinaryOpInstruction(BinaryOpInstruction instruction, HashMap<String, Descriptor> varTable) {
        String leftOperand = loadElement(instruction.getLeftOperand(), varTable);
        String rightOperand = loadElement(instruction.getRightOperand(), varTable);

        return this.dealWithOperation(instruction.getUnaryOperation(), leftOperand, rightOperand);
    }

    private String dealWithOperation(Operation operation, String leftOperand, String rightOperand) {
        OperationType ot = operation.getOpType();
        switch (ot) {
            case ADD:
                return leftOperand + rightOperand + "iadd\n";
            case SUB:
                return leftOperand + rightOperand + "isub\n";
            case MUL:
                return leftOperand + rightOperand + "imul\n";
            case DIV:
                return leftOperand + rightOperand + "idiv\n";
            case LTH:
            case GTE:
            case ANDB:
            case NOTB:
                this.conditional++;
                return this.dealWithBooleanOperation(ot, leftOperand, rightOperand, "");
            default:
                return "Deu esparguete nas Ops\n";
        }
    }

    private String dealWithBooleanOperation(OperationType ot, String leftOperand, String rightOperand, String branchLabel) {
        boolean defaultLabel = branchLabel.isEmpty();
        switch (ot) {
            case LTH:
            case GTE: {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(leftOperand).append(rightOperand);

                if (defaultLabel) {
                    stringBuilder.append(this.dealWithRelationalOperation(ot, this.getTrueLabel()))
                            .append("iconst_0\n")
                            .append("goto ").append(this.getEndIfLabel()).append("\n")
                            .append(this.getTrueLabel()).append(":\n")
                            .append("iconst_1\n")
                            .append(this.getEndIfLabel()).append(":\n");
                }
                else {
                    stringBuilder.append(this.dealWithRelationalOperation(ot, branchLabel));
                }

                return stringBuilder.toString();
            }
            case EQ: {
                // javap faz com ifeq
                StringBuilder stringBuilder = new StringBuilder();

                if (rightOperand.equals("iconst_1\n")) {
                    stringBuilder.append(leftOperand)
                            .append("ifne ").append(branchLabel).append("\n");
                }

                return stringBuilder.toString();
            }
            case ANDB: {
                String ifeq = "ifeq " + this.getTrueLabel() + "\n";

                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(leftOperand)
                        .append(ifeq)
                        .append(rightOperand)
                        .append(ifeq);

                if (defaultLabel) {
                    stringBuilder.append("iconst_1\n")
                            .append("goto ").append(this.getEndIfLabel()).append("\n")
                            .append(this.getTrueLabel()).append(":\n")
                            .append("iconst_0\n")
                            .append(this.getEndIfLabel()).append(":\n");
                }
                else {
                    stringBuilder.append("goto ").append(branchLabel).append("\n");
                    stringBuilder.append(this.getTrueLabel()).append(":\n");
                }

                return stringBuilder.toString();
            }
            case NOTB: {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(leftOperand);

                if (defaultLabel) {
                    stringBuilder.append("ifne ").append(this.getTrueLabel()).append("\n")
                            .append("iconst_1\n")
                            .append("goto ").append(this.getEndIfLabel()).append("\n")
                            .append(this.getTrueLabel()).append(":\n")
                            .append("iconst_0\n")
                            .append(this.getEndIfLabel()).append(":\n");
                }
                else {
                    stringBuilder.append("ifne ").append(branchLabel).append("\n");
                }

                return stringBuilder.toString();
            }
            default:
                return "Deu esparguete nas BooleansOperations\n";
        }
    }

    private String dealWithRelationalOperation(OperationType ot, String trueLabel) {
        switch (ot) {
            case LTH:
                return String.format("if_icmplt %s\n", trueLabel);
            case GTE:
                return String.format("if_icmpge %s\n", trueLabel);
            default:
                return "Deu esparguete nas RelationalOps\n";
        }
    }

    public String dealWithCallInstruction(CallInstruction instruction, HashMap<String, Descriptor> varTable) {
        String stringBuilder = "";
        CallType type = instruction.getInvocationType();

        switch (type) {
            case invokestatic:
            case invokespecial:
            case invokevirtual:
                stringBuilder += this.dealWithInvoke(instruction, varTable, type);
                break;
            case arraylength:
                stringBuilder += this.loadElement(instruction.getFirstArg(), varTable);
                stringBuilder += "arraylength\n";
                break;
            case NEW:
                stringBuilder += this.dealWithNewObject(instruction, varTable);
                break;
        }

        return stringBuilder;
    }

    public String dealWithInvoke(CallInstruction instruction, HashMap<String, Descriptor> varTable, CallType type){
        String stringBuilder = ""; //TODO deal with invokes
        String className;

        Operand obj = (Operand)instruction.getFirstArg();
        LiteralElement func = (LiteralElement) instruction.getSecondArg();
        String parameters = "";

        stringBuilder += this.loadElement(instruction.getFirstArg(), varTable);
        for (Element element : instruction.getListOfOperands()) {
            stringBuilder += this.loadElement(element, varTable);
            parameters += this.convertElementType(element.getType().getTypeOfElement());
        }

        if(obj.getName().equals("this"))
            className = classUnit.getClassName() + "." ;
        else
            className = "";

        stringBuilder += type.name() + " " + className + func.getLiteral().replace("\"","") + "(" + parameters + ")" + this.convertElementType(instruction.getReturnType().getTypeOfElement()) + "\n";

        if(obj.getType().getTypeOfElement().equals(ElementType.OBJECTREF))
            stringBuilder += this.storeElement(obj, varTable);

        return stringBuilder;
    }

    public String dealWithNewObject(CallInstruction instruction, HashMap<String, Descriptor> varTable){
        Element e = instruction.getFirstArg();
        String stringBuilder = "";

        if (e.getType().getTypeOfElement().equals(ElementType.ARRAYREF)) {
            stringBuilder += this.loadElement(instruction.getListOfOperands().get(0), varTable);
            stringBuilder += "newarray int\n";
        }
        else if (e.getType().getTypeOfElement().equals(ElementType.OBJECTREF)){
            stringBuilder += "new " + ((Operand)e).getName() + "\ndup\n";
        }

        return stringBuilder;
    }

    public String dealWithCondBranchInstruction(CondBranchInstruction instruction, HashMap<String, Descriptor> varTable) {
        String leftOperand = this.loadElement(instruction.getLeftOperand(), varTable);
        String rightOperand = this.loadElement(instruction.getRightOperand(), varTable);

        return this.dealWithBooleanOperation(instruction.getCondOperation().getOpType(), leftOperand, rightOperand, instruction.getLabel());
    }

    public String dealWithGotoInstrutcion(GotoInstruction instruction, HashMap<String, Descriptor> varTable) {
        return String.format("goto %s\n", instruction.getLabel());
    }

    public String dealWithPutFieldInstruction(PutFieldInstruction instruction, HashMap<String, Descriptor> varTable) {
        String stringBuilder = "";
        Operand obj = (Operand)instruction.getFirstOperand();
        Operand var = (Operand)instruction.getSecondOperand();
        Element value = instruction.getThirdOperand();

        stringBuilder += this.loadElement((Element) obj, varTable); //push object (Class ref) onto the stack

        stringBuilder += this.loadElement(value, varTable); //store const element on stack

        //TODO using className not this
        return stringBuilder + "putfield " + classUnit.getClassName() + "/" + var.getName() + " " + convertElementType(var.getType().getTypeOfElement()) + "\n";
    }

    public String dealWithGetFieldInstruction(GetFieldInstruction instruction, HashMap<String, Descriptor> varTable) {
        String jasminCode = "";
        Operand obj = (Operand)instruction.getFirstOperand();
        Operand var = (Operand)instruction.getSecondOperand();

        jasminCode += this.loadElement((Element) obj, varTable); //push object (Class ref) onto the stack

        //TODO using same syntax as putfield with className, example: https://flylib.com/books/en/2.883.1.11/1/
        return jasminCode + "getfield " + classUnit.getClassName() + "/" + var.getName() + " " + convertElementType(var.getType().getTypeOfElement()) +  "\n";
    }

    public String dealWithReturnInstruction(ReturnInstruction instruction, HashMap<String, Descriptor> varTable) {
        //if(!instruction.hasReturnValue()) return "return";
        String returnString = "";

        // TODO switch to instruction.getElementType
        switch (instruction.getOperand().getType().getTypeOfElement()) {
            case INT32:
            case BOOLEAN:
                returnString = loadElement(instruction.getOperand(), varTable);
                returnString += "ireturn";
                break;
            case ARRAYREF:
            case OBJECTREF:
                returnString = loadElement(instruction.getOperand(), varTable);
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
            case VOID:
                return "V";
        }
        return "Deu esparguete converter ElementType";
    }

    public String loadElement(Element element, HashMap<String, Descriptor> varTable) {
        if (element instanceof LiteralElement) {
            String num = ((LiteralElement) element).getLiteral();
            if (Integer.parseInt(num) <= 5) {
                return "iconst_" + num + "\n";
            }
            else {
                return "bipush " + num + "\n";
            }
        }
        else if (element instanceof ArrayOperand) {
            ArrayOperand operand = (ArrayOperand) element;

            // Load array
            String stringBuilder = String.format("aload%s\n", this.getVirtualReg(operand.getName(), varTable));
            // Load index
            stringBuilder += loadElement(operand.getIndexOperands().get(0), varTable);

            return stringBuilder + "iaload\n";
        }
        else if (element instanceof Operand) {
            Operand operand = (Operand) element;
            switch (operand.getType().getTypeOfElement()) {
                // TODO this appears in VarTable as local variable
                case THIS:
                    return "aload_0\n";
                case INT32:
                case BOOLEAN: {
                    return String.format("iload%s\n", this.getVirtualReg(operand.getName(), varTable));
                }
                case ARRAYREF: {
                    return String.format("aload%s\n", this.getVirtualReg(operand.getName(), varTable));
                }
                case CLASS: { //TODO deal with class
                    return "";
                }
                case OBJECTREF:{
                    return ""; // CALL Operand: q OBJECTREF, Literal: "<init>"
                }
                default:
                    break;
            }
        }
        System.out.println(element);
        return "Deu esparguete nos loads Elements\n";
    }

    public String storeElement(Operand operand, HashMap<String, Descriptor> varTable) {
        switch (operand.getType().getTypeOfElement()) {
            case INT32:
            case BOOLEAN: {
                return String.format("istore%s\n", this.getVirtualReg(operand.getName(), varTable));
            }
            case ARRAYREF: {
                return String.format("astore%s\n", this.getVirtualReg(operand.getName(), varTable));
            }
            case OBJECTREF: {
                return String.format("astore%s\n", this.getVirtualReg(operand.getName(), varTable));
            }
            default:
                break;
        }

        return "Deu esparguete nos store Elements";
    }

    private String getVirtualReg(String varName, HashMap<String, Descriptor> varTable) {
        int virtualReg = varTable.get(varName).getVirtualReg();
        if (virtualReg > 3) {
            return " " + virtualReg;
        }
        return "_" + virtualReg;
    }

    private String getTrueLabel() {
        return "True" + this.conditional;
    }

    private String getEndIfLabel() {
        return "EndIf" + this.conditional;
    }
}
