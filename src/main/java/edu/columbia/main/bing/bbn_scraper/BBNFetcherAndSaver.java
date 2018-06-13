package edu.columbia.main.bing.bbn_scraper;

import edu.columbia.main.collection.BabelConsumer;
import edu.columbia.main.collection.SoupScraper;
import edu.columbia.main.language_id.LanguageDetector;
import edu.columbia.main.screen_logging.TaskLogger;
import edu.columbia.main.screen_logging.ViewManager;
import org.apache.commons.httpclient.HttpClient;
import org.apache.log4j.Logger;

import java.net.MalformedURLException;
import java.util.AbstractMap;

/**
 * Created by Gideon on 4/24/15.
 */


/**
 * Fetches a job from the queue and tries to scrape the RSS feed from that results
 */
public class BBNFetcherAndSaver extends BabelConsumer implements Runnable{

    HttpClient httpClient;
    ViewManager viewManager;
    
    private static final Logger log = Logger.getLogger(BBNFetcherAndSaver.class);
    
    public BBNFetcherAndSaver(BBNBroker broker, LanguageDetector languageDetector, int i, HttpClient httpClient, ViewManager viewManager) {
        super(broker, languageDetector, i, null);
        this.httpClient = httpClient;
        this.viewManager = viewManager;
    }

    /**
     * Fetch results
     */
    @Override
    public void run() {
        Thread.currentThread().setName("Parser " + i);
        BBNJob data;
        try {
            while (true){
                data = (BBNJob) broker.get();
                if (data != null) {
                    searchAndSave(data);
                    viewManager.printToConsole();
                }
            }
        }
        catch (InterruptedException e) {
            log.error(e);
        }
    }

    /**
     * Scrape RSS feed
     * @param job contains url to RSS feed
     */
    protected void searchAndSave(BBNJob job){
        try {
            SoupScraper soupScraper = new SoupScraper(job.getURL(), job.getLanguage(), job.getDB(), this.ld);
            soupScraper.fetchAndSave();
        } catch (MalformedURLException e) {
            log.error(e);
        } catch (Exception ex){
            log.error(ex);
        }
    }


}
