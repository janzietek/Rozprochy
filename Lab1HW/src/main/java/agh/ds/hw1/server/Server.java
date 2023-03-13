package agh.ds.hw1.server;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Runnable {
    public static final ArrayList<ConnectionHandler> ConnectionsList = new ArrayList<>();
    public static final Set<Integer> UsersUdpPorts = ConcurrentHashMap.newKeySet();
    public static int PORT = 12345;
    private ServerSocket serverTCP;

    @Override
    public void run() {
        try {
            serverTCP = new ServerSocket(PORT);
            DatagramSocket datagramSocket = new DatagramSocket(PORT);
            System.out.println("SERVER STARTED");
            ExecutorService pool = Executors.newCachedThreadPool();
            while (!serverTCP.isClosed()) {
                Socket socket = serverTCP.accept();
                ConnectionHandler clientHandler = new ConnectionHandler(socket, datagramSocket);
                pool.execute(clientHandler);
                System.out.println("NEW CLIENT WAITING");
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            CloseServer();
        }
    }

    private void CloseServer() {
        try {
            if (serverTCP != null) {
                serverTCP.close();
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
