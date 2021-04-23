import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ast.JmmSymbolTable;
import ast.OllirVisitor;
import ast.SymbolTableVisitor;
import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.SpecsIo;

/**
 * Copyright 2021 SPeCS.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License. under the License.
 */

public class OptimizationStage implements JmmOptimization {

    @Override
    public OllirResult toOllir(JmmSemanticsResult semanticsResult) {

        JmmNode node = semanticsResult.getRootNode();

        // More reports from this stage
        OllirVisitor visitor = new OllirVisitor((JmmSymbolTable) semanticsResult.getSymbolTable(), semanticsResult.getReports());
        // Convert the AST to a String containing the equivalent OLLIR code
        System.out.println("Preorder Visitor - Generating OLLIR...");
        String ollirCode = (String) visitor.visit(node, Arrays.asList("DEFAULT_VISIT")).get(0);
        System.out.println("OLLIR Generation Successful!");

        // System.out.println(ollirCode);

        // Para escrever o ficheiro com codigo OLLIR para efeitos de teste
        try {
            FileWriter myWriter = new FileWriter("test.ollir");
            myWriter.write(ollirCode);
            myWriter.close();
            System.out.println("Successfully wrote to the file.");
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }


        return new OllirResult(semanticsResult, ollirCode, semanticsResult.getReports());
    }

    @Override
    public JmmSemanticsResult optimize(JmmSemanticsResult semanticsResult) {
        // THIS IS JUST FOR CHECKPOINT 3
        return semanticsResult;
    }

    @Override
    public OllirResult optimize(OllirResult ollirResult) {
        // THIS IS JUST FOR CHECKPOINT 3
        return ollirResult;
    }

}
