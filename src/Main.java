import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.JmmParser;
import pt.up.fe.comp.jmm.JmmParserResult;
import pt.up.fe.comp.jmm.report.Report;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

public class Main implements JmmParser {
	public JmmParserResult parse(String jmmCode) {
		
		try {
			Calculator myCalc = new Calculator(new StringReader(jmmCode));
			SimpleNode root = myCalc.Program(); // returns reference to root node
            	
    		root.dump(""); // prints the tree on the screen
    	
    		return new JmmParserResult(root, myCalc.reports);
		} catch(ParseException e) {
			throw new RuntimeException("Error while parsing", e);
		}
	}

    public static void main(String[] args) {
        System.out.println("Executing with args: " + Arrays.toString(args));
        if (args[0].contains("fail")) {
            throw new RuntimeException("It's supposed to fail");
        }
    }

	public static String getJmmCode(final File jmmFile) throws IOException {
		FileInputStream fis = new FileInputStream(jmmFile);
		byte[] data = new byte[(int) jmmFile.length()];
		fis.read(data);
		fis.close();

		return new String(data, StandardCharsets.UTF_8);
	}

}