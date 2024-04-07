package hgnn;

import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.*;
import java.util.*;

public class RMIGateway extends UnicastRemoteObject implements RMIGatewayBase {

    private Queue queue;
    private ArrayList<BarrelTotal> barrels;
    private int activeBarrels;
    private int barrelCount;
    private HashMap<String, Integer> tops;

    public RMIGateway() throws RemoteException{

        super();
        this.barrels = new ArrayList<BarrelTotal>();
        this.tops = new HashMap<>();
        this.activeBarrels = 0;
        this.barrelCount = 0;

        try{

            this.queue = (Queue) Naming.lookup("rmi://192.168.1.98/Queue");
            System.out.println("[Gateway] Connected to the Queue.");

        }catch(RemoteException re){

            System.out.println("[Gateway] Remote Exception: Cannot connect to the Queue.");

        }catch(Exception e){

            System.out.println("[Gateway] Exception in constructor: " + e);

        }

    }

    public ArrayList<Webpage> search(String page, int attempts) throws RemoteException{

        if(attempts >= 5)
            return new ArrayList<>();

        else if(attempts == 0){

            if(this.tops.containsKey(page)){

                this.tops.put(page, this.tops.get(page) + 1);
            }
            else{

                this.tops.put(page, 1);
            }

        }
        try{
            if(barrels.size() > 0){
                Random rand = new Random();
                for (int i = 0; i < 10; i++){

                    int n = rand.nextInt(0, this.barrelCount);
                    if(barrels.get(n).getActive() == true){

                        return barrels.get(n).getBarrel().search(page);
                    }
                }
                throw new RemoteException();
            }
        }
        catch (RemoteException re){

            System.out.println("[Gateway] No result was found, trying again" + (attempts + 1) + "/ 5");
            return this.search(page, attempts + 1);
        }
        return new ArrayList<>();
    }

    public void indexNewUrl(String url) throws RemoteException{

        System.out.println("[Gateway] Index new url: " + url);
        queue.addURL(url);
    }

    public int subBarrel(BarrelBase barrel, String barrelname) {

        try {
            this.getBarrels();
        } catch (RemoteException re) {

            re.printStackTrace();
        }

        for (int i = 0; i < getBarrelsSize(); i++) {

            if (barrels.get(i).getName().equals(barrelname)) {

                if (barrels.get(i).getActive()) {

                    System.out.println("[Gateway] Error subscribing barrel, already in use.");
                    return -1;
                } else {

                    barrels.get(i).setBarrel(barrel);
                    System.out.println("[Gateway] Subscription to barrel successful: Barrel " + barrelname + " reconnected.");
                    return i;
                }
            }
        }

        this.barrels.add(new BarrelTotal(barrel, barrelname));
        this.barrelCount++;

        System.out.println("[Gateway] Subscription to barrel successful: Barrel " + barrelname + " subscribed and connected.");
        return this.barrelCount;
    }

    public HashSet<String> lookupURL(String key, int attempts) throws RemoteException{

        if(attempts >= 5){

            return new HashSet<String>();
        }
        try{

            if(barrels.size() > 0){

                Random rand = new Random();
                for (int i = 0; i < 5; i++) {
                    
                    int x = rand.nextInt(0, this.barrelCount);
                    if(barrels.get(x).getActive() == true){

                        return this.barrels.get(x).getBarrel().lookupLinks(key);
                    }
                }
                throw new RemoteException();
            }
        }
        catch(RemoteException re){

            System.out.println("[Gateway] No urls found when looking, attemping again " + (attempts + 1) + "/5");
            return this.lookupURL(key, attempts + 1);
        }
        return new HashSet<String>();
    }

    public void checkBarrel(int i) {

        try {

            this.barrels.get(i).setActive(this.barrels.get(i).getBarrel().ActiveChecker());
        } catch (RemoteException e) {

            this.barrels.get(i).setActive(false);
        }
    }

    public ArrayList<String> getTops() throws RemoteException {

        List<String> list = new ArrayList<>(this.tops.keySet());
        Collections.sort(list, new Comparator<String>() {

            public int compare(String a, String b){

                return tops.get(b).compareTo(tops.get(a));
            }
        });

        return new ArrayList<>(list.subList(0, Math.min(list.size(), 10)));
    }

    public int totalActiveBarrels() {

        int total = 0;
        for (BarrelTotal barrel : this.barrels) {

            if (barrel.getActive() == true) {

                total++;
            }
        }
        return total;
    }

    public int getBarrelsSize() {

        return this.barrels.size();
    }

    public ArrayList<String> getBarrels() throws RemoteException {

        for (int i = 0; i < this.getBarrelsSize(); i++) {

            this.checkBarrel(i);
            if (this.totalActiveBarrels() != this.activeBarrels) {

                this.activeBarrels = this.totalActiveBarrels();
            }

        }

        ArrayList<String> activeBarrels = new ArrayList<>();

        for (BarrelTotal barrel : this.barrels) {

            if (barrel.getActive()) {

                activeBarrels.add(barrel.getName());
            }
        
        }

        return activeBarrels;

    }

    public static void main(String[] args){

        try{
            System.out.println("[Gateway] Booting up...");
            RMIGatewayBase rmiBase = new RMIGateway();
            Registry regist = LocateRegistry.createRegistry(1099);
            regist.rebind("Gateway", rmiBase);
            System.out.println("[Gateway] RMI Gateway ready");
        }
        catch (RemoteException re){

            System.out.println("[Gateway] Remote exception found in main: " + re);
        }
        catch (Exception e){

            System.out.println("[Gateway] Excpetion found in main: " + e);
        }
    }
}

class BarrelTotal {

    private BarrelBase barrel;
    private String name;
    private boolean active;

    public BarrelTotal(BarrelBase barrel1, String name1) {

        this.barrel = barrel1;
        this.name = name1;
        this.active = true;
    }

    public void setBarrel(BarrelBase barrel) {

        this.barrel = barrel;
    }

    public BarrelBase getBarrel() {

        return this.barrel;
    }

    public void setName(String name) {

        this.name = name;
    }

    public String getName() {

        return this.name;
    }

    public void setActive(boolean active) {

        this.active = active;
    }

    public Boolean getActive() {

        return this.active;
    }

}