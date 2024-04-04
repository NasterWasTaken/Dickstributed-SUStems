package hgnn;

import java.rmi.*;

public interface QueueInterface extends Remote {
    public void addURL(String url) throws RemoteException;
    public void addVisited(String url) throws RemoteException;
    public int getQueueSize() throws RemoteException;
    public int getVisitedSize() throws RemoteException;
    public boolean inVisited(String url) throws RemoteException;
    public String removeURL() throws RemoteException;
    public String getFrontURL() throws InterruptedException, RemoteException;
    public String peekFront() throws RemoteException;
    public void updateQueue(String url) throws InterruptedException, RemoteException;
}
