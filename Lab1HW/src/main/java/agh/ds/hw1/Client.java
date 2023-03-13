package agh.ds.hw1;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

public class Client {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private boolean active = true;

    public Client() throws IOException {
        try {
            this.socket = new Socket(InetAddress.getLocalHost(), 12345);
            this.out = new PrintWriter(this.socket.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));

        } catch (IOException e) {
            e.printStackTrace();
            ShutDown();
        }
    }

    public void ListenForMessages(){
        new Thread(() -> {
            while (active) {
                try {
                    String message = this.in.readLine();
                    System.out.println(message);

                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }).start();
    }

    public void WriteMessage(){
        new Thread(() -> {
            BufferedReader inReader = new BufferedReader(new InputStreamReader(System.in));
            while (active) {
                try {
                    String message = inReader.readLine();
                    HandleMessage(message);

                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }).start();
    }

    private void HandleMessage(String message) throws IOException {
        if (message == null)
            return;

        this.out.println(message);
        if (message.equals("q")) {
            ShutDown();
        }
    }

    private void ShutDown() throws IOException {
        try {
            active = false;
            if (this.in != null)
                this.in.close();
            if (this.out != null)
                this.out.close();
            if (this.socket != null && !this.socket.isClosed())
                this.socket.close();
        } catch (Exception ignored) {
        }
    }

    public static void main(String[] args) throws IOException {
        Client client = new Client();
        client.WriteMessage();
        client.ListenForMessages();
    }
}
