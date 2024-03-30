package hgnn;

public class QueueManager {
    public static void main(String args[]) {
        String url = args[1];
        int numDownloaders = Integer.parseInt(args[0]);
        Queue que = new Queue(100000);

        que.firstURL(url);

        for(int i = 0; i < numDownloaders; ++i) {
            new Downloader(que, i);
        }
    }
}