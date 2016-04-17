import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Socket;

import java.util.NoSuchElementException;
import java.util.Stack;
import java.util.StringTokenizer;

class ServerPI implements Runnable {

    private Socket clientSocket;
    private BufferedReader reader;
    private PrintWriter writer;
    private ServerDTP dtp;
    private Class commandHandlerArgTypes[] = { String.class, StringTokenizer.class };

    private String username;
    private String password;

    private final String baseDir = System.getProperty("user.home");
    private String currentDir = "/";

    ServerPI(Socket clientSocket) throws IOException {
        this.clientSocket = clientSocket;
        reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        writer = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()), true);

        dtp = new ServerDTP(this);
    }

    @Override
    public void run() {
        try {
            handleClient();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    int reply(int code, String text) {
        writer.println(code + " " + text);
        return code;
    }

    private String createNativePath(String ftpPath) {
        String path = baseDir;

        if (ftpPath.charAt(0) != '/') {
            path += (currentDir + "/");
        }

        path += ftpPath;

        return path;
    }

    private String resolvePath(String path) {
        if (path.charAt(0) != '/') {
            path = currentDir + "/" + path;
        }

        StringTokenizer pathSt = new StringTokenizer(path, "/");
        Stack<String> segments = new Stack<>();

        while (pathSt.hasMoreTokens()) {
            String segment = pathSt.nextToken();
            if (segment.equals("..")) {
                if (!segments.empty()) {
                    segments.pop();
                }
            } else if (segment.equals(".")) {
                // skip
            } else {
                segments.push(segment);
            }
        }

        StringBuilder pathBuf = new StringBuilder("/");
        segments.forEach(elem -> { pathBuf.append(elem); pathBuf.append("/"); });

        return pathBuf.toString();
    }

    private void checkLogin() throws CommandException {
        if (password == null) {
            throw new CommandException(530, "Please login with USER and PASS.");
        }
    }

    private void handleClient() throws IOException {
        reply(220, clientSocket.getInetAddress().getHostName()+ " FTP server is ready");
        String line;

        while ((line = reader.readLine()) != null)
        {
            //debug
            System.out.println(line);
            //debug

            StringTokenizer st = new StringTokenizer(line);
            String command = st.nextToken().toLowerCase();
            Object args[] = { line, st };

            try {
                Method commandHandler = getClass().getMethod("handle_" + command, commandHandlerArgTypes);
                int code = (Integer) commandHandler.invoke(this, args);
                if (code == 221) {
                    return;
                }
            } catch (InvocationTargetException e) {
                try {
                    throw (Exception) e.getTargetException();
                } catch (CommandException ce) {
                    reply(ce.getCode(), ce.getText());
                } catch (NoSuchElementException e1) {
                    reply(500, "'" + line + "': command not understood.");
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            } catch (NoSuchMethodException e) {
                reply(500, "'" + line + "': command not understood.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public int handle_user(String line, StringTokenizer st) throws CommandException {
        username = st.nextToken();

        return reply(331, "Password required for " + username + ".");
    }

    public int handle_pass(String line, StringTokenizer st) throws CommandException {
        if (username == null) {
            throw new CommandException(503, "Login with USER first.");
        }

        this.password = (st.hasMoreTokens()) ? st.nextToken() : "";

        return reply(230, "User " + username + " logged in.");
    }

    public int handle_list(String line, StringTokenizer st) throws CommandException {
        checkLogin();

        String path = (st.hasMoreTokens() ? st.nextToken() : currentDir);

        path = createNativePath(path);

        return dtp.sendList(path);
    }

    public int handle_pwd(String line, StringTokenizer st) throws CommandException {
        checkLogin();

        return reply(257, currentDir);
    }

    public int handle_type(String line, StringTokenizer st) throws CommandException {
        checkLogin();
        String arg = st.nextToken().toUpperCase();

        if (arg.length() != 1) {
            throw new CommandException(500, "TYPE: invalid argument '" + arg + "'");
        }

        char code = arg.charAt(0);
        Representation representation = Representation.get(code);

        if (representation == null) {
            throw new CommandException(500, "TYPE: invalid argument '" + arg + "'");
        }

        dtp.setRepresentation(representation);
        return reply(200, "Type set to " + arg);
    }

    public int handle_pasv(String line, StringTokenizer st) throws CommandException {
        checkLogin();
        return reply(500, "'" + line + "': command not supported.");
    }

    public int handle_port(String line, StringTokenizer st) throws CommandException {
        checkLogin();

        String portStr = st.nextToken();
        st = new StringTokenizer(portStr, ",");

        String h1 = st.nextToken();
        String h2 = st.nextToken();
        String h3 = st.nextToken();
        String h4 = st.nextToken();
        int p1 = Integer.parseInt(st.nextToken());
        int p2 = Integer.parseInt(st.nextToken());

        String dataHost = h1 + "." + h2 + "." + h3 + "." + h4;
        int dataPort = (p1 << 8) | p2;

        dtp.setDataPort(dataHost, dataPort);

        return reply(200, "PORT command successful.");
    }

    public int handle_cwd(String line, StringTokenizer st) throws CommandException {
        checkLogin();

        String arg = st.nextToken();
        String newDir = arg;

        if (newDir.length() == 0) {
            newDir = "/";
        }

        newDir = resolvePath(newDir);

        File file = new File(createNativePath(newDir));

        if (!file.exists()) {
            throw new CommandException(550, arg + ": no such directory");
        } else if (!file.isDirectory()) {
            throw new CommandException(550, arg + ": not a directory");
        }

        currentDir = newDir;
        return reply(250, "CWD command successful.");
    }

    public int handle_cdup(String line, StringTokenizer st) throws CommandException {
        checkLogin();
        return handle_cwd(line, st);
    }

    public int handle_quit(String line, StringTokenizer st) throws CommandException {
        username = null;
        password = null;
        return reply(221, "Goodbye.");
    }

    public int handle_noop(String line, StringTokenizer st) throws CommandException {
        checkLogin();

        return reply(200, "NOOP command successful.");
    }

    public int handle_nlst(String line, StringTokenizer st) throws CommandException {
        checkLogin();

        String path = (st.hasMoreTokens() ? st.nextToken() : currentDir);

        path = createNativePath(path);

        return dtp.sendNameList(path);
    }

    public int handle_mkd(String line, StringTokenizer st) throws CommandException {
        checkLogin();

        String arg = st.nextToken();
        String dirPath = resolvePath(arg);

        File dir = new File(createNativePath(dirPath));

        if (dir.exists()) {
            throw new CommandException(550, arg + ": file exists");
        }
        if (!dir.mkdir()) {
            throw new CommandException(550, arg + ": directory could not be created");
        }

        return reply(257, "\"" + dirPath + "\" directory created");
    }

    public int handle_dele(String line, StringTokenizer st) throws CommandException {
        checkLogin();

        String arg = st.nextToken();
        String filePath = resolvePath(arg);

        File file = new File(createNativePath(filePath));

        if (!file.exists()) {
            throw new CommandException(550, arg + ": file does not exist");
        }
        if (!file.delete()) {
            throw new CommandException(550, arg + ": could not delete file");
        }

        return reply(250, "DELE command successful.");
    }

    public int handle_rmd(String line, StringTokenizer st) throws CommandException {
        checkLogin();

        String arg = st.nextToken();
        String dirPath = resolvePath(arg);

        File dir = new File(createNativePath(dirPath));

        if (!dir.exists()) {
            throw new CommandException(550, arg + ": directory does not exist");
        }
        if (!dir.isDirectory()) {
            throw new CommandException(550, arg + ": not a directory");
        }
        if (!dir.delete()) {
            throw new CommandException(550, arg + ": could not remove directory");
        }

        return reply(250, "RMD command successful.");
    }

    public int handle_size(String line, StringTokenizer st) throws CommandException {
        checkLogin();

        String arg = st.nextToken();
        String path = resolvePath(arg);
        File file = new File(createNativePath(path));

        if (!file.exists()) {
            throw new CommandException(550, arg + ": no such file");
        }
        if (!file.isFile()) {
            throw new CommandException(550, arg + ": not a plain file");
        }

        Representation representation = dtp.getRepresentation();
        long size;
        try {
            size = representation.sizeOf(file);
        } catch (IOException e) {
            throw new CommandException(550, e.getMessage());
        }

        return reply(213, "" + size);
    }

    public int handle_rein(String line, StringTokenizer st) throws CommandException {
        checkLogin();

        username = null;
        password = null;
        currentDir = "/";
        dtp = new ServerDTP(this);
        return reply(220, "Service ready for new user.");
    }

    public int handle_retr(String line, StringTokenizer st) throws CommandException {
        checkLogin();
        String path;

        try {
            path = line.substring(5);
        } catch (Exception e) {
            throw new NoSuchElementException(e.getMessage());
        }

        path = createNativePath(path);

        return dtp.sendFile(path);
    }

    public int handle_stor(String line, StringTokenizer st) throws CommandException {
        checkLogin();
        String path;

        try {
            path = line.substring(5);
        } catch (Exception e) {
            throw new NoSuchElementException(e.getMessage());
        }

        path = createNativePath(path);
        return dtp.receiveFile(path);
    }
}
