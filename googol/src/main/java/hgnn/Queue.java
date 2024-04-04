package hgnn;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.rmi.*;
import java.rmi.registry.*;
import java.rmi.server.*;

public class Queue extends UnicastRemoteObject implements QueueInterface {
    private ArrayBlockingQueue<String> que;
    private HashSet<String> visited;

    public Queue(int size) throws RemoteException {
        super();
        this.que = new ArrayBlockingQueue<>(size);
        this.visited = new HashSet<String>();
    }

    public void addURL(String url) {
        this.que.add(url);
    }

    public void addVisited(String url) {
        this.visited.add(url);
    }

    public int getQueueSize() {
        return this.que.size();
    }

    public int getVisitedSize() {
        return this.visited.size();
    }

    public boolean inVisited(String url) {
        return visited.contains(url);
    }

    public String removeURL() {
        return que.poll();
    }

    public String getFrontURL() throws InterruptedException {
        return this.que.take();
    }

    public String peekFront() {
        return this.que.peek();
    }

    public void updateQueue(String url) throws InterruptedException {
        try {

        } catch (Exception e) {
            System.out.println("[Error] Remote Exception in update");
        }
    }

    public static void main(String args[]) {
        try {
            
            System.out.println("[Queue] Booting Queue...");
            QueueInterface queI = new Queue(100000);
            queI.addURL("https://youtube.com");

            Registry reg = LocateRegistry.createRegistry(9876);
            reg.rebind("Queue", queI);
            System.out.println("[Queue] Successfully Booted and Started RMI Server!");

            

            while(true) {
                // TODO: receber url de gateway/Downloader, meter na fila

            }

        } catch (IOException e) {
            System.out.println("[Error] IO: " + e.getMessage());
        }
    }
}