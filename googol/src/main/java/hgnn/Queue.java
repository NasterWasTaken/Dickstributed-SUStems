package hgnn;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.rmi.*;
import java.rmi.registry.*;
import java.rmi.server.*;

/**
 * Queue class
 */
public class Queue extends UnicastRemoteObject implements QueueInterface {
    public ArrayBlockingQueue<String> que;
    private HashSet<String> visited;

    /**
     * Queue constructor
     * @param size Queue initial size
     * @throws RemoteException
     */
    public Queue(int size) throws RemoteException {
        super();
        this.que = new ArrayBlockingQueue<>(size);
        this.visited = new HashSet<String>();
    }

    
    /** 
     * Adds a url to the queue
     * @param url url
     * @return boolean
     */
    public boolean addURL(String url) {
        return que.add(url);
    }

    /** 
     * Adds a url to the visited list
     * @param url url
     * @return boolean
     */
    public boolean addVisited(String url) {
        return this.visited.add(url);
    }
    
    
    /** 
     * Returns Queue size
     * @return int
     */
    public int getQueueSize() {
        return this.que.size();
    }

    
    /** 
     * Returns visited list size
     * @return int
     */
    public int getVisitedSize() {
        return this.visited.size();
    }

    
    /** 
     * Checks if url has already been visited
     * @param url url
     * @return boolean
     */
    public boolean inVisited(String url) {
        return visited.contains(url);
    }

    
    /** 
     * Removes and returns the url at the front of the queue
     * @return String
     */
    public String removeURL() {
        return que.poll();
    }

    
    /** 
     * Removes and returns the url at the front of the queue
     * If there is none, waits
     * @return String
     * @throws InterruptedException
     */
    public String getFrontURL() throws InterruptedException {
        return this.que.take();
    }

    /** 
     * Returns the url at the front of the queue
     * @return String
     */
    public String peekFront() {
        return this.que.peek();
    }

    
    /** 
     * Signals to the Downloaders that there are elements in the queue
     * @return boolean
     * @throws RemoteException
     */
    public boolean Signal() throws RemoteException {
        if(this.que.size() == 0) return false;

        return true;
    }

    public static void main(String args[]) {
        try {
            
            System.out.println("[Queue] Booting Queue...");
            QueueInterface queI = new Queue(100000);

            Registry reg = LocateRegistry.createRegistry(1099);
            reg.rebind("Queue", queI);
            System.out.println("[Queue] Successfully Booted and Started RMI Server!");

            //queI.addURL("http://youtube.com");
            
            while(true) {}

        } catch (IOException e) {
            System.out.println("[Error] IO: " + e.getMessage());
        }
    }
}