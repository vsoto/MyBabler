package edu.columbia.main.bing.bbn_scraper;

import edu.columbia.main.LogDB;
import edu.columbia.main.collection.BabelJob;
import org.apache.log4j.Logger;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by Gideon on 4/24/15.
 */

/**
 * A scraping job
 */
public class BBNJob extends BabelJob {

    static Logger log = Logger.getLogger(BBNJob.class);
    private String origData;
    /* the url to collect */
    private String url;
    /* is this job valid collection job*/
    private boolean isValid = true;


    public BBNJob(String url, String lang, LogDB logDb) {
        super(url, lang, logDb, null);
        setURL(url);
        origData = data;
    }


    public String getURL() {
        return url;
    }

    public boolean isValid(){
        return isValid;
    }

    @Override
    public String toString() {
        return this.origData;
    }

    /**
     * sets the URL and checks that it's a valid url
     * @param URL A url from bing search resutls
     */
    public void setURL(String URL) {
        java.net.URL tempUrl = null;
        try {
            tempUrl = new URL(URL);
        } catch (MalformedURLException e) {
            log.debug(e);
            this.isValid = false;
        }
        this.url = "http://" + tempUrl.getHost(); //  + "/feeds/posts/default";
		System.out.println(this.url);

        // Ignore blogspot homepage urls
        // if (this.url.equals("http://www.blogspot.com/feeds/posts/default")){
        //    this.isValid = false;
        //}
    }
}
