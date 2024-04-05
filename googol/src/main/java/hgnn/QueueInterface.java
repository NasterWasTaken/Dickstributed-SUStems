package hgnn;

import java.rmi.*;

public interface QueueInterface extends Remote {
    public boolean addURL(String url) throws RemoteException;
    public boolean addVisited(String url) throws RemoteException;
    public int getQueueSize() throws RemoteException;
    public int getVisitedSize() throws RemoteException;
    public boolean inVisited(String url) throws RemoteException;
    public String removeURL() throws RemoteException;
    public String getFrontURL() throws InterruptedException, RemoteException;
    public String peekFront() throws RemoteException;
}
