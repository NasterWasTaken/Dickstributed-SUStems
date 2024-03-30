package hgnn;

public class Downloader implements Runnable {
    Queue que;
    public boolean exit;

    Downloader(Queue que, int id) {
        this.que = que;
        this.exit = false;
        new Thread(this, String.format("Downloader-%d", id)).start();
    }

    public void stop() {
        this.exit = true;
    }

    public void run() {
        String url = que.haveALook();
        
        while(exit == false && que.getQueueSize() != 0) {
            if(que.inVisited(url)) {
                url = que.removeQueURL();
                continue;
            }
            
            try {
                System.out.print("Thread: " + Thread.currentThread().getName());
                que.updateQueue(url);
            } catch (InterruptedException e) {
                e.printStackTrace();
                stop();
            }
        }
    }
}
