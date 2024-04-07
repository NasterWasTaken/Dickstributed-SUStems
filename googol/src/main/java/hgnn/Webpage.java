package hgnn;

import java.io.Serializable;

public class Webpage implements Serializable{

    private String url;
    private String title = "";
    private String content = "";


    public Webpage(String url){

        this.url = url;
    }

    public Webpage(String url, String title){

        super();
        this.url = url;
        this.title = title;
    }

    public Webpage(String url, String title, String content){

        super();
        this.url = url;
        this.title = title;
        this.content = content;
    }

    public void setUrl(String url) {
        this.url = url;
    }
    public String getUrl() {
        return this.url;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public String getTitle() {
        return this.title;
    }
    public void setContent(String content) {
        this.content = content;
    }

    public String getContent(){
        return this.content;
    }

     @Override
     public String toString(){
        return this.url + " | " + this.title + " | " + this.content;
     }

}