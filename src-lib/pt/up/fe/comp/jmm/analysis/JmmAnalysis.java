package pt.up.fe.comp.jmm.analysis;

import pt.up.fe.comp.jmm.JmmParserResult;

/**
 * This stage deals with analysis performed at the AST level, essentially semantic analysis and symbol table generation. 
 */
public interface JmmAnalysis {

	/**
	 * Executes the Semantic Analysis on a JMMParserResult
	 * @param parserResult 	a JmmParserResult to be analysed
	 * @return 				The Result of the Analysis as a JmmSemanticsResult
	 */
	JmmSemanticsResult semanticAnalysis(JmmParserResult parserResult);
		
}