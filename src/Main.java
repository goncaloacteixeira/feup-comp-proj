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
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;

public class Main implements JmmParser {

	public static void main(String[] args) throws IOException {
		System.out.println("Executing with args: " + Arrays.toString(args));

		/*File jmmFile = new File(args[0]);
		String jmm = Files.readString(jmmFile.toPath());

		JmmParserResult parserResult = new Main().parse(jmm);
		JmmSemanticsResult jmmAnalysis = new AnalysisStage().semanticAnalysis(parserResult);
		OllirResult ollirResult = new OptimizationStage().toOllir(jmmAnalysis);
		JasminResult jasminResult = new BackendStage().toJasmin(ollirResult);

		jasminResult.compile(new File("jasmin"));*/
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
