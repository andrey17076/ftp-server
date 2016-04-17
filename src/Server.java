import java.io.IOException;

import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    private static final int SERVER_PORT = 8888;

    public static void main(String[] args) throws IOException {
        Server server = new Server();
        server.start();
    }

    private void start() throws IOException {
        ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
        while (true) {
            Socket clientSocket = serverSocket.accept();
            ServerPI pi = new ServerPI(clientSocket);
            new Thread(pi).start();
        }
    }
}