package hgnn;

import java.util.*;
import java.io.BufferedWriter;
import java.io.*;
import java.net.*;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;

/**
 * Barrel class
 */
public class Barrel implements BarrelBase, Runnable, Serializable {

    private HashSet<Webpage> webpages;
    private HashMap<String, HashSet<String>> index;
    private HashMap<String, HashSet<String>> links;

    private String SAVE;
    private String name;
    
    private static int MAX_DOWNS = 3;
    private Vector<Integer> curPack;
    private Vector<Vector<Integer>> lostPacks;

    /**
     * Barrel constructor
    * @throws RemoteException
    */
    public Barrel() throws RemoteException {
        this.webpages = new HashSet<>();
        this.index = new HashMap<>();
        this.links = new HashMap<>();

        this.curPack = new Vector<>();
        this.lostPacks = new Vector<>();

        for (int i = 0; i < MAX_DOWNS; i++) {

            this.curPack.add(i, -1);
            this.lostPacks.insertElementAt(new Vector<Integer>(), i);
        }

        new Thread(this, "Barrel").start();
    }

    
    /** 
     * Starts the database from file
     * @param name
     * @throws RemoteException
     */
    public void init(String name) throws RemoteException{

        this.SAVE = "./data/save" + name + ".txt";
        this.readSave(); 
    }

    
    /** 
     * Returns barrel name
     * @return String
     * @throws RemoteException
     */
    public String getName() throws RemoteException{
        return this.name;
    }

    /**
     * Writes to save file
     */
    public void writeSave(){

        try{

            FileWriter writer = new FileWriter(this.SAVE, false);
            BufferedWriter buffer = new BufferedWriter(writer);

            for(Webpage web : this.webpages){

                buffer.write("WEB" + web.getUrl() + "|" + web.getTitle() + "|" + web.getContent() + "|");
                buffer.write("\n");
            }

            for(String key : this.index.keySet()){

                buffer.write("INDEX|" + key + "|");
                HashSet<String> keys = this.index.get(key);
                for(String url : keys){

                    buffer.write(url + "|");
                }
                buffer.write("\n");
            }

            for (String key : this.links.keySet()){

                buffer.write("LINK| " + key + "|");

                HashSet<String> keys = this.links.get(key);
                for (String s : keys){

                    buffer.write(s + "|");
                }
                buffer.write("\n");
            }

            for (int i = 0; i <lostPacks.size(); i++){

                if(lostPacks.get(i).size() > 0){

                    buffer.write("LOST_PACKETS|" + i + "|");
                    for(int pack : lostPacks.get(i)){

                        buffer.write(pack + "|");
                    }

                    buffer.write("\n");
                }
            }

            buffer.close();
            writer.close();
        }
        catch (IOException io){

            System.out.println("[Barrel] IOException writing the save file");
        }
    }

    /**
     * Reads from save file
     */
    public void readSave(){

        File file = new File(this.SAVE);
        if(file.exists() && file.isFile()){

            try{

                FileReader reader = new FileReader(file);
                BufferedReader buffer = new BufferedReader(reader);

                String line;
                while ((line = buffer.readLine()) != null){

                    lineParser(line);                    
                }
                reader.close();
                buffer.close();
            }
            catch (FileNotFoundException fn){

                System.out.println("[Barrel] Error: File not found.");
            }
            catch (IOException ie){

                System.out.println("[Barrel] Error: Unable to read file.");
            }
            catch (NumberFormatException | IndexOutOfBoundsException ex){

                System.out.println("[Barrel] Error: Unable to properly parse " + ex);
            }
            catch (Exception e){

               System.out.println("[Barrel] Exception on reader"); 
            }
        }

        else {

            System.out.println("[Barrel] Error: Database not found ... creating new file");
            System.out.println(file);
        }
    }

    
    /** 
     * Adds a web page to the barrel
     * @param url page url
     * @param title page title
     * @param content page content
     */
    synchronized public void addWebpage(String url, String title, String content){

        for(Webpage web: this.webpages){

            if(web.getUrl().equals(url)){
                
                return;
            }
        }
        this.webpages.add(new Webpage(url, title, content));    
    }

    
    /** 
     * Returns the number of webpages associated to a word
     * @param web
     * @return int
     */
    synchronized public int getWebNumber(Webpage web){

        if(this.links.containsKey(web.getUrl())){

            return this.links.get(web.getUrl()).size();
        }
        return -1;
    }

    
    /** 
     * Indexes word in the barrel and a corresponding url
     * @param url
     * @param content
     */
    synchronized public void addContent(String url, String content){

        if(this.index.containsKey(content)){

            this.index.get(content).add(url);
        }
        else{

            HashSet<String> urls = new HashSet<>();
            urls.add(url);
            this.index.put(content, urls);
        }
    }

    
    /** 
     * Adds info from lost packed
     * @param downID
     * @param packID
     */
    synchronized public void addLostPacket(int downID, int packID){

        if(!this.lostPacks.get(downID).contains(packID)){

            this.lostPacks.get(downID).add(packID);
        }
    }

    
    /** 
     * Parses through lines from save file
     * @param line
     * @throws NumberFormatException
     * @throws IndexOutOfBoundsException
     */
    public void lineParser(String line) throws NumberFormatException, IndexOutOfBoundsException{

        String s[];
        s = line.split("\\|");

        if(s[0].equals("WEB")){
            this.addWebpage(s[1], s[2], s[3]);
            
        }
        if(s[0].equals("INDEX")){

            for (int i = 2; i < s.length; i++) {
                
                this.addContent(s[i], s[1]);
            }
        }
        if(s[0].equals("LOST_PACKETS")){

            for(int i = 2; i < s.length; i++){

                this.addLostPacket(Integer.parseInt(s[1]), Integer.parseInt(s[i]));
            }
        }
        else{
            System.out.println("[SAVE] Error: Lines not properly formatted." + line);
        }
    }

