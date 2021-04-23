import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.JmmParser;
import pt.up.fe.comp.jmm.JmmParserResult;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;

import java.io.StringReader;
import java.util.Arrays;

public class Main implements JmmParser {

	public static void main(String[] args) {
		System.out.println("Executing with args: " + Arrays.toString(args));
		if (args[0].contains("fail")) {
			throw new RuntimeException("It's supposed to fail");
		}
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
