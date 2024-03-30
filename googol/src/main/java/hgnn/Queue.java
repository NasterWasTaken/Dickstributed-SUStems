package hgnn;

import org.jsoup.*;
import org.jsoup.nodes.*;
import org.jsoup.select.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class Queue {
    private ArrayBlockingQueue<String> que;
    private Set<String> visited;

    public Queue(int size) {
        this.que = new ArrayBlockingQueue<>(size);
        this.visited = new HashSet<String>();
    }

    public void firstURL(String url) {
        this.que.add(url);
    }

    synchronized public void addVisited(String url) {
        this.visited.add(url);
    }

    synchronized public int getQueueSize() {
        return this.que.size();
    }

    synchronized public boolean inVisited(String url) {
        return visited.contains(url);
    }

    synchronized public String removeQueURL() {
        return que.poll();
    }

    synchronized public String getURLWait() throws InterruptedException {
        return this.que.take();
    }

    synchronized public String haveALook() {
        return this.que.peek();
    }

    synchronized public String updateQueue(String url) throws InterruptedException {
        try {
            wait();
        } catch (InterruptedException e) {
            System.out.println("interruptedException caught");
        }

        try {
            Document doc = Jsoup.connect(url).get();
                
            Elements links = doc.select("a[href]");
            for (Element link : links) {
                if(visited.contains(link.attr("abs:href"))) continue;

                que.put(link.attr("abs:href"));
            }
            
            visited.add(url);
            url = que.poll();

            System.out.printf(" | Queue size: %d | Visited: %d | url: %s\n", que.size(), visited.size(), haveALook());

        } catch (IOException e) {
            e.printStackTrace();
        }

        notifyAll();
        return url;
    }
}
