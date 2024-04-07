package hgnn;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.HashSet;
import java.util.ArrayList;
public interface BarrelBase extends Remote{
    
    public void init(String name) throws RemoteException;
    public String getName() throws RemoteException;
    public ArrayList<Webpage> search (String key) throws RemoteException;
    public boolean ActiveChecker() throws RemoteException;
    public HashSet<String> lookupLinks(String key) throws RemoteException;

}