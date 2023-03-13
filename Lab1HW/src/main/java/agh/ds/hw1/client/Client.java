package agh.ds.hw1.client;

import agh.ds.hw1.utils.ASCIIArt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.Arrays;

public class Client {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    private boolean active = true;
    private boolean nickAccepted = false;

    private String nick;
    private int PORT = 12345;

    private DatagramSocket datagramSocket;
    private InetAddress address;

    private InetSocketAddress MulticastGroup;
    private MulticastSocket multicastSocket;

    public Client() throws IOException {
        try {
            datagramSocket = new DatagramSocket();
            address = InetAddress.getByName("localhost");

            socket = new Socket(InetAddress.getLocalHost(), PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            this.multicastSocket = new MulticastSocket(6789);
            InetAddress mcastaddr = InetAddress.getByName("228.5.6.7");
            MulticastGroup = new InetSocketAddress(mcastaddr, 6789);
            NetworkInterface netIf = NetworkInterface.getByName("bge0");

            multicastSocket.joinGroup(new InetSocketAddress(mcastaddr, 0), netIf);

            // Init UDP
            SendUPDMessage("INIT");
        } catch (IOException e) {
            e.printStackTrace();
            ShutDown();
        }
    }

    public void TCPListener(){
        new Thread(() -> {
            while (active) {
                try {
                    String message = this.in.readLine();
                    if (message.startsWith("NICK")) {
                        String[] temp = message.split(" ");
                        this.nick = temp[1];
                        System.out.println("Your nick <" + nick + "> was accepted");
                        this.nickAccepted = true;
                    }
                    else {
                        System.out.println(message);
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }).start();
    }

    public void UDPListener(){
        new Thread(() -> {
            byte[] receiveBuffer = new byte[1024];
            while (true) {
                try {
                    Arrays.fill(receiveBuffer, (byte) 0);
                    DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                    datagramSocket.receive(receivePacket);
                    String message = new String(receivePacket.getData());
                    System.out.println(message);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void MulticastListener(){
        new Thread(() -> {
            byte[] receiveBuffer = new byte[1024];
            while (true) {
                try {
                    if (this.multicastSocket != null) {
                        Arrays.fill(receiveBuffer, (byte) 0);
                        DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                        this.multicastSocket.receive(receivePacket);
                        String message = new String(receivePacket.getData());
                        if (!message.startsWith(this.nick)) {
                            System.out.println(message);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
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

        switch (message) {
            case "U" -> {
                SendUPDMessage(ASCIIArt.Art1);
                return;
            }
            case "M" -> {
                SendMulticastMessage(ASCIIArt.Art2);
                return;
            }
            default -> {
                WriteTCPMessage(message);
            }
        }

    }

    private void SendMulticastMessage(String message) {
        String str = this.nick + ": " +
                message +
                "\n";
        byte[] sendBuff = str.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendBuff, sendBuff.length, MulticastGroup);
        try {
            datagramSocket.send(sendPacket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void SendUPDMessage(String message) {
        byte[] sendBuff = message.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendBuff, sendBuff.length, address, PORT);
        try {
            datagramSocket.send(sendPacket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void WriteTCPMessage(String message) {
        out.println(message);
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
        client.TCPListener();
        client.UDPListener();
        client.MulticastListener();
    }
}
