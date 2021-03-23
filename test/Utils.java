import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Utils {
    public static final PrintStream realSystemOut = System.out;
    public static class NullOutputStream extends OutputStream {
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

    public static String getJmmCode(final String filename) throws IOException {
        File file = new File("test/fixtures/public/" + filename);
        FileInputStream fis = new FileInputStream(file);
        byte[] data = new byte[(int) file.length()];
        fis.read(data);
        fis.close();

        return new String(data, StandardCharsets.UTF_8);
    }

    public static List<String> getValidFiles() {
        File dir = new File("test/fixtures/public/");
        String[] names = dir.list((dir1, name) -> name.endsWith(".jmm"));

        assert names != null;
        return Arrays.asList(names);
    }

    public static List<String> getSyntacticalErrorFiles() {
        List<String> syntacticalErrorFiles = new ArrayList<>();
        File dirSyn = new File("test/fixtures/public/fail/syntactical/");
        String[] synNames = dirSyn.list((dir1, name) -> name.endsWith(".jmm"));

        assert synNames != null;
        for (String synName : synNames)
            syntacticalErrorFiles.add("/fail/syntactical/" + synName);

        return syntacticalErrorFiles;
    }

    public static List<String> getSemanticErrorFiles() {
        List<String> semanticErrorFiles = new ArrayList<>();
        File dirSem = new File("test/fixtures/public/fail/semantic/");
        String[] semNames = dirSem.list((dir1, name) -> name.endsWith(".jmm"));

        assert semNames != null;
        for (String semName : semNames)
            semanticErrorFiles.add("/fail/semantic/" + semName);

        return semanticErrorFiles;
    }
}
