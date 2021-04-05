package pt.up.fe.comp.jmm;

/**
 * Parses J-- code.
 * 
 * @author COMP2021
 *
 */
public interface JmmParser {

	/**
	 * Given a String representing Jmm Code, returns the Result of that parsing
	 * @param jmmCode String representing Java minus minus code
	 * @return A JmmParserResult
	 */
	JmmParserResult parse(String jmmCode);
	
}