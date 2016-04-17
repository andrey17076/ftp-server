import java.io.*;
import java.net.Socket;

class AsciiRepresentation extends Representation {
    AsciiRepresentation() {
        super("ascii", 'A');
    }

    public InputStream getInputStream(Socket socket) throws IOException {
        return new AsciiInputStream(socket.getInputStream());
    }

    public OutputStream getOutputStream(Socket socket) throws IOException {
        return new AsciiOutputStream(socket.getOutputStream());
    }

    public long sizeOf(File file) throws IOException {
        InputStream in = new FileInputStream(file);
        long count = 0;

        try {
            int c;
            while ((c = in.read()) != -1) {
                if (c == '\r')
                    continue;
                if (c == '\n')
                    count++;
                count++;
            }
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return count;
    }
}

class AsciiInputStream extends FilterInputStream {
    AsciiInputStream(InputStream in) {
        super(in);
    }

    public int read() throws IOException {
        int c;
        if ((c = in.read()) == -1)
            return c;
        if (c == '\r') {
            if ((c = in.read()) == -1)
                return c;
        }
        return c;
    }

    public int read(byte data[], int off, int len) throws IOException {
        if (len <= 0)
            return 0;

        int c;

        if ((c = read()) == -1)
            return -1;
        else
            data[off] = (byte) c;

        int i = 1;
        try {
            for (; i < len; i++) {
                if ((c = read()) == -1)
                    break;
                if (c == '\r') {
                    if ((c = in.read()) == -1)
                        break;
                }
                data[off + i] = (byte) c;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return i;
    }
}

class AsciiOutputStream extends FilterOutputStream {

    AsciiOutputStream(OutputStream out) {
        super(out);
    }

    public void write(int b) throws IOException {
        if (b == '\r')
            return;
        if (b == '\n')
            out.write('\r');
        out.write(b);
    }

    public void write(byte data[], int off, int len) throws IOException {
        for (int i = 0; i < len; i++) {
            byte b = data[off + i];
            if (b == '\n')
                out.write('\r');
            out.write(b);
        }
    }
}
