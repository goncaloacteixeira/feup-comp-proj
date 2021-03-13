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
    public void findMax() throws IOException {
        String findMax = parse("FindMaximum.jmm");

		assertEquals("Program", TestUtils.parse(findMax).getRootNode().getKind());
	}

	@Test
    public void helloWorld() throws IOException {
        String helloWorld = parse("HelloWorld.jmm");
        assertEquals("Program", TestUtils.parse(helloWorld).getRootNode().getKind());
    }

    @Test
    public void lazySort() throws IOException {
        String lazySort = parse("Lazysort.jmm");
        assertEquals("Program", TestUtils.parse(lazySort).getRootNode().getKind());
    }

    @Test
    public void monteCarloPi() throws IOException {
        String monteCarloPi = parse("MonteCarloPi.jmm");
        assertEquals("Program", TestUtils.parse(monteCarloPi).getRootNode().getKind());
    }

    @Test
    public void quickSort() throws IOException {
        String quickSort = parse("QuickSort.jmm");
        assertEquals("Program", TestUtils.parse(quickSort).getRootNode().getKind());
    }

}
