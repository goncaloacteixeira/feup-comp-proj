package ast;

import org.specs.comp.ollir.*;

import java.util.*;

public class JasminGenerator {
    private ClassUnit classUnit;
    private String jasminCode;
    private int conditional;
    private int stack_counter;
    private int max_counter;

    public JasminGenerator(ClassUnit classUnit) {
        this.classUnit = classUnit;
    }

    public String dealWithClass() {
        StringBuilder stringBuilder = new StringBuilder("");

        // class declaration
        stringBuilder.append(".class ").append(classUnit.getClassName()).append("\n");

        // extends declaration
        if (classUnit.getSuperClass() != null) {
            stringBuilder.append(".super ").append(classUnit.getSuperClass()).append("\n");
        }
        else {
            stringBuilder.append(".super java/lang/Object\n");
        }

        // fields declaration
        for (Field f : classUnit.getFields()) {
            stringBuilder.append(".field '").append(f.getFieldName()).append("' ").append(this.convertType(f.getFieldType())).append("\n");
        }

        for (Method method : classUnit.getMethods()) {
            this.conditional = 0;
            this.stack_counter = 0;
            this.max_counter = 0;

            stringBuilder.append(this.dealWithMethodHeader(method));
            String instructions = this.dealtWithMethodIntructions(method);
            if (!method.isConstructMethod()) {
                stringBuilder.append(this.dealWithMethodLimits(method));
                stringBuilder.append(instructions);
            }
        }

        return stringBuilder.toString();
    }

    private String dealWithMethodHeader(Method method) {
        if (method.isConstructMethod()) {
            String classSuper = "java/lang/Object";
            if (classUnit.getSuperClass() != null) {
                classSuper = classUnit.getSuperClass();
            }

            return "\n.method public <init>()V\naload_0\ninvokespecial " + classSuper +  ".<init>()V\nreturn\n.end method\n";
        }

        StringBuilder stringBuilder = new StringBuilder("\n.method").append(" ").append(method.getMethodAccessModifier().name().toLowerCase()).append(" ");

        if (method.isStaticMethod()) {
            stringBuilder.append("static ");
        }
        else if (method.isFinalMethod()) {
            stringBuilder.append("final ");
        }

        // Parameters type
        stringBuilder.append(method.getMethodName()).append("(");
        for (Element element: method.getParams()) {
            stringBuilder.append(convertType(element.getType()));
        }
        // Return type
        stringBuilder.append(")").append(this.convertType(method.getReturnType())).append("\n");

        return stringBuilder.toString();
    }

    private String dealWithMethodLimits(Method method) {
        StringBuilder stringBuilder = new StringBuilder();

        int localCount = method.getVarTable().size() + 1;
        stringBuilder.append(".limit locals ").append(localCount).append("\n");
        stringBuilder.append(".limit stack ").append(" 99").append("\n");

        return stringBuilder.toString();
    }

    private String dealtWithMethodIntructions(Method method) {
        StringBuilder stringBuilder = new StringBuilder();
        method.getVarTable();
        for (Instruction instruction : method.getInstructions()) {
            stringBuilder.append(dealWithInstruction(instruction, method.getVarTable(), method.getLabels()));
            if (instruction instanceof CallInstruction && ((CallInstruction) instruction).getReturnType().getTypeOfElement() != ElementType.VOID) {
                stringBuilder.append("pop\n");
                this.decrementStackCounter(1);
            }
        }

        stringBuilder.append("\n.end method\n");
        return stringBuilder.toString();
    }

