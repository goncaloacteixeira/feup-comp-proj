import org.junit.Before;
import org.junit.Test;
import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.JmmParserResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.stringparser.ParserResult;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ParserTest {
    private final PrintStream realSystemOut = System.out;
    private static class NullOutputStream extends OutputStream {
        @Override
        public void write(int b){
        }
        @Override
        public void write(byte[] b){
        }
        @Override
        public void write(byte[] b, int off, int len){
        }
        public NullOutputStream(){
        }
    }

    private List<String> validFiles;
    private List<String> syntacticalErrorFiles;
    private List<String> semanticErrorFiles;

    @Before
    public void setup() {
        validFiles = new ArrayList<>();
        File dir = new File("test/fixtures/public/");
        String[] names = dir.list((dir1, name) -> name.endsWith(".jmm"));

        assert names != null;
        validFiles.addAll(Arrays.asList(names));

        syntacticalErrorFiles = new ArrayList<>();
        File dirSyn = new File("test/fixtures/public/fail/syntactical/");
        String[] synNames = dirSyn.list((dir1, name) -> name.endsWith(".jmm"));

        assert synNames != null;
        for (String synName : synNames)
            syntacticalErrorFiles.add("/fail/syntactical/" + synName);

        semanticErrorFiles = new ArrayList<>();
        File dirSem = new File("test/fixtures/public/fail/semantic/");
        String[] semNames = dirSem.list((dir1, name) -> name.endsWith(".jmm"));

        assert semNames != null;
        for (String semName : semNames)
            semanticErrorFiles.add("/fail/semantic/" + semName);
    }

    @Test
    public void unitTest() throws IOException {
        System.out.println("Unit Test");
        String code = TestUtils.getJmmCode("/fail/syntactical/CompleteWhileTest.jmm");

        JmmParserResult parserResult = TestUtils.parse(code);

        assertEquals("Program", parserResult.getRootNode().getKind());
        System.out.println("\nREPORTS");
        for (Report report : parserResult.getReports()) {
            System.out.println(report);
        }
    }

    @Test
    public void generateJSON() throws IOException {
        System.out.println("Unit Test");
        System.setOut(new PrintStream(new NullOutputStream()));
        for (String filename : validFiles) {
            String code = TestUtils.getJmmCode(filename);
            String astJson = TestUtils.parse(code).getRootNode().toJson();
            File jmm = new File(filename);
            int i = jmm.getName().lastIndexOf('.');
            String name = jmm.getName().substring(0,i);
            File json = new File(name + ".json");

            FileOutputStream fos = new FileOutputStream(json);
            fos.write(astJson.getBytes(StandardCharsets.UTF_8));
            fos.close();
        }
        System.setOut(realSystemOut);
    }

    @Test
    public void testParser() throws IOException {
        System.out.println("\nTesting Valid Files");
        for (String filename : this.validFiles) {
            System.out.print("Testing: " + filename);
            String code = TestUtils.getJmmCode(filename);
            System.setOut(new PrintStream(new NullOutputStream()));
            assertEquals("Program", TestUtils.parse(code).getRootNode().getKind());
            System.setOut(realSystemOut);
            System.out.print("  - PASSED\n");
        }
	}

	/*@Test
    public void testSyntacticalErrors() throws IOException {
        System.out.println("\nTesting Syntactical Errors");
        for (String filename : this.syntacticalErrorFiles) {
            System.out.print("Testing: " + filename);
            String code = TestUtils.getJmmCode(filename);
            System.setOut(new PrintStream(new NullOutputStream()));
            try {
                TestUtils.parse(code).getRootNode().getKind();
                System.setOut(realSystemOut);
                fail();
            } catch (Exception e) {
                System.setOut(realSystemOut);
                System.out.print("  - PASSED\n");
            }
        }
    }*/

    @Test
    public void testSemanticErrors() throws IOException {
        System.out.println("\nTesting Semantic Errors");
        for (String filename : this.semanticErrorFiles) {
            System.out.print("Testing: " + filename);
            String code = TestUtils.getJmmCode(filename);
            System.setOut(new PrintStream(new NullOutputStream()));
            assertEquals("Program", TestUtils.parse(code).getRootNode().getKind());
            System.setOut(realSystemOut);
            System.out.print("  - PASSED\n");
        }
    }
}
