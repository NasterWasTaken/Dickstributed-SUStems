package hgnn;

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
    public RMIClient(RMIGatewayBase gate) {

        this.gateway = gate;
        this.webOutcomes = new ArrayList<>();
    }

    public static void main(String[] args) {
        
        try {

            Scanner sc = new Scanner(System.in);
            Scanner inner = new Scanner(System.in);
            int operation;
            String temp;
            boolean on = true;
            System.out.println("Client is turning on.");
            RMIGatewayBase gate = (RMIGatewayBase) Naming.lookup("rmi://10.16.0.21:1100/Gateway");
            System.out.println("The Client has connected to the Gateway.");

            RMIClient client = new RMIClient(gate);

            while (on) {

                System.out.println("Which operation would you like to perform?");
                System.out.println("1 - Index a new URL.");
                System.out.println("2 - Perform a search.");
                System.out.println("3 - Access Admin functions.");
                System.out.println("4 - Exit.");

                operation = sc.nextInt();

                switch (operation) {
                    case 1:

                        System.out.print("Insert your url to index: ");
                        temp = inner.nextLine();
                        client.indexURL(0, temp);
                        break;
                    case 2:

                        System.out.print("Insert your search request: ");
                        temp = inner.nextLine();
                        client.search(0, temp);
                        break;
                    case 3:

                        client.admin(0);
                        break;

                    case 4:

                        on = false;
                        break;
                }

                
                
                try {

                    Thread.sleep((long) (Math.random() * 1000));
                } catch (InterruptedException e) {
                    System.out.println("[Error] Thread interrupted");
                }
            }

            sc.close();
            inner.close();

        } catch (RemoteException re) {

            System.out.println("[Client] A RemoteException occurred in main, unable to connect to the the Gateway.");
        } catch (Exception e) {

            System.out.println("[Client] An exceprtion " + e + " was found in main.");
        }
        
        
    }

    /**
     * Performs search through gateway
     * @param attempts
     * @param text
     */
    public void search(int attempts, String text) {

        if (attempts >= 5) {

            return;
        }
        try {

            this.webOutcomes = this.gateway.search(removeSpecial(text.toLowerCase()), 0);
            for (Webpage web : this.webOutcomes) {

                System.out.println("Title: " + web.getTitle() + " | URL: " + web.getUrl() + "\n");
            }

        } catch (RemoteException re) {

            System.out.println("[Client] Unable to successfully perform the search, attempting again.");
            search(attempts + 1, text);
        }
        return;
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
    public void admin(int attempts) {

        if(attempts >= 5) {

            return;
        }
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

    public void run() {
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
