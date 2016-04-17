import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

class StreamTransmission {
    private static final int BUF_SIZE = 1024;
    private boolean isAborted = false;

    void abort() {
        isAborted = true;
    }

    void sendFile(InputStream in, Socket s, Representation representation) throws IOException {
        isAborted = false;

        OutputStream out = representation.getOutputStream(s);
        byte buf[] = new byte[BUF_SIZE];
        int nread;
        while (((nread = in.read(buf)) > 0) && !isAborted) {
            out.write(buf, 0, nread);
        }
        out.close();
    }

    void receiveFile(Socket s, OutputStream out, Representation representation) throws IOException {
        isAborted = false;

        InputStream in = representation.getInputStream(s);
        byte buf[] = new byte[BUF_SIZE];
        int nread;
        while (((nread = in.read(buf, 0, BUF_SIZE)) > 0) && !isAborted) {
            out.write(buf, 0, nread);
        }
        in.close();
    }
}