    public boolean ActiveChecker() throws RemoteException {

        return true;
    }

    public HashSet<String> lookupLinks(String key) throws RemoteException {

        System.out.println("Looking up links: " + key);
        if(this.links.containsKey(key)){

            return this.links.get(key);
        }
        return new HashSet<>();
    }

    public ArrayList<Webpage> search (String key) throws RemoteException {

        System.out.println("Searching: " + key);
        HashSet<String> obtainedUrls = new HashSet<>();

        StringTokenizer tokenizer = new StringTokenizer(key);
        
        if(!tokenizer.hasMoreTokens()){

            return new ArrayList<>();
        }

        String token = tokenizer.nextToken();

        if(this.index.containsKey(token)){

            obtainedUrls.addAll(this.index.get(token));

            while(tokenizer.hasMoreTokens()){

                token = tokenizer.nextToken();
                if(this.index.containsKey(token)){

                    HashSet<String> tempObtained = new HashSet<>(this.index.get(token));
                    obtainedUrls.retainAll(tempObtained);
                }
                else{

                    return new ArrayList<>();
                }
            }

        }

        HashSet<Webpage> outcome = new HashSet<>();
        for(String url: obtainedUrls){

            for(Webpage web : this.webpages){
                
                if(web.getUrl().equals(url)){

                    outcome.add(web);
                }
            }
        }

        Comparator<WebpageTotal> comp = new Comparator<WebpageTotal>(){

            @Override
            public int compare(WebpageTotal web1, WebpageTotal web2){

                return Integer.compare(web2.getTotalUrl(), web1.getTotalUrl());
            }
        };

        ArrayList<WebpageTotal> sortOutcome = new ArrayList<>();
        for(Webpage web: outcome){

            sortOutcome.add(new WebpageTotal(web, this.getWebNumber(web)));
        }

        sortOutcome.sort(comp);

        ArrayList<Webpage> finalArray = new ArrayList<>();
        for(WebpageTotal webTotal: sortOutcome){

            finalArray.add(webTotal.getWeb());
        }

        return finalArray;
    }

    public static void main(String[] args) {

        if (args.length == 0) {
    
            System.out.println("[Error] Incorrect command. Exiting...");
            System.exit(0);
        }

        try {
            System.out.println("[Barrel] Booting up Barrel...");

            RMIGatewayBase rGate = null;
            if(Integer.parseInt(args[1]) == 0) rGate = (RMIGatewayBase) LocateRegistry.getRegistry(1100).lookup("Gateway");
            else if(Integer.parseInt(args[1]) == 1) rGate = (RMIGatewayBase) Naming.lookup("rmi://192.168.1.98:1100/Gateway");
            else System.out.println("[Error] Invalid RMI configuration");
            System.out.println("[Barrel] Connected to Gateway RMI server!");

            BarrelBase barrel = new Barrel();

            if(rGate.subBarrel((BarrelBase) barrel, args[0]) == -1) {
                System.out.println("[Barrel]" + args[0] + " server already active, shutting down...");
                System.exit(0);
            }

            barrel.init(args[0]);

        } catch (Exception e) {
            System.out.println("[Error] Some Exception in main: " + e);
            e.printStackTrace();
        }
    }

