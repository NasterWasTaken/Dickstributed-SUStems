package hgnn;

import java.io.IOException;
import java.net.*;
import java.rmi.*;
import java.rmi.registry.*;
import java.rmi.server.*;
import java.text.Normalizer;
import java.util.*;
import java.util.concurrent.Semaphore;

import org.jsoup.*;
import org.jsoup.nodes.*;
import org.jsoup.select.*;

public class Downloader extends UnicastRemoteObject implements DownloaderInterface, Runnable  {
    private static String MULTICAST_ADDRESS = "224.3.2.1";
    private static int PORT = 4321;
    private MulticastSocket socket;
    private InetAddress add;
    
    private QueueInterface que;
    private Semaphore sem;
    private final int id;

    private int packID;
    private HashMap<Integer, DatagramPacket> packBuf;

    Downloader(QueueInterface que, int id, Semaphore sem, MulticastSocket socket, InetAddress add) throws RemoteException {
        super();
        this.socket = socket;
        this.add = add;
        this.que = que;
        this.sem = sem;
        this.id = id;

        this.packBuf = new HashMap<>();

        new Thread(this, String.format("Downloader-%d", id)).start();
    }

    public int getId() {
        return this.id;
    }

    public static String normalizeString(String str) {
        str = Normalizer.normalize(str, Normalizer.Form.NFD);
        str = str.replaceAll("\\p{M}", "");
        str = str.replaceAll("[^a-zA-Z0-9]", "");
        return str;
    }

    public void sendPageMulticast(String url, String title, String body) {
        if(url.length() < 1 || title.length() < 1 || body.length() < 1) return;

        try {
            // TODO: change template
            String msg = "template page\n";

            byte[] buf = msg.getBytes();
            DatagramPacket pack = new DatagramPacket(buf, buf.length, add, PORT);

            this.packBuf.put(this.packID, pack);
            socket.send(pack);
            this.packID++;

        } catch (IOException e) {
            System.out.printf("[%s] Error Sending Word\n", Thread.currentThread().getName());
        }
    }

    public void sendWordMulticast(String word, String url) {
        if(word.length() < 1 || url.length() < 1) return;

        try {
            // TODO: change template
            String msg = "template word\n";

            byte[] buf = msg.getBytes();
            DatagramPacket pack = new DatagramPacket(buf, buf.length, add, PORT);

            this.packBuf.put(this.packID, pack);
            socket.send(pack);
            this.packID++;
            
        } catch (IOException e) {
            System.out.printf("[%s] Error Sending Word\n", Thread.currentThread().getName());
        }
    }

    public void sendURLMulticast(String url) {
        if(url.length() < 1) return;

        try {
            // TODO: change template
            String msg = "template url\n";

            byte[] buf = msg.getBytes();
            DatagramPacket pack = new DatagramPacket(buf, buf.length, add, PORT);

            this.packBuf.put(this.packID, pack);
            socket.send(pack);
            this.packID++;

        } catch (IOException e) {
            System.out.printf("[%s] Error Sending Word\n", Thread.currentThread().getName());
        }
    }

    public void resend(int packID) {

        try {
            
            if(this.packBuf.containsKey(packID)) {
                socket.send(this.packBuf.get(packID));

                return;
            }

            String msg = "resend dont exist\n";

            byte[] buf = msg.getBytes();
            DatagramPacket pack = new DatagramPacket(buf, buf.length, add, PORT);
            socket.send(pack);

        } catch (IOException e) {
            System.out.printf("[%s] Error Sending Word\n", Thread.currentThread().getName());
        }
    }

    public void parsePage(String url) throws RemoteException {
        try {
            sem.acquire();
            Document doc = Jsoup.connect(url).ignoreHttpErrors(true).get();
            StringTokenizer tok = new StringTokenizer(doc.text(), " ,;.:ºª?!|\n\t");

            String title = normalizeString(doc.title());
            if(title.length() < 1) title = "Untitled Page";

            String body = normalizeString(doc.text());
            body = body.replaceAll("|", "");
            body = body.replaceAll(";", "");
            if(body.length() > 150) {
                body = body.substring(0, 150);
                body += "...";
            }

            sendPageMulticast(url, title, body);

            while(tok.hasMoreElements()) {
                String word = normalizeString(tok.nextToken().toLowerCase());
                sendWordMulticast(word, url);
            }
                
            Elements links = doc.select("a[href]");
            for (Element link : links) {
                if(que.inVisited(link.attr("abs:href"))) continue;
                
                if(link.attr("abs:href").length() < 100) {
                    String temp = link.attr("abs:href").replaceAll("/", "");
                    int depth = link.attr("abs:href").length() - temp.length();

                    if(depth > 0 && depth < 7) {
                        que.addURL(link.attr("abs:href"));
                        sendURLMulticast(link.attr("abs:href"));
                    }
                } 
            }

            System.out.printf("Thread: %s | Queue size: %d | Visited: %d | url: %s\n",
                                    Thread.currentThread().getName(), this.que.getQueueSize(), this.que.getVisitedSize(), url);

            que.addVisited(url);
            que.removeURL();

            sem.release();
        } catch (IOException e) {
            System.out.printf("[%s] Error Parsing Page: %s\n", Thread.currentThread().getName(), url);
        } catch (InterruptedException e) {
            System.out.printf("[%s] Thread interrupted while Parsing Page: %s\n", Thread.currentThread().getName(), url);
        }

    }

    public static void main(String[] args) {
        int numDownloaders = Integer.parseInt(args[0]);
        Semaphore sem = new Semaphore(1, true);

        try {
            
            System.out.println("[Downloader] Booting all Downloaders...");
            MulticastSocket socket = new MulticastSocket(PORT);
            InetAddress add = InetAddress.getByName(MULTICAST_ADDRESS);

            QueueInterface que = (QueueInterface) LocateRegistry.getRegistry(9876).lookup("Queue");
            System.out.println("[Downloader] Connected to Queue RMI Server!");

            for(int i = 1; i <= numDownloaders; ++i) {
                new Downloader(que, i, sem, socket, add);
            }

        } catch (Exception e) {
            System.out.println("[Error] Some Exception in main: " + e);
        }
    }

    public void run() {
        
        try {
           String url = que.peekFront();
        
            while(true) {
                
                if(que.inVisited(url)) {
                    url = que.removeURL();
                    continue;
                }

                parsePage(url);
                url = que.getFrontURL();
            } 
        } catch (RemoteException e) {
            System.out.printf("[%s] Error RemoteException\n", Thread.currentThread().getName());
        } catch (InterruptedException e) {
            System.out.printf("[%s] Error InterrruptedException\n", Thread.currentThread().getName());
        }
        
        
    }
}
