package hgnn;

import java.rmi.*;

public interface DownloaderInterface extends Remote {
    public void parsePage(String url) throws RemoteException;
    
}