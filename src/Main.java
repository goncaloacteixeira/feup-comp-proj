import ast.JmmSymbolTable;
import pt.up.fe.comp.MainAnalysis;
import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.JmmParser;
import pt.up.fe.comp.jmm.JmmParserResult;
import pt.up.fe.comp.jmm.analysis.JmmAnalysis;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.specs.util.SpecsIo;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;

public class Main implements JmmParser {

	public static void main(String[] args) throws IOException {
		System.out.println("Executing with args: " + Arrays.toString(args));

		File jmmFile = new File(args[0]);
		String jmm = Files.readString(jmmFile.toPath());

		JmmParserResult parserResult = new Main().parse(jmm);
		JmmSemanticsResult jmmAnalysis = new AnalysisStage().semanticAnalysis(parserResult);
		OllirResult ollirResult = new OptimizationStage().toOllir(jmmAnalysis);
		JasminResult jasminResult = new BackendStage().toJasmin(ollirResult);

		Path path = Paths.get(ollirResult.getSymbolTable().getClassName() + "/");
		Files.createDirectory(path);

		/* AST */
		try {
			FileWriter myWriter = new FileWriter(path + "/ast.json");
			myWriter.write(parserResult.toJson());
			myWriter.close();
		} catch (IOException e) {
			System.out.println("An error occurred.");
			e.printStackTrace();
		}

		/* VARTABLE */
		try {
			FileWriter myWriter = new FileWriter(path + "/symboltable.txt");
			myWriter.write(ollirResult.getSymbolTable().toString());
			myWriter.close();
		} catch (IOException e) {
			System.out.println("An error occurred.");
			e.printStackTrace();
		}

		/* OLLIR CODE */
		try {
			FileWriter myWriter = new FileWriter(path + "/" + ollirResult.getSymbolTable().getClassName() + ".ollir");
			myWriter.write(ollirResult.getOllirCode());
			myWriter.close();
		} catch (IOException e) {
			System.out.println("An error occurred.");
			e.printStackTrace();
		}

		/* Jasmin Code */
		try {
			FileWriter myWriter = new FileWriter(path + "/" + ollirResult.getSymbolTable().getClassName() + ".j");
			myWriter.write(jasminResult.getJasminCode());
			myWriter.close();
		} catch (IOException e) {
			System.out.println("An error occurred.");
			e.printStackTrace();
		}

		/* JAVA compiled class */
		jasminResult.compile(path.toFile());
	}

	/**
	 * Given a String representing Jmm Code, returns the Result of that parsing
	 * @param jmmCode 	String representing Java minus minus code
	 * @return 			JmmParserResult
	 */
	public JmmParserResult parse(String jmmCode) {

		try {
			JAVAMINUSMINUSPARSER parser = new JAVAMINUSMINUSPARSER(new StringReader(jmmCode));
			SimpleNode root = parser.Program(); // returns reference to root node

    		// root.dump(""); // prints the tree on the screen

    		return new JmmParserResult(root, parser.reports);
		} catch(ParseException e) {
			throw new RuntimeException("Error while parsing", e);
		}
	}
}
