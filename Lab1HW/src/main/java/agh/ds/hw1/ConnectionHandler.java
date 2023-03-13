package agh.ds.hw1;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Objects;

public class ConnectionHandler implements Runnable {
    private static final ArrayList<ConnectionHandler> ConnectionsList = new ArrayList<>();
    private Socket client;
    private BufferedReader in;
    private PrintWriter out;
    private String nick;
    private boolean active = true;

    public ConnectionHandler(Socket client) throws IOException {
        try {
            this.client = client;
            this.out = new PrintWriter(client.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(client.getInputStream()));

        } catch (IOException e) {
            e.printStackTrace();
            ShutDown();
        }
    }

    private void AskForNickname() throws IOException {
        try {
            this.out.println("SERVER: Please provide your nickname: ");

            String temporaryNick = this.in.readLine();
            String validationMessage = ValidateNick(temporaryNick);
            if (validationMessage.equals("No Errors")) {
                this.nick = temporaryNick;
                ConnectionsList.add(this);
                System.out.println(nick + " entered a chat");
                ServerInformation(this.nick + " entered a chat");
            }
            else {
                this.out.println("SERVER: " + validationMessage);
                AskForNickname();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
            ShutDown();
        }
    }

    private String ValidateNick(String nick) {
        String[] temp = nick.split(" ");
        if (temp.length != 1) {
            return "the nickname should have no whitespaces";
        }
        for (ConnectionHandler handler : ConnectionsList) {
            if (handler.nick.equals(temp[0])) {
                return "nick already taken";
            }
        }
        return "No Errors";
    }

    @Override
    public void run() {
        try {
            AskForNickname();
            while (client.isConnected()) {
                String message;
                message = this.in.readLine();
                HandleMessage(message);
            }
        } catch (IOException e) {
            try {
                if (active) {
                    ShutDown();
                }
            } catch (IOException ignored) {
            }
        }
    }

    private void HandleMessage(String message) throws IOException {
        if (message.equals("q")) {
            active = false;
            ShutDown();
        }
        else if (message.equals("l")) {
            ListActiveUsers();
        }
        else {
            BroadcastToAll(message);
        }
    }

    private void ListActiveUsers() {
        StringBuilder builder = new StringBuilder();
        builder.append("\nActive Users: \n");
        for (ConnectionHandler handler : ConnectionsList) {
             builder.append(handler.nick + "\n");
        }
        this.out.println(builder.toString());
    }

    private void BroadcastToAll(String s) {
        for (ConnectionHandler handler : ConnectionsList) {
            if (!Objects.equals(handler.nick, this.nick)) {
                handler.out.println(this.nick + ": " + s);
            }
        }
    }

    private void ServerInformation(String s) {
        for (ConnectionHandler handler : ConnectionsList) {
            if (!Objects.equals(handler.nick, this.nick)) {
                handler.out.println("SERVER: " + s);
            }
        }
    }

    private void ShutDown() throws IOException {
        if (this.in != null)
            this.in.close();
        if (this.out != null)
            this.out.close();
        if (this.client != null && !this.client.isClosed())
            this.client.close();
        RemoveConnection();
    }

    private void RemoveConnection() {
        System.out.println(nick + " left a chat");
        ConnectionsList.remove(this);
        ServerInformation(nick + " left a chat");
    }
}
