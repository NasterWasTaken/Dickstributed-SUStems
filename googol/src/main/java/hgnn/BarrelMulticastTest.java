package hgnn;

import java.util.*;
import java.io.IOException;
import java.net.*;
import java.rmi.RemoteException;

public class BarrelMulticastTest implements Runnable {
    private static String MULTICAST_ADDRESS = "224.3.2.1";
    private static int PORT = 7561;
    private MulticastSocket socket;
    private InetAddress add;
    
    private Vector<Integer> curPack;
    private Vector<Vector<Integer>> lostPacks;

    public BarrelMulticastTest(MulticastSocket socket, InetAddress add) throws RemoteException {
        this.socket = socket;
        this.add = add;

        this.curPack = new Vector<>();
        this.lostPacks = new Vector<>();

        new Thread(this, "Barrel").start();
    }

    

    public void exit() {
        this.socket.close();
    }

    public static void main(String[] args) {

        try {
            MulticastSocket socket = new MulticastSocket(PORT);
            InetAddress add = InetAddress.getByName(MULTICAST_ADDRESS);

            // TODO: RMI goes here

            BarrelMulticastTest barrel = new BarrelMulticastTest(socket, add);

        } catch (Exception e) {
            // TODO: handle exception
        }
    }

    public void run() {
        new MulticastHandlerBarrel(this);
    }
}

class MulticastHandlerBarrel implements Runnable {

    private BarrelMulticastTest barrel;
    private static String MULTICAST_ADDRESS = "224.3.2.1";
    private static int PORT = 7561;

    public MulticastHandlerBarrel(BarrelMulticastTest barrel) {
        this.barrel = barrel;

        new Thread(this, "Receiver").start();
    }

    public void run() {
        MulticastSocket socket = null;
        try {
            socket = new MulticastSocket(PORT);
            socket.setSoTimeout(10000);
            InetSocketAddress add = new InetSocketAddress(MULTICAST_ADDRESS, PORT);
            NetworkInterface nwtI = NetworkInterface.getByName("multi");
            socket.joinGroup(add, nwtI);

            System.out.println("[Handler: Multicast] Listening...");
            while(true) {
                byte[] buf = new byte[256];
                DatagramPacket pack = new DatagramPacket(buf, buf.length);
                socket.receive(pack);

                String msg = new String(pack.getData(), 0, pack.getLength());
                new MessageHandlerBarrel(this.barrel, msg);
            }

        } catch (IOException e) {
            System.out.println("[Handler: Multicast] IOException occurred");
        } finally {
            socket.close();
            System.out.println("[Handler: Multicast] Offline...");
        }
    }
}

class MessageHandlerBarrel implements Runnable {
    
    private BarrelMulticastTest barrel;
    private String msg;

    public MessageHandlerBarrel(BarrelMulticastTest barrel, String msg) {
        this.barrel = barrel;
        this.msg = msg;

        new Thread(this, "MessageHandler").start();
    }

    public void run() {
        //type|index[whatever];packID|ID;
        try {
            StringTokenizer tok_1 = new StringTokenizer(this.msg, ";");
            StringTokenizer tok_2 = new StringTokenizer(tok_1.nextToken(), "|");

            if(tok_2.nextToken().equals("type")) {
                
                String type = tok_2.nextToken();
                tok_2 = new StringTokenizer(tok_1.nextToken(), "|");

                int packID = -1;
                if(tok_2.nextToken().equals("packID")) packID = Integer.parseInt(tok_2.nextToken());
                if(packID == -1) {
                    // TODO: do something
                    return;
                }

                if(type.equals("indexPage")) {
                    String url = new String();
                    String title = new String();
                    String body = new String();

                    while(tok_1.hasMoreTokens()) {
                        tok_2 = new StringTokenizer(tok_1.nextToken(), "|");
                        String part = tok_2.nextToken();

                        if(part.equals("url"))  url = tok_2.nextToken();
                        if(part.equals("title")) title = tok_2.nextToken();
                        if(part.equals("body")) body = tok_2.nextToken();
                    }

                    // TODO: colocar no barrel
                    System.out.printf("[Handler: Message] Indexed Page - %s | %s | %s\n", url, title, body);
                    
                }
                
                if(type.equals("indexWord")) {
                    String word = new String();
                    String url = new String();

                    while(tok_1.hasMoreTokens()) {
                        tok_2 = new StringTokenizer(tok_1.nextToken(), "|");
                        String part = tok_2.nextToken();

                        if(part.equals("word")) word = tok_2.nextToken();
                        if(part.equals("url")) url = tok_2.nextToken();
                    }

                    // TODO: colocar no barrel
                    System.out.printf("[Handler: Message] Indexed Word - %s | %s\n", word, url);
                }
            }


        } catch (Exception e) {
            System.out.println("[Handler: Message] Exception occurred while processing " + this.msg);
            e.printStackTrace();
        }
    }
}
