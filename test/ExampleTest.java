import org.junit.Test;
import pt.up.fe.comp.TestUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;

public class ExampleTest {
    public String parse(final String filename) throws IOException {
        File file = new File("test/fixtures/public/" + filename);
        FileInputStream fis = new FileInputStream(file);
        byte[] data = new byte[(int) file.length()];
        fis.read(data);
        fis.close();

        return new String(data, StandardCharsets.UTF_8);
    }


    @Test
    public void testExpression() throws IOException {
        String findMax = parse("FindMaximum.jmm");

		assertEquals("Program", TestUtils.parse(findMax).getRootNode().getKind());
	}

}
