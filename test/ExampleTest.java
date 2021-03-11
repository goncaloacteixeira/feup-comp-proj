import static org.junit.Assert.*;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Properties;
import java.io.StringReader;

import pt.up.fe.comp.TestUtils;

public class ExampleTest {


    @Test
    public void testExpression() {
        String testImport = "import MathUtils;\n" +
                "import ioPlus;\n" +
                "\n" +
                "class MonteCarloPi {\n" +
                "\tpublic boolean performSingleEstimate() {\n" +
                "\t\tint rand1;\n" +
                "\t\tint rand2;\n" +
                "\t\tboolean in_circle;\n" +
                "\t\tint squareDist;\n" +
                "\n" +
                "\t\trand1 = MathUtils.random(0-100, 100);\n" +
                "\t\trand2 = MathUtils.random(0-100, 100);\n" +
                "\n" +
                "\t\tsquareDist = (rand1 * rand1 + rand2 * rand2) / 100;\n" +
                "\t\tif (squareDist < 100) {\n" +
                "\t\t\tin_circle = true;\n" +
                "\t\t} else {\n" +
                "\t\t\tin_circle = false;\n" +
                "\t\t}\n" +
                "\n" +
                "\t\treturn in_circle;\n" +
                "\t}\n" +
                "\n" +
                "\tpublic int estimatePi100(int n) {\n" +
                "\t\tint samples_in_circle;\n" +
                "\t\tint samples_so_far;\n" +
                "\t\tint pi_estimate;\n" +
                "\n" +
                "\t\tsamples_so_far = 0;\n" +
                "\t\tsamples_in_circle = 0;\n" +
                "\n" +
                "\t\twhile (samples_so_far < n) {\n" +
                "\t\t\tif (this.performSingleEstimate()) {\n" +
                "\t\t\t\tsamples_in_circle = samples_in_circle + 1;\n" +
                "\t\t\t} else {\n" +
                "\t\t\t}\n" +
                "\t\t\tsamples_so_far = samples_so_far + 1;\n" +
                "\t\t}\n" +
                "\n" +
                "\t\tpi_estimate = 400 * samples_in_circle / n;\n" +
                "\t\treturn pi_estimate;\n" +
                "\t}\n" +
                "\n" +
                "\tpublic static void main(String[] args) {\n" +
                "\t\tint pi_estimate_times_100;\n" +
                "\t\tint num_samples;\n" +
                "\n" +
                "\t\tnum_samples = ioPlus.requestNumber();\n" +
                "\t\tpi_estimate_times_100 = new MonteCarloPi().estimatePi100(num_samples);\n" +
                "\n" +
                "\t\tioPlus.printResult(pi_estimate_times_100);\n" +
                "\t}\n" +
                "}";

		assertEquals("Program", TestUtils.parse(testImport).getRootNode().getKind());
	}

}
