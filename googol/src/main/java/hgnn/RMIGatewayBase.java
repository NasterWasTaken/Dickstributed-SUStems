package hgnn;

import java.rmi.*;
import java.util.*;

public interface RMIGatewayBase extends Remote{
    public ArrayList<Webpage> search(String page, int attempts) throws RemoteException;
    public void indexNewUrl(String url) throws RemoteException;
    public HashSet<String> lookupURL(String key, int attempts) throws RemoteException;
    public int subBarrel(BarrelBase barrel, String barrelname);
    public ArrayList<String> getBarrels() throws RemoteException;
    public ArrayList<String> getTops() throws RemoteException;
    
}