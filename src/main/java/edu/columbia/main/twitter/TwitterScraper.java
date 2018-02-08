package edu.columbia.main.twitter;

import edu.columbia.main.FileSaver;
import edu.columbia.main.LanguageDataManager;
import edu.columbia.main.LogDB;
import edu.columbia.main.configuration.BabelConfig;
import edu.columbia.main.db.DAO;
import edu.columbia.main.db.Models.Tweet;
import edu.columbia.main.language_id.LanguageDetector;
import edu.columbia.main.language_id.Result;
import edu.columbia.main.normalization.TwitterNormalizer;
import edu.columbia.main.screen_logging.ViewManager;
import org.apache.log4j.Logger;
import twitter4j.*;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Gideon on 2/20/15.
 */

/**
 * Consumes the Twitter API for a given language
 * Saves all found tweets
 */
public class TwitterScraper implements Serializable {

    private ViewManager viewManager;
    private Twitter twitter;
    private int counter = 0;
    private int numOfRequests = 0;
    private HashMap<String, Boolean> map = new HashMap<String, Boolean>();
    private ArrayList<String> words;
    private LogDB logDb;
    private String lang;
    private LanguageDetector languageDetector;
    static Logger log = Logger.getLogger(TwitterScraper.class);
    static int ngram = BabelConfig.getInstance().getConfigFromFile().ngram();


    public TwitterScraper(String language, LanguageDetector languageDetector, ViewManager viewManager) {
        this.lang = language; //1 lang per scraper
        this.words = LanguageDataManager.getMostCommonWords(this.lang, 5000, ngram);
        this.logDb = new LogDB(this.lang); //saving text files
        this.languageDetector = languageDetector;
        this.viewManager = viewManager;
    }


    /**
     * Iterates over all seed words
     *
     * @throws TwitterException
     */
    public void scrapeByLanguage() throws TwitterException {

        while (true) {
            Iterator it = words.iterator();
            while (it.hasNext()) {
                String word = (String) it.next();
                word = word.trim();
                if (word.equals("")) {
                    continue;
                }
                searchAndSave(word, languageDetector, this.lang);
                it.remove();
            }

            //after finishing all the words refill the list
            this.words = LanguageDataManager.getMostCommonWords(this.lang, 5000, ngram);
        }

    }


    /**
     * Queries the twitter api for a given words and saves all matched results
     *
     * @param word to query
     * @param lp   language identification instance
     * @param lang that tweets should be in
     * @throws TwitterException
     */
    private void searchAndSave(String word, LanguageDetector lp, String lang) throws TwitterException {
        log.info("Searching for posts that contain the word: " + word);
        Query query = new Query(word);
        if (lang.equals("lit")) {
            query.setLang("lt");
        } else if (lang.equals("tel"))
            query.setLang("te");

        query.setCount(100);
        query.setSince("2010-01-01");

        QueryResult result;
        do {
            result = twitter.search(query);
            if (numOfRequests++ == 178) {
                throw new TwitterException("API Limit");
            }
            List<Status> tweets = result.getTweets();
            for (Status tweet : tweets) {
                try {
                    if (map.get(tweet.getText()) == null) {
                        String content = tweet.getText();
                        String url = "http://twitter.com/" + tweet.getUser().getId() + "/status/" + tweet.getId();
                        String origContent = content;
                        content = new TwitterNormalizer().cleanTweet(content);
                        Result res = lp.detectLanguage(content, this.lang);
                        if (res.languageCode.equals(this.lang) && res.isReliable) {
                            FileSaver file = new FileSaver(content, lang, "twitter", url, String.valueOf(tweet.getId()));
                            String filename = file.getFileName();
                            Tweet t = new Tweet(content, origContent, this.lang, tweet.getUser().getScreenName(), null, "topsyTwitter", url, String.valueOf(tweet.getId()), filename);
                            if (DAO.saveEntry(t)) {
                                file.save(logDb);
                                this.viewManager.getLogger(this.lang).incrementSaved();
                            } else {
                                this.viewManager.getLogger(this.lang).incrementDuplicate();
                            }

                            this.map.put(tweet.getText(), true); //to not repeat
                            counter++;
                            if (counter > 500) { //refresh map cache
                                this.map = new HashMap<String, Boolean>();
                                counter = 0;
                            }
                        }
                        else{
                            this.viewManager.getLogger(this.lang).incrementNotInLang();
                        }
                    }
                } catch (Exception e) {
                    log.error(e);
                }
            }
            viewManager.printToConsole();
        } while ((query = result.nextQuery()) != null);

    }

    public void saveScrapingState() throws IOException {
        FileOutputStream fout = new FileOutputStream("TwitterScraper_" + this.lang + ".ser");
        ObjectOutputStream oos = new ObjectOutputStream(fout);
        oos.writeObject(this);
        oos.close();

        log.info("SAVED: " + this.toString());

    }

    public static TwitterScraper getSerializedFromDisk(String lang) {

        FileInputStream fin = null;
        try {
            fin = new FileInputStream("TwitterScraper_" + lang + ".ser");
            ObjectInputStream ois = new ObjectInputStream(fin);
            TwitterScraper ts = (TwitterScraper) ois.readObject();
            ois.close();
            return ts;
        } catch (FileNotFoundException e) {
            log.error(e);
            return null;
        } catch (ClassNotFoundException e) {
            log.error(e);
            return null;
        } catch (IOException e) {
            log.error(e);
            return null;
        }


    }

    public void setKey(Twitter key) {
        this.twitter = key;
    }


}

