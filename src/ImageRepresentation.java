import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

class ImageRepresentation extends Representation {
    ImageRepresentation() {
        super("binary", 'I');
    }

    public InputStream getInputStream(Socket socket) throws IOException {
        return socket.getInputStream();
    }

    public OutputStream getOutputStream(Socket socket) throws IOException {
        return socket.getOutputStream();
    }

    public long sizeOf(File file) throws IOException {
        return file.length();
    }
}
