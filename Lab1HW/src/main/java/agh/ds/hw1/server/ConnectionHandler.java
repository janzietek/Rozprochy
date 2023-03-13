package agh.ds.hw1.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public class ConnectionHandler implements Runnable {
    private static final ArrayList<ConnectionHandler> ConnectionsList = new ArrayList<>();
    private Socket tcpSocket;
    private BufferedReader in;
    private PrintWriter out;
    private String nick;
    private DatagramSocket datagramSocket;

    public ConnectionHandler(Socket client, DatagramSocket datagramSocket) throws IOException {
        try {
            this.tcpSocket = client;
            this.out = new PrintWriter(client.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            this.datagramSocket = datagramSocket;
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
            Thread udpListener = UPDListener();
            udpListener.start();
            while (tcpSocket.isConnected()) {
                String message;
                message = this.in.readLine();
                HandleMessage(message);
            }
        } catch (IOException e) {
            try {
                boolean active = true;
                if (active) {
                    ShutDown();
                }
            } catch (IOException ignored) {
            }
        }
    }

    private Thread UPDListener() {
        return new Thread(() -> {
            try{
                byte[] receiveBuffer = new byte[1024];

                while(true) {
                    Arrays.fill(receiveBuffer, (byte)0);
                    DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                    datagramSocket.receive(receivePacket);
                    String message = new String(receivePacket.getData());
                    int senderPort = receivePacket.getPort();
                    Server.UsersUdpPorts.add(senderPort);
                    if (!message.equals("INIT"))
                        UDPBroadcast(message, receivePacket.getAddress(), senderPort);
                }
            }
            catch(Exception e){
                e.printStackTrace();
            }
            finally {
                if (datagramSocket != null) {
                    datagramSocket.close();
                }
            }
        });
    }

    private void UDPBroadcast(String message, InetAddress address, int senderPort) {
        if (message.isBlank())
            return;

        byte[] sendBuffer = message.getBytes(StandardCharsets.UTF_8);
        for (Integer port : Server.UsersUdpPorts) {
            if (port != senderPort) {
                DatagramPacket sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, address, port);
                try {
                    datagramSocket.send(sendPacket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void HandleMessage(String message) throws IOException {
        if (message.equals("l")) {
            ListActiveUsers();
        }
        else {
            TCPBroadcast(message);
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

    private void TCPBroadcast(String s) {
        for (ConnectionHandler handler : ConnectionsList) {
            if (!Objects.equals(handler.nick, this.nick)) {
                handler.out.println(this.nick + ": " + s);
            }
        }
    }

    private void ServerInformation(String s) {
        for (ConnectionHandler handler : ConnectionsList) {
                handler.out.println("SERVER: " + s);
        }
    }

    private void ShutDown() throws IOException {
        if (this.in != null)
            this.in.close();
        if (this.out != null)
            this.out.close();
        if (this.tcpSocket != null && !this.tcpSocket.isClosed())
            this.tcpSocket.close();
        RemoveConnection();
    }

    private void RemoveConnection() {
        System.out.println(nick + " left a chat");
        ConnectionsList.remove(this);
        ServerInformation(nick + " left a chat");
    }
}