    public void run() {
        new MulticastHandlerBarrel(this);
    }
}

/**
 * Helper class to process the information that comes from the Downloaders
 */
class MulticastHandlerBarrel implements Runnable {

    private Barrel barrel;
    private static String MULTICAST_ADDRESS = "224.3.2.1";
    private static int PORT = 7561;

    public MulticastHandlerBarrel(Barrel barrel) {
        this.barrel = barrel;

        new Thread(this, "Receiver").start();
    }

    public void run() {
        MulticastSocket socket = null;
        try {
            socket = new MulticastSocket(PORT);
            InetSocketAddress add = new InetSocketAddress(MULTICAST_ADDRESS, PORT);
            NetworkInterface nwtI = NetworkInterface.getByName("multi");
            socket.joinGroup(add, nwtI);

            System.out.println("[Handler: Multicast] Listening...");
            while(true) {
                byte[] buf = new byte[256];
                DatagramPacket pack = new DatagramPacket(buf, buf.length);
                socket.receive(pack);

                String msg = new String(pack.getData(), 0, pack.getLength());
                new MessageHandlerBarrel(this.barrel, msg);
            }

        } catch (IOException e) {
            System.out.println("[Handler: Multicast] IOException occurred");
        } finally {
            socket.close();
            System.out.println("[Handler: Multicast] Offline...");
        }
    }
}

/**
 * Helper class to process the messages from the downloaders
 */
class MessageHandlerBarrel implements Runnable {
    
    private Barrel barrel;
    private String msg;

    public MessageHandlerBarrel(Barrel barrel, String msg) {
        this.barrel = barrel;
        this.msg = msg;

        new Thread(this, "MessageHandler").start();
    }

    public void run() {
        //type|index[whatever];packID|ID;
        try {
            StringTokenizer tok_1 = new StringTokenizer(this.msg, ";");
            StringTokenizer tok_2 = new StringTokenizer(tok_1.nextToken(), "|");

            if(tok_2.nextToken().equals("type")) {
                
                String type = tok_2.nextToken();
                tok_2 = new StringTokenizer(tok_1.nextToken(), "|");

                int packID = -1;
                if(tok_2.nextToken().equals("packID")) packID = Integer.parseInt(tok_2.nextToken());
                if(packID == -1) {
                    System.out.println("[Handler: Message] Invalid Packet ID");
                    return;
                }

                if(type.equals("indexPage")) {
                    String url = new String();
                    String title = new String();
                    String body = new String();

                    while(tok_1.hasMoreTokens()) {
                        tok_2 = new StringTokenizer(tok_1.nextToken(), "|");
                        String part = tok_2.nextToken();

                        if(part.equals("url"))  url = tok_2.nextToken();
                        if(part.equals("title")) title = tok_2.nextToken();
                        if(part.equals("body")) body = tok_2.nextToken();
                    }

                    this.barrel.addWebpage(url, title, body);
                    System.out.printf("[Handler: Message] Indexed Page - %s | %s | %s\n", url, title, body);
                    
                }
                
                if(type.equals("indexWord")) {
                    String word = new String();
                    String url = new String();

                    while(tok_1.hasMoreTokens()) {
                        tok_2 = new StringTokenizer(tok_1.nextToken(), "|");
                        String part = tok_2.nextToken();

                        if(part.equals("word")) word = tok_2.nextToken();
                        if(part.equals("url")) url = tok_2.nextToken();
                    }

                    this.barrel.addContent(url, word);
                    System.out.printf("[Handler: Message] Indexed Word - %s | %s\n", word, url);
                }
            }


        } catch (Exception e) {
            System.out.println("[Handler: Message] Exception occurred while processing " + this.msg);
            e.printStackTrace();
        }
    }
}

/**
 * Helper class for webpage management
 */
class WebpageTotal{

    private Webpage web;
    private int totalUrl;

    public WebpageTotal(Webpage web, int totalUrl){

        this.web = web;
        this.totalUrl = totalUrl;
    }

    public Webpage getWeb(){
        return this.web;
    }

    public int getTotalUrl(){
        return this.totalUrl;
    }
}