    private String dealWithInstruction(Instruction instruction, HashMap<String, Descriptor> varTable, HashMap<String, Instruction> methodLabels) {
        StringBuilder stringBuilder = new StringBuilder();
        for (Map.Entry<String, Instruction> entry : methodLabels.entrySet()) {
            if (entry.getValue().equals(instruction)){
                stringBuilder.append(entry.getKey()).append(":\n");
            }
        }

        switch (instruction.getInstType()) {
            case ASSIGN:
                return stringBuilder.append(dealWithAssignment((AssignInstruction) instruction, varTable)).toString();
            case NOPER:
                return stringBuilder.append(dealWithSingleOpInstruction((SingleOpInstruction) instruction, varTable)).toString();
            case BINARYOPER:
                return stringBuilder.append(dealWithBinaryOpInstruction((BinaryOpInstruction) instruction, varTable)).toString();
            case UNARYOPER:
                return "Deal with '!' in correct form";
            case CALL:
                return stringBuilder.append(dealWithCallInstruction((CallInstruction) instruction, varTable)).toString();
            case BRANCH:
                return stringBuilder.append(dealWithCondBranchInstruction((CondBranchInstruction) instruction, varTable)).toString();
            case GOTO:
                return stringBuilder.append(dealWithGotoInstrutcion((GotoInstruction) instruction, varTable)).toString();
            case PUTFIELD:
                return stringBuilder.append(dealWithPutFieldInstruction((PutFieldInstruction) instruction, varTable)).toString();
            case GETFIELD:
                return stringBuilder.append(dealWithGetFieldInstruction((GetFieldInstruction) instruction, varTable)).toString();
            case RETURN:
                return stringBuilder.append(dealWithReturnInstruction((ReturnInstruction) instruction, varTable)).toString();
            default:
                return "Error in Instructions";
        }
    }

    private String dealWithAssignment(AssignInstruction inst, HashMap<String, Descriptor> varTable) {
        String stringBuilder = "";
        Operand operand = (Operand) inst.getDest();
        if (operand instanceof ArrayOperand) {
            ArrayOperand aoperand = (ArrayOperand) operand;

            // Load array
            stringBuilder += String.format("aload%s\n", this.getVirtualReg(aoperand.getName(), varTable));
            this.incrementStackCounter(1);

            // Load index
            stringBuilder += loadElement(aoperand.getIndexOperands().get(0), varTable);
        }

        stringBuilder += dealWithInstruction(inst.getRhs(), varTable, new HashMap<String, Instruction>());
        if(!(operand.getType().getTypeOfElement().equals(ElementType.OBJECTREF) && inst.getRhs() instanceof CallInstruction)) { //if its a new object call does not store yet
            stringBuilder += this.storeElement(operand, varTable);
        }

        return stringBuilder;
    }

    private String dealWithSingleOpInstruction(SingleOpInstruction instruction, HashMap<String, Descriptor> varTable) {
        return loadElement(instruction.getSingleOperand(), varTable);
    }

    private String dealWithBinaryOpInstruction(BinaryOpInstruction instruction, HashMap<String, Descriptor> varTable) {
        String leftOperand = loadElement(instruction.getLeftOperand(), varTable);
        String rightOperand = loadElement(instruction.getRightOperand(), varTable);

        // TODO Deal with Ifs and Conditionals
        return this.dealWithOperation(instruction.getUnaryOperation(), leftOperand, rightOperand);
    }

    private String dealWithOperation(Operation operation, String leftOperand, String rightOperand) {
        OperationType ot = operation.getOpType();
        switch (ot) {
            // ..., value1, value2 →
            // ..., result
            case ADD:
                this.decrementStackCounter(1);
                return leftOperand + rightOperand + "iadd\n";
            case SUB:
                this.decrementStackCounter(1);
                return leftOperand + rightOperand + "isub\n";
            case MUL:
                this.decrementStackCounter(1);
                return leftOperand + rightOperand + "imul\n";
            case DIV:
                this.decrementStackCounter(1);
                return leftOperand + rightOperand + "idiv\n";
            case LTH:
            case GTE:
            case ANDB:
            case NOTB:
                // TODO Deal with Ifs and Conditionals
                this.conditional++;
                return this.dealWithBooleanOperation(ot, leftOperand, rightOperand, "");
            default:
                return "Error in Operations\n";
        }
    }

    private String dealWithBooleanOperation(OperationType ot, String leftOperand, String rightOperand, String branchLabel) {
        boolean defaultLabel = branchLabel.isEmpty();
        switch (ot) {
            case LTH:
            case GTE: {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(leftOperand).append(rightOperand);

                stringBuilder.append(this.dealWithRelationalOperation(ot, this.getTrueLabel()))
                        .append("iconst_1\n")
                        .append("goto ").append(this.getEndIfLabel()).append("\n")
                        .append(this.getTrueLabel()).append(":\n")
                        .append("iconst_0\n")
                        .append(this.getEndIfLabel()).append(":\n");

                this.incrementStackCounter(1);

                return stringBuilder.toString();
            }
            case ANDB: {
                String ifeq = "ifeq " + this.getTrueLabel() + "\n";

                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(leftOperand)
                        .append(ifeq)
                        .append(rightOperand)
                        .append(ifeq);

                stringBuilder.append("iconst_1\n")
                        .append("goto ").append(this.getEndIfLabel()).append("\n")
                        .append(this.getTrueLabel()).append(":\n")
                        .append("iconst_0\n")
                        .append(this.getEndIfLabel()).append(":\n");

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
                    //..., value →
                    // ...
                    stringBuilder.append("ifeq ").append(branchLabel).append("\n");
                    this.decrementStackCounter(1);
                }

                return stringBuilder.toString();
            }
            default:
                return "Error in BooleansOperations\n";
        }
    }

    private String dealWithRelationalOperation(OperationType ot, String trueLabel) {
        // ..., value1, value2 →
        // ...
        this.decrementStackCounter(2);
        switch (ot) {
            case LTH:
                return String.format("if_icmpge %s\n", trueLabel);
            case GTE:
                return String.format("if_icmplt %s\n", trueLabel);
            default:
                return "Error in RelationalOperations\n";
        }
    }

    private String dealWithCallInstruction(CallInstruction instruction, HashMap<String, Descriptor> varTable) {
        String stringBuilder = "";
        CallType callType = instruction.getInvocationType();

        switch (callType) {
            case invokespecial:
                Operand arg = (Operand) instruction.getFirstArg();
                stringBuilder += this.dealWithInvoke(instruction, varTable, callType, ((ClassType)instruction.getFirstArg().getType()).getName());
                if(arg.getType().getTypeOfElement() != ElementType.THIS) stringBuilder += this.storeElement(arg, varTable);
                break;
            case invokestatic:
                stringBuilder += this.dealWithInvoke(instruction, varTable, callType, ((Operand)instruction.getFirstArg()).getName());
                break;
            case invokevirtual:
                stringBuilder += this.dealWithInvoke(instruction, varTable, callType, ((ClassType)instruction.getFirstArg().getType()).getName());
                break;
            case arraylength:
                stringBuilder += this.loadElement(instruction.getFirstArg(), varTable);

                // ..., arrayref →
                // ..., length
                // No need to change stack
                stringBuilder += "arraylength\n";
                break;
            case NEW:
                stringBuilder += this.dealWithNewObject(instruction, varTable);
                break;
            default:
                return "Erro in CallInstruction";
        }

        return stringBuilder;
    }

    private String dealWithInvoke(CallInstruction instruction, HashMap<String, Descriptor> varTable, CallType callType, String className){
        String stringBuilder = ""; //TODO deal with invokes

        LiteralElement func = (LiteralElement) instruction.getSecondArg();
        String parameters = "";

        if (!func.getLiteral().equals("\"<init>\"")) {  //does not load element because its a new object, its already done in dealWithNewObject with new and dup
            stringBuilder += this.loadElement(instruction.getFirstArg(), varTable);
        }

        int num_params = 0;
        for (Element element : instruction.getListOfOperands()) {
            stringBuilder += this.loadElement(element, varTable);
            parameters += this.convertType(element.getType());
            num_params++;
        }

        // ..., objectref (if not static), [arg1, [arg2 ...]] →
        // ..., value (if not void)
        if (!instruction.getInvocationType().equals(CallType.invokestatic)) {
            num_params += 1;
        }
        this.decrementStackCounter(num_params);
        if (instruction.getReturnType().getTypeOfElement() != ElementType.VOID) {
            this.incrementStackCounter(1);
        }

        stringBuilder += callType.name() + " " + this.getOjectClassName(className) + "." + func.getLiteral().replace("\"","") + "(" + parameters + ")" + this.convertType(instruction.getReturnType()) + "\n";

        return stringBuilder;
    }

    private String dealWithNewObject(CallInstruction instruction, HashMap<String, Descriptor> varTable){
        Element e = instruction.getFirstArg();
        String stringBuilder = "";

        if (e.getType().getTypeOfElement().equals(ElementType.ARRAYREF)) {
            stringBuilder += this.loadElement(instruction.getListOfOperands().get(0), varTable);

            // ..., count →
            // ..., arrayref
            // No need to change stack
            stringBuilder += "newarray int\n";
        }
        else if (e.getType().getTypeOfElement().equals(ElementType.OBJECTREF)){
            // NEW:
            // ... →
            // ..., objectref

            // DUP:
            // ..., value →
            // ..., value, value
            this.incrementStackCounter(2);

            stringBuilder += "new " + this.getOjectClassName(((Operand)e).getName()) + "\ndup\n";
        }

        return stringBuilder;
    }

    private String dealWithCondBranchInstruction(CondBranchInstruction instruction, HashMap<String, Descriptor> varTable) {
        String leftOperand = this.loadElement(instruction.getLeftOperand(), varTable);
        String rightOperand = this.loadElement(instruction.getRightOperand(), varTable);

        return this.dealWithBooleanOperation(instruction.getCondOperation().getOpType(), leftOperand, rightOperand, instruction.getLabel());
    }

    private String dealWithGotoInstrutcion(GotoInstruction instruction, HashMap<String, Descriptor> varTable) {
        return String.format("goto %s\n", instruction.getLabel());
    }

    private String dealWithPutFieldInstruction(PutFieldInstruction instruction, HashMap<String, Descriptor> varTable) {
        String stringBuilder = "";
        Operand obj = (Operand)instruction.getFirstOperand();
        Operand var = (Operand)instruction.getSecondOperand();
        Element value = instruction.getThirdOperand();

        stringBuilder += this.loadElement(obj, varTable); //push object (Class ref) onto the stack

        stringBuilder += this.loadElement(value, varTable); //store const element on stack

        // ..., objectref, value →
        this.decrementStackCounter(2);

        return stringBuilder + "putfield " + classUnit.getClassName() + "/" + var.getName() + " " + convertType(var.getType()) + "\n";
    }

    private String dealWithGetFieldInstruction(GetFieldInstruction instruction, HashMap<String, Descriptor> varTable) {
        String jasminCode = "";
        Operand obj = (Operand)instruction.getFirstOperand();
        Operand var = (Operand)instruction.getSecondOperand();

        jasminCode += this.loadElement(obj, varTable); //push object (Class ref) onto the stack

        // ..., objectref →
        // ..., value
        // No need to change stack

        return jasminCode + "getfield " + classUnit.getClassName() + "/" + var.getName() + " " + convertType(var.getType()) +  "\n";
    }

    private String dealWithReturnInstruction(ReturnInstruction instruction, HashMap<String, Descriptor> varTable) {
        if(!instruction.hasReturnValue()) return "return";
        String returnString = "";

        switch (instruction.getOperand().getType().getTypeOfElement()) {
            case VOID:
                returnString = "return";
                break;
            case INT32:
            case BOOLEAN:
                returnString = loadElement(instruction.getOperand(), varTable);

                // value →
                this.decrementStackCounter(1);
                returnString += "ireturn";
                break;
            case ARRAYREF:
            case OBJECTREF:
                returnString = loadElement(instruction.getOperand(), varTable);

                // objectref →
                this.decrementStackCounter(1);
                returnString  += "areturn";
                break;
            default:
                break;
        }

        return returnString;
    }

    private String convertType(Type type) {
        ElementType elementType = type.getTypeOfElement();
        String stringBuilder = "";

        if (elementType == ElementType.ARRAYREF) {
            elementType = ((ArrayType) type).getTypeOfElements();
            stringBuilder += "[";
        }

        switch (elementType) {
            case INT32:
                return stringBuilder + "I";
            case BOOLEAN:
                return stringBuilder + "Z";
            case STRING:
                return stringBuilder + "Ljava/lang/String;";
            case OBJECTREF:
                String className = ((ClassType) type).getName();
                return stringBuilder + "L" + this.getOjectClassName(className) + ";";
            case CLASS:
                return "CLASS";
            case VOID:
                return "V";
            default:
                return "Error converting ElementType";
        }
    }

    private String getOjectClassName(String className) {
        for (String _import : classUnit.getImports()) {
            if (_import.endsWith("." + className)) {
                return _import.replaceAll("\\.", "/");
            }
        }
        return className;
    }

    private String loadElement(Element element, HashMap<String, Descriptor> varTable) {
        if (element instanceof LiteralElement) {
            String num = ((LiteralElement) element).getLiteral();
            this.incrementStackCounter(1);
            return this.selectConstType(num) + "\n";
        }
        else if (element instanceof ArrayOperand) {
            ArrayOperand operand = (ArrayOperand) element;

            // Load array
            String stringBuilder = String.format("aload%s\n", this.getVirtualReg(operand.getName(), varTable));
            this.incrementStackCounter(1);

            // Load index
            stringBuilder += loadElement(operand.getIndexOperands().get(0), varTable);

            // ..., arrayref, index →
            // ..., value
            this.decrementStackCounter(1);
            return stringBuilder + "iaload\n";
        }
        else if (element instanceof Operand) {
            Operand operand = (Operand) element;
            switch (operand.getType().getTypeOfElement()) {
                case THIS:
                    this.incrementStackCounter(1);
                    return "aload_0\n";
                case INT32:
                case BOOLEAN: {
                    this.incrementStackCounter(1);
                    return String.format("iload%s\n", this.getVirtualReg(operand.getName(), varTable));
                }
                case OBJECTREF:
                case ARRAYREF: {
                    this.incrementStackCounter(1);
                    return String.format("aload%s\n", this.getVirtualReg(operand.getName(), varTable));
                }
                case CLASS: { //TODO deal with class
                    return "";
                }
                default:
                    return "Error in operand loadElements\n";
            }
        }
        System.out.println(element);
        return "Error in loadElements\n";
    }

    private String storeElement(Operand operand, HashMap<String, Descriptor> varTable) {
        if (operand instanceof ArrayOperand) {
            // ..., arrayref, index, value →
            this.decrementStackCounter(3);
            return "iastore\n";
        }

        switch (operand.getType().getTypeOfElement()) {
            case INT32:
            case BOOLEAN: {
                // ..., value →
                this.decrementStackCounter(1);
                return String.format("istore%s\n", this.getVirtualReg(operand.getName(), varTable));
            }
            case OBJECTREF:
            case ARRAYREF: {
                // ..., objectref →
                this.decrementStackCounter(1);
                return String.format("astore%s\n", this.getVirtualReg(operand.getName(), varTable));
            }
            default:
                return "Error in storeElements";
        }
    }

    private String getVirtualReg(String varName, HashMap<String, Descriptor> varTable) {
        int virtualReg = varTable.get(varName).getVirtualReg();
        if (virtualReg > 3) {
            return " " + virtualReg;
        }
        return "_" + virtualReg;
    }

    private String getTrueLabel() {
        return "myTrue" + this.conditional;
    }

    private String getEndIfLabel() {
        return "myEndIf" + this.conditional;
    }

    private String selectConstType(String literal){
        return Integer.parseInt(literal) < -1 || Integer.parseInt(literal) > 5 ?
                    Integer.parseInt(literal) < -128 || Integer.parseInt(literal) > 127 ?
                        Integer.parseInt(literal) < -32768 || Integer.parseInt(literal) > 32767 ?
                            "ldc " + literal :
                        "sipush " + literal :
                    "bipush " + literal :
                "iconst_" + literal;
    }

    private void incrementStackCounter(int add) {
        this.stack_counter += add;
        if (this.stack_counter > this.max_counter) this.max_counter = stack_counter;
    }

    private void decrementStackCounter(int sub) {
        this.stack_counter -= sub;
    }
}
