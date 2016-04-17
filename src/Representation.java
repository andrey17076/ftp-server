import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Hashtable;

abstract class Representation {

    private static Hashtable<Character, Representation> representations = new Hashtable<>();

    public static final Representation ASCII = new AsciiRepresentation();
    public static final Representation IMAGE = new ImageRepresentation();

    static Representation get(char code) {
        return representations.get(code);
    }

    private String name;
    private char code;

    Representation(String name, char code) {
        this.name = name;
        this.code = code;

        representations.put(code, this);
    }

    final String getName() {
        return name;
    }

    public final char getCode() {
        return code;
    }

    public abstract InputStream getInputStream(Socket socket) throws IOException;

    public abstract OutputStream getOutputStream(Socket socket) throws IOException;

    public abstract long sizeOf(File file) throws IOException;
}
