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

        //setDefaultVisit(this::defaultVisit);
    }

    private String dealWithClass(){
        String stringBuilder = "";
        stringBuilder += ".class " + classUnit.getClassName() + "\n";
        stringBuilder += ".method public <init>()V\n" +
                "aload_0\n" +
                "invokenonvirtual java/lang/Object/<init>()V\n" +
                "return\n" +
                ".end method";
        return stringBuilder;
    }

    public String dealWithMethod(){
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

                stringBuilder += ")";
            }

            for (Map.Entry<String, Descriptor> entry : OllirAccesser.getVarTable(method).entrySet()){
                if (entry.getValue().getScope().equals(VarScope.LOCAL))
                    localCount++;
            }

            stringBuilder += ".limit stack 100\n";
            stringBuilder += ".limit locals " + localCount + "\n";


            for (Instruction instruction : method.getInstructions()){
                dealWithInstruction(instruction);
            }

        }
        return stringBuilder;
    }

    public String dealWithInstruction(Instruction instruction){
        String stringBuilder = "";
        if (instruction instanceof AssignInstruction){

        }
        return "";
    }

    public String dealWithAssignment(AssignInstruction inst){
        return dealWithInstruction(inst.getRhs());
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
