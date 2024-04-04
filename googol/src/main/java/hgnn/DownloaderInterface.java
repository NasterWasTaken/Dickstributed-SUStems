package hgnn;

import java.rmi.*;

public interface DownloaderInterface extends Remote {
    public String parsePage(String url) throws RemoteException;
    
}