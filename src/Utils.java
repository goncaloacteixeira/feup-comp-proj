import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;


public class Utils {
    /**
     * Transforms a String to a InputStream
     * @param text  String to be transformed
     * @return      The corresponding InputStream
     */
	public static InputStream toInputStream(String text) {
        try {
            return new ByteArrayInputStream(text.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }
}