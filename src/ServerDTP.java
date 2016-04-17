import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

class ServerDTP {
    private ServerPI serverPI;
    private String dataHost;
    private int dataPort = -1;
    private StreamTransmission transmission = new StreamTransmission();
    private Representation representation = Representation.ASCII;

    ServerDTP(ServerPI serverPI) {
        this.serverPI = serverPI;
    }

    Representation getRepresentation() {
        return representation;
    }

    void setRepresentation(Representation representation) {
        this.representation = representation;
    }

    public void setDataPort(String host, int port) {
        dataHost = host;
        dataPort = port;
    }

    int receiveFile(String path) throws CommandException {
        int reply = 0;
        FileOutputStream fos = null;
        Socket dataSocket = null;
        try {
            File file = new File(path);
            if (file.exists())
                throw new CommandException(550, "File exists in that location.");

            fos = new FileOutputStream(file);

            if (dataPort == -1)
                throw new CommandException(500, "Can't establish data connection: no PORT specified.");
            dataSocket = new Socket(dataHost, dataPort);

            serverPI.reply(150, "Opening " + representation.getName() + " mode data connection.");
            transmission.receiveFile(dataSocket, fos, representation);
            reply = serverPI.reply(226, "Transfer complete.");
        } catch (ConnectException e) {
            throw new CommandException(425, "Can't open data connection.");
        } catch (IOException e) {
            throw new CommandException(550, "Can't write to file");
        } finally {
            try {
                if (fos != null)
                    fos.close();
                if (dataSocket != null)
                    dataSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return reply;
    }

    int sendFile(String path) throws CommandException {
        int reply = 0;
        FileInputStream fis = null;
        Socket dataSocket = null;
        try {
            File file = new File(path);
            if (!file.isFile()) {
                throw new CommandException(550, "Not a plain file.");
            }

            fis = new FileInputStream(file);

            if (dataPort == -1) {
                throw new CommandException(500, "Can't establish data connection: no PORT specified.");
            }

            dataSocket = new Socket(dataHost, dataPort);

            serverPI.reply(150, "Opening " + representation.getName() + " mode data connection.");
            transmission.sendFile(fis, dataSocket, representation);
            reply = serverPI.reply(226, "Transfer complete.");
        } catch (FileNotFoundException e) {
            throw new CommandException(550, "No such file.");
        } catch (ConnectException e) {
            throw new CommandException(425, "Can't open data connection.");
        } catch (IOException e) {
            throw new CommandException(553, "Not a regular file.");
        } finally {
            try {
                if (fis != null)
                    fis.close();
                if (dataSocket != null)
                    dataSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return reply;
    }

    void abort() {
        transmission.abort();
    }

    int sendNameList(String path) throws CommandException {
        int reply = 0;
        Socket dataSocket = null;
        try {
            File dir = new File(path);
            String fileNames[] = dir.list();

            dataSocket = new Socket(dataHost, dataPort);
            Representation representation = Representation.ASCII;
            PrintWriter writer = new PrintWriter(representation.getOutputStream(dataSocket));

            serverPI.reply(150, "Opening " + representation.getName() + " mode data connection.");
            for (String fileName : fileNames) {
                writer.print(fileName);
                writer.print('\n');
            }
            writer.flush();
            reply = serverPI.reply(226, "Transfer complete.");
        } catch (ConnectException e) {
            throw new CommandException(425, "Can't open data connection.");
        } catch (Exception e) {
            throw new CommandException(550, "No such directory.");
        } finally {
            try {
                if (dataSocket != null)
                    dataSocket.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        return reply;
    }

    int sendList(String path) throws CommandException {
        int reply = 0;
        Socket dataSocket = null;
        try {
            File dir = new File(path);
            String fileNames[] = dir.list();
            int numFiles = (fileNames != null) ? fileNames.length : 0;

            dataSocket = new Socket(dataHost, dataPort);
            Representation representation = Representation.ASCII;
            PrintWriter writer = new PrintWriter(representation.getOutputStream(dataSocket));

            serverPI.reply(150, "Opening " + representation.getName() + " mode data connection.");
            writer.print("total " + numFiles + "\n");

            for (int i = 0; i < numFiles; i++) {
                String fileName = fileNames[i];

                File file = new File(dir, fileName);
                listFile(file, writer);
            }

            writer.flush();

            reply = serverPI.reply(226, "Transfer complete.");
        } catch (ConnectException e) {
            throw new CommandException(425, "Can't open data connection.");
        } catch (Exception e) {
            throw new CommandException(550, "No such directory.");
        } finally {
            try {
                if (dataSocket != null)
                    dataSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return reply;
    }

    private void listFile(File file, PrintWriter writer) throws IOException{
        Date date = new Date(file.lastModified());
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd hh:mm");
        String dateStr = dateFormat.format(date);
        long size = file.length();
        String sizeStr = Long.toString(size);
        int sizePadLength = Math.max(8 - sizeStr.length(), 0);
        String sizeField = pad(sizePadLength) + sizeStr;
        writer.print(file.isDirectory() ? 'd' : '-');
        writer.print("rwxrwxrwx");
        writer.print(" ");
        writer.print("  1");
        writer.print(" ");
        writer.print("ftp     ");
        writer.print(" ");
        writer.print("ftp     ");
        writer.print(" ");
        writer.print(sizeField);
        writer.print(" ");
        writer.print(dateStr);
        writer.print(" ");
        writer.print(file.getName());

        writer.print('\n');
    }

    private static String pad(int length) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < length; i++)
            buf.append(' ');
        return buf.toString();
    }
}