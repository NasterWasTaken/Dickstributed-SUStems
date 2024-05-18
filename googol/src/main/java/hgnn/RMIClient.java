package hgnn;

import java.net.MalformedURLException;
import java.rmi.*;
import java.util.*;
import java.text.*;

/**
 * RMI Client class
 */
public class RMIClient implements Runnable {

    private RMIGatewayBase gateway;
    private ArrayList<Webpage> webOutcomes;

    /**
     * RMIClient constructor
     * @param gate
     */
    public RMIClient() {
        this.webOutcomes = new ArrayList<>();

        try {
            System.out.println("Client is turning on.");
            this.gateway = (RMIGatewayBase) Naming.lookup("rmi://192.168.1.65:1100/Gateway");
            System.out.println("The Client has connected to the Gateway.");
        } catch (RemoteException re) {
            System.out.println("[Client] A RemoteException occurred, unable to connect to the the Gateway.");
        } catch (NotBoundException nbe) {
            System.out.println("[Client] A NotBoundException occurred when trying to connect.");
        } catch (MalformedURLException mue) {
            System.out.println("[Client] A MalformedURLException occurred when trying to connect.");
        }
    }

    /**
     * Performs search through gateway
     * @param attempts
     * @param text
     */
    public ArrayList<Webpage> search(int attempts, String text) {

        if (attempts >= 5) {

            return null;
        }
        try {
            this.webOutcomes = this.gateway.search(removeSpecial(text.toLowerCase()), 0);
        } catch (RemoteException re) {

            System.out.println("[Client] Unable to successfully perform the search, attempting again.");
            search(attempts + 1, text);
        }
        return this.webOutcomes;
    }

    /**
     * Sends url to be indexed through gateway
     * @param attempts
     * @param text
     */
    public void indexURL(int attempts, String text) {

        if (attempts >= 5) {

            return;
        }
        try {

            new IndexManagement(gateway, text);
            System.out.println("URL properly indexed.");
        } catch (Exception e) {

            System.out.println("[Client] Unable to propertly index url, attempting again.");
            this.indexURL(attempts + 1, text);
        }
    }

    /**
     * Admin console priviliges for deeper access
     * @param attempts
     */
    public ArrayList<String> admin(int attempts) {

        if(attempts >= 5) {

            return null;
        }

        ArrayList<String> results = new ArrayList<>();
        try{

            ArrayList<String> tops = this.gateway.getTops();
            ArrayList<String> activeBarrels = this.gateway.getBarrels();
            System.out.println("TOP: \n");
            for (String top : tops) {

                System.out.println(top + "\n");
            }
            System.out.println("BARRELS: \n");
            for (String barrel : activeBarrels) {

                System.out.println(barrel + "\n");
            }
        }
        catch (RemoteException re){

            System.out.println("Unable to properly open the console, attempting again");
            admin(attempts + 1);
        }

        return results;
    }

    public ArrayList<String> tops() {

        try {
            ArrayList<String> results = this.gateway.getTops();
            return results;
        } catch (RemoteException re) {
            System.out.println("[Client] Error fetching top searches list.");
        }

        return null;
    }

    public ArrayList<String> barrelLists() {
        try {
            ArrayList<String> results = this.gateway.getBarrels();
            return results;
        } catch (RemoteException re) {
            System.out.println("[Client] Error fetching barrels list.");
        }

        return null;
    }

    /** 
     * Used to remove certain unwanted characters from String
     * @param txt
     * @return String
     */
    public static String removeSpecial(String txt) {

        String finalText = Normalizer.normalize(txt, Normalizer.Form.NFD);
        finalText = finalText.replaceAll("\\p{M}", "");
        finalText = finalText.replaceAll("[^a-zA-Z0-9 ]", "");
        return finalText;
    }

    /*public void updateTops(ArrayList<String> tops) {
        template.convertAndSend("/tops", "clear");
        for(String top: tops) {
            template.convertAndSend("/tops", top);
        }
    }

    public void updateBarrels(ArrayList<String> barrels) {
        template.convertAndSend("/barrels", "clear");
        for(String barrel: barrels) {
            template.convertAndSend("/barrels", barrel);
        }
    }*/

    public void run() {
        /*try {
            while(true) {
                updateTops(this.gateway.getTops());
                updateBarrels(this.gateway.getBarrels());
            }
        } catch (RemoteException re) {
            System.out.println("[Client] RemoteException occurred in threads.");
        }*/
        
    };
}

/**
 * Helper class to manage index
 */
class IndexManagement implements Runnable {

    private RMIGatewayBase gateway;
    private String key;

    IndexManagement(RMIGatewayBase gate, String key) {

        this.gateway = gate;
        this.key = key;
        new Thread(this, "IndexManagement").start();
    }

    public void run() {

        try {
            System.out.println("[Client] IndexManagement: New URL is being indexed ...");
            this.gateway.indexNewUrl(key);
        } catch (Exception e) {

            System.out.println("[Client] An exception was found in the Index Management.");
        }
    }
}
