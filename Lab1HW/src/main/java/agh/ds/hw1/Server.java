package agh.ds.hw1;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Runnable {
    private final int portNumber = 12345;
    private ServerSocket server;
    private ExecutorService pool;

    @Override
    public void run() {
        StartServer();
        System.out.println("SERVER STARTED");
        try {
            pool = Executors.newCachedThreadPool();
            while (!server.isClosed()) {
                Socket socket = server.accept();
                ConnectionHandler clientHandler = new ConnectionHandler(socket);
                pool.execute(clientHandler);
                System.out.println("NEW CLIENT");
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            CloseServer();
        }
    }

    private void StartServer() {
        try {
            server = new ServerSocket(portNumber);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void CloseServer() {
        try {
            if (server != null) {
                server.close();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("SERVER CLOSED");
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.run();
    }
}
