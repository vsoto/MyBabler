package edu.columbia.main.bing.bbn_scraper;

import edu.columbia.main.LanguageDataManager;
import edu.columbia.main.MTHttpClient;
import edu.columbia.main.collection.BabelScraper;
import edu.columbia.main.language_id.LanguageDetector;
import edu.columbia.main.screen_logging.ViewManager;
import org.apache.commons.httpclient.HttpClient;
import org.apache.log4j.Logger;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by Victor on 4/1/18.
 */

/**
 * This class implements a producer consumer pattern
 * It launches two thread pools, one for producers that generate Bing api queries based on seed words
 * and one for consumers that visit the pages found and tries to scrape them.
 * The communication between producers and consumers is done via the Broker, a blocking queue.
 *
 * The process flow is as follows:
 *
 * 1. A BBNSearchProducer searches Bing for a word
 * 2. Each search results is then placed in the Broker(queue) as a BBNJob
 * 3. The consumer(BSFetcherAndSaver) pulls a job from the queue and tries to scrape the URL. If succeeded then the data is saved.
 *
 *
 * Each BBNSearchProducer is responsible for a single language
 * The BBNFetcherAndSaver instances are generic and shared between languages
 *
 */
public class BBNJobManager extends BabelScraper {

    private static final Logger log = Logger.getLogger(BBNJobManager.class);

    public void run(String ranked_ngrams_file){
        BBNBroker broker = new BBNBroker();
        LanguageDetector lp = new LanguageDetector();
        HttpClient httpClient = new MTHttpClient().getClient();
        String lang = LanguageDataManager.getLanguage();
        String[] langs = new String[]{lang};
        log.info(ranked_ngrams_file);
        String[] ranked_ngrams_files = new String[]{ranked_ngrams_file};
        log.info(ranked_ngrams_files);
        ViewManager viewManager = new ViewManager(langs);



        ExecutorService consumers = Executors.newFixedThreadPool(NUM_OF_CONSUMERS);
        ExecutorService producers = Executors.newFixedThreadPool(NUM_OF_PRODUCERS);
        Future[] producersFutures = new Future[NUM_OF_PRODUCERS];//one for each languageCode

        //intiate producers
        for(int i = 0 ; i < NUM_OF_PRODUCERS ; i++){
            Future f = producers.submit(new BBNSearchProducer(broker, langs[i], ranked_ngrams_files[i]));
            producersFutures[i] = f;
        }

        //initiate consumers
        for(int i = 0 ; i < NUM_OF_CONSUMERS; i++){
            consumers.execute(new BBNFetcherAndSaver(broker, lp, i, httpClient, viewManager));
        }


        try {
            //wait for all producers
            for(int i = 0 ; i < NUM_OF_PRODUCERS ; i++){
                producersFutures[i].get();
            }
            producers.shutdown();
            consumers.shutdown();
        } catch (InterruptedException e) {
            log.error(e);
        } catch (ExecutionException e) {
            log.error(e);
        }

    }


}
