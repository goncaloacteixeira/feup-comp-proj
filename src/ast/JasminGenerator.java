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

            HashMap<String, Descriptor> varTable = OllirAccesser.getVarTable(method);

            for (Map.Entry<String, Descriptor> entry : varTable.entrySet()) {
                if (entry.getValue().getScope().equals(VarScope.LOCAL))
                    localCount++;
            }

            stringBuilder += ".limit stack 100\n";
            stringBuilder += ".limit locals 100\n"; //+ localCount + "\n";


            for (Instruction instruction : method.getInstructions()) {
                stringBuilder += dealWithInstruction(instruction, varTable, OllirAccesser.getLabels(method));
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
        switch (operation.getOpType()) {
            case ADD:
                return leftOperand + rightOperand + "iadd\n";
            case SUB:
                return leftOperand + rightOperand + "isub\n";
            case MUL:
                return leftOperand + rightOperand + "imul\n";
            case DIV:
                return leftOperand + rightOperand + "idiv\n";
            case LTH:
                this.conditional++;
                return leftOperand +
                        rightOperand +
                        "if_icmplt " + this.getTrueLabel() + "\n" +
                        "iconst_0\n" +
                        "goto " + this.getEndIfLabel() + "\n" +
                        this.getTrueLabel() + ":\n" +
                        "iconst_1\n" +
                        this.getEndIfLabel() + ":\n";
            case ANDB:
                this.conditional++;
                String ifeq = "ifeq " + this.getTrueLabel() + "\n";
                return leftOperand +
                        ifeq +
                        rightOperand +
                        ifeq +
                        "iconst_1\n" +
                        "goto " + this.getEndIfLabel() + "\n" +
                        this.getTrueLabel() + ":\n" +
                        "iconst_0\n" +
                        this.getEndIfLabel() + ":\n";
            case NOTB:
                this.conditional++;
                return leftOperand +
                        "ifne " + this.getTrueLabel() + "\n" +
                        "iconst_1\n" +
                        "goto " + this.getEndIfLabel() + "\n" +
                        this.getTrueLabel() + ":\n" +
                        "iconst_0\n" +
                        this.getEndIfLabel() + ":\n";
            default:
                return "Deu esparguete nas Ops\n";
        }
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
        else if (element instanceof Operand) {
            Operand operand = (Operand) element;
            System.out.println(operand.getName());
            switch (operand.getType().getTypeOfElement()) {
                // TODO this appears in VarTable as local variable
                // TODO when accessing array (a[2]) it assumes as iload
                case THIS:
                    return "aload_0\n";
                case INT32:
                case BOOLEAN: {
                    int virtualReg = varTable.get(operand.getName()).getVirtualReg();
                    if (virtualReg > 3) {
                        return "iload " + virtualReg + "\n";
                    }
                    else {
                        return "iload_" + virtualReg + "\n";
                    }
                }
                case ARRAYREF: {
                    int virtualReg = varTable.get(operand.getName()).getVirtualReg();
                    if (virtualReg > 3) {
                        return "aload " + virtualReg + "\n";
                    }
                    else {
                        return "aload_" + virtualReg + "\n";
                    }
                }
                default:
                    break;
            }
        }
        return "Deu esparguete nos loads Elements";
    }

    public String storeElement(Operand operand, HashMap<String, Descriptor> varTable) {
        switch (operand.getType().getTypeOfElement()) {
            case INT32:
            case BOOLEAN: {
                int virtualReg = varTable.get(operand.getName()).getVirtualReg();
                if (virtualReg > 3) {
                    return "istore " + virtualReg + "\n";
                }
                else {
                    return "istore_" + virtualReg + "\n";
                }
            }
            case ARRAYREF: {
                int virtualReg = varTable.get(operand.getName()).getVirtualReg();
                if (virtualReg > 3) {
                    return "astore " + virtualReg + "\n";
                }
                else {
                    return "astore_" + virtualReg + "\n";
                }
            }
            default:
                break;
        }

        return "Deu esparguete nos store Elements";
    }

    public String dealWithCallInstruction(CallInstruction instruction, HashMap<String, Descriptor> varTable) {
        String stringBuilder = "";
        switch (OllirAccesser.getCallInvocation(instruction)) {
            case invokevirtual:
                Operand obj = (Operand)instruction.getFirstArg();
                LiteralElement func = (LiteralElement) instruction.getSecondArg();
                String parameters = "";

                stringBuilder += this.loadElement(instruction.getFirstArg(), varTable);
                for (Element element : instruction.getListOfOperands()) {
                    stringBuilder += this.loadElement(element, varTable);
                    parameters += this.convertElementType(element.getType().getTypeOfElement());
                }

                stringBuilder += "invokevirtual " + obj.getName() + "." + func.getLiteral().replace("\"","") + "(" + parameters + ")" + this.convertElementType(instruction.getReturnType().getTypeOfElement()) + "\n";
                break;
            case arraylength:
                stringBuilder += this.loadElement(instruction.getFirstArg(), varTable);
                stringBuilder += "arraylength\n";
                break;
            case NEW:
                // TODO only dealing with array init
                stringBuilder += this.loadElement(instruction.getListOfOperands().get(0), varTable);
                stringBuilder += "newarray int\n";
                break;
        }

        return stringBuilder;
    }

    public String dealWithCondBranchInstruction(CondBranchInstruction instruction, HashMap<String, Descriptor> varTable) {
        // TODO improve conditions, no need to get true or false from operation to go to branch
        String leftOperand = this.loadElement(instruction.getLeftOperand(), varTable);
        String rightOperand = this.loadElement(instruction.getRightOperand(), varTable);

        String operation = this.dealWithOperation(instruction.getCondOperation(), leftOperand, rightOperand);
        return operation + "ifne " + instruction.getLabel() + "\n";
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
        }

        return "Deu esparguete converter ElementType";
    }

    private String getTrueLabel() {
        return "True" + this.conditional;
    }

    private String getEndIfLabel() {
        return "EndIf" + this.conditional;
    }
}
