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

/**
 * Downloader Class
 */
public class Downloader extends UnicastRemoteObject implements DownloaderInterface, Runnable  {
    private static String MULTICAST_ADDRESS = "224.3.2.1";
    private static int PORT = 7561;
    private MulticastSocket socket;
    private InetAddress add;
    
    private QueueInterface que;
    private Semaphore sem;
    private final int id;

    private int packID;
    private HashMap<Integer, DatagramPacket> packBuf;

    /**
     * Downloader constructor
     * @param que Queue Interface
     * @param id Downloader id
     * @param sem Semaphore
     * @param socket Multicast Socket
     * @param add InetAddress
     * @throws RemoteException
     */
    public Downloader(QueueInterface que, int id, Semaphore sem, MulticastSocket socket, InetAddress add) throws RemoteException {
        super();
        this.socket = socket;
        this.add = add;
        this.que = que;
        this.sem = sem;
        this.id = id;

        this.packBuf = new HashMap<>();

        new Thread(this, String.format("Downloader-%d", id)).start();
    }

    
    /** 
     * Returns the current downloader ID
     * @return int
     */
    public int getId() {
        return this.id;
    }

    
    /** 
     * Signal used to control information flow from Queue
     * @return boolean
     * @throws RemoteException
     */
    public boolean signal() throws RemoteException {
        return true;
    }

    
    /** 
     * Used to remove certain unwanted characters from String
     * @param str
     * @return String
     */
    public static String normalizeString(String str) {
        str = Normalizer.normalize(str, Normalizer.Form.NFD);
        str = str.replaceAll("\\p{M}", "");
        str = str.replaceAll("[^a-zA-Z0-9]", "");
        return str;
    }

    
    /** 
     * Sends page information through UDP Multicast channel to Barrels
     * @param url
     * @param title
     * @param body
     */
    public void sendPageMulticast(String url, String title, String body) {
        if(url.length() < 1 || title.length() < 1 || body.length() < 1) return;

        try {
            // type|indexPage;packID|id;url|url;title|title;body|body
            String msg = String.format("type|indexPage;packID|%d;url|%s;title|%s;body|%s", this.packID, url, title, body);

            byte[] buf = msg.getBytes();
            DatagramPacket pack = new DatagramPacket(buf, buf.length, add, PORT);

            this.packBuf.put(this.packID, pack);
            socket.send(pack);
            this.packID++;

        } catch (IOException e) {
            System.out.printf("[%s] Error Sending Word\n", Thread.currentThread().getName());
        }
    }

    
    /** 
     * Sends word with url to be indexed in Barrels through UDP Multicast
     * @param word
     * @param url
     */
    public void sendWordMulticast(String word, String url) {
        if(word.length() < 1 || url.length() < 1) return;

        try {
            // type|indexWord;packID|id;word|word;url|url
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

    
    /** 
     * Resends lost UDP Multicast packet to Barrel when asked for
     * @param packID
     */
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

    
    /** 
     * Parses through the information in the url's respective HTML Elememts
     * @param url
     * @throws RemoteException
     */
    public void parsePage(String url) throws RemoteException {
        try {
            sem.acquire();
            Document doc = Jsoup.connect(url).ignoreHttpErrors(true).get();
            StringTokenizer tok = new StringTokenizer(doc.text(), " ,;.:ºª?!+|\n\t");

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
            
            QueueInterface que = null;
            if(Integer.parseInt(args[1]) == 0) que = (QueueInterface) LocateRegistry.getRegistry(1099).lookup("Queue");
            else if(Integer.parseInt(args[1]) == 1) que = (QueueInterface) Naming.lookup("rmi://10.16.0.21:1099/Queue");
            else System.out.println("[Error] Invalid RMI configuration");
            System.out.println("[Downloader] Connected to Queue RMI Server!");

            for(int i = 1; i <= numDownloaders; ++i) {
                new Downloader(que, i, sem, socket, add);
            }

        } catch (Exception e) {
            System.out.println("[Error] Some Exception in main: " + e);
            e.printStackTrace();
        }
    }

    public void run() {
        new MulticastHandler(this);
        boolean first = true;
        
        try {
        
            while(true) {
                String url = que.getFrontURL();
                
                if(que.inVisited(url)) {
                    url = que.removeURL();
                    continue;
                }

                if(first) {
                    parsePage(url);
                    first = false;
                    continue;
                }

                if(que.Signal()) parsePage(url);
            } 
        } catch (RemoteException e) {
            System.out.printf("[%s] Error RemoteException\n", Thread.currentThread().getName());
        } catch (InterruptedException e) {
            System.out.printf("[%s] Error InterrruptedException\n", Thread.currentThread().getName());
        }
        
        
    }
}

/**
 * Helper class to run a thread used for listening in the Multicast channels
 * Creates new instance of @see{@link MessageHandler} when it receives a message
 */
class MulticastHandler implements Runnable {

    private Downloader dl;
    private static String MULTICAST_ADDRESS = "224.3.2.1";
    private static int PORT = 7561;

    public MulticastHandler(Downloader dl) {
        new Thread(this, "Receiver").start();
        this.dl = dl;
    }

    public void run() {
        MulticastSocket socket = null;

        try {
            
            socket = new MulticastSocket(PORT);
            InetSocketAddress add = new InetSocketAddress(MULTICAST_ADDRESS, PORT);
            NetworkInterface netI = NetworkInterface.getByName("multi");
            socket.joinGroup(add, netI);

            System.err.println("[Handler: Multicast] Listening...");

            while(true) {
                byte[] buf = new byte[256];
                DatagramPacket pack = new DatagramPacket(buf, buf.length);
                socket.receive(pack);

                String msg = new String(pack.getData(), 0, pack.getLength());

                new MessageHandler(this.dl, msg);
            }

        } catch (IOException e) {
            System.out.println("[Handler: Multicast] IOException occurred");
        } finally {
            socket.close();
            System.out.println("[Handler: Multicast] Offline...");
        }
    }
}

/**
 * Helper class to process a message's content
 */
class MessageHandler implements Runnable {
    private Downloader dl;
    private String msg;

    public MessageHandler(Downloader dl, String msg) {
        this.dl = dl;
        this.msg = msg;

        new Thread(this, "MessageHandler").start();
    }

    public void run() {
        //type|resend;packID|ID
        try {
            
            StringTokenizer tok_1 = new StringTokenizer(this.msg, ";");

            StringTokenizer tok_2 = new StringTokenizer(tok_1.nextToken(), "|");

            if(tok_2.nextToken().equals("type")) {
                if(tok_2.nextToken().equals("resend")) {
                    
                    tok_2 = new StringTokenizer(tok_1.nextToken(), "|");
                    if(tok_2.nextToken().equals("packID")) {
                        
                        int packID = Integer.parseInt(tok_2.nextToken());
                        dl.resend(packID);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("[Handler: Message] Exception occurred while processing " + this.msg);
            e.printStackTrace();
        }
    }
}