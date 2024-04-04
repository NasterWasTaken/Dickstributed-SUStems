package hgnn;

import java.io.IOException;
import java.net.*;
import java.rmi.*;
import java.rmi.registry.*;
import java.rmi.server.*;
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
    

    Downloader(QueueInterface que, int id, Semaphore sem, MulticastSocket socket, InetAddress add) throws RemoteException {
        super();
        this.socket = socket;
        this.add = add;
        this.que = que;
        this.sem = sem;

        new Thread(this, String.format("Downloader-%d", id)).start();
    }

    public String parsePage(String url) throws RemoteException {
        try {
            sem.acquire();
            Document doc = Jsoup.connect(url).ignoreHttpErrors(true).get();
                
            Elements links = doc.select("a[href]");
            for (Element link : links) {
                if(que.inVisited(link.attr("abs:href"))) continue;
                
                if(link.attr("abs:href").length() < 100) {
                    String temp = link.attr("abs:href").replaceAll("/", "");
                    int depth = link.attr("abs:href").length() - temp.length();

                    if(depth > 0 && depth < 9) que.addURL(link.attr("abs:href"));
                } 
            }
            
            que.addVisited(url);
            url = que.removeURL();

            sem.release();
        } catch (IOException e) {
            System.out.printf("[%s] Error Parsing Page: %s\n", Thread.currentThread().getName(), url);
        } catch (InterruptedException e) {
            System.out.printf("[%s] Thread interrupted while Parsing Page: %s\n", Thread.currentThread().getName(), url);
        }

        return url;
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
        
            while(que.getQueueSize() != 0) {
                
                if(que.inVisited(url)) {
                    url = que.removeURL();
                    continue;
                }

                url = parsePage(url);
                System.out.printf("Thread: %s | Queue size: %d | Visited: %d | url: %s\n",
                                    Thread.currentThread().getName(), this.que.getQueueSize(), this.que.getVisitedSize(), this.que.peekFront());
                
                
            } 
        } catch (RemoteException e) {
            System.out.printf("[%s] Error RemoteException\n", Thread.currentThread().getName());
        }
        
        
    }
}
