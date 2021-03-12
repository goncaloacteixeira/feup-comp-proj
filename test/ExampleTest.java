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
        String testImport = "import io;\n" +
                "class WhileAndIF {\n" +
                " \n" +
                "    public static void main(String[] args){\n" +
                "        int a;\n" +
                "        int b;\n" +
                "        int c;\n" +
                "        int[] d;\n" +
                "        a = 20;\n" +
                "        b = 10;\n" +
                "        d = new int[10];\n" +
                "        if( a < b){\n" +
                "            c  = a-1;\n" +
                "        }else{\n" +
                "            c = b-1;\n" +
                "        }\n" +
                "\n" +
                "\n" +
                "        while((0-1) < c){\n" +
                "            d[c] = a-b; \n" +
                "            c= c-1;\n" +
                "            a= a-1;\n" +
                "            b= b-1;\n" +
                "        }\n" +
                "        c=0;\n" +
                "        while(c < d.length){\n" +
                "            io.println(d[c]);\n" +
                "            c= c+1;\n" +
                "        }\n" +
                "    }\n" +
                "\n" +
                "}";

		assertEquals("Program", TestUtils.parse(testImport).getRootNode().getKind());
	}

}
