package hgnn;

import org.jsoup.*;
import org.jsoup.nodes.*;
import org.jsoup.select.*;
import java.io.*;
import java.util.*;

public class Downloader {
    public static void main(String args[]) {
        String url = args[0];

        try {
            Document doc = Jsoup.connect(url).get();
            StringTokenizer tokens = new StringTokenizer(doc.text());
            int countTokens = 0;
            while (tokens.hasMoreElements() && countTokens++ < 100)
                System.out.println(tokens.nextToken().toLowerCase());
            Elements links = doc.select("a[href]");
            for (Element link : links)
                System.out.println(link.text() + "\n" + link.attr("abs:href") + "\n");
        
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}