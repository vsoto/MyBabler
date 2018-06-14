package edu.columbia.main.collection;

import com.sun.syndication.feed.synd.SyndContentImpl;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;
import edu.columbia.main.FileSaver;
import edu.columbia.main.LogDB;
import static edu.columbia.main.collection.RSSScraper.log;
import edu.columbia.main.db.DAO;
import edu.columbia.main.db.Models.BBNPost;
import edu.columbia.main.language_id.LanguageDetector;
import edu.columbia.main.language_id.Result;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Whitelist;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import java.util.HashMap;
import java.util.AbstractMap.SimpleEntry;

/**
 * This class scrapes data from a RSS feed, checks that it is in the desired
 * languageCode and finally saves it Created by Gideon on 9/28/14.
 */
public class SoupScraper {

    public int numOfFiles = 0;
    private final LanguageDetector ld;
    public final String language;
    private final LogDB logDb;
    private final String url;
    private static final Logger log = Logger.getLogger(SoupScraper.class);

    public SoupScraper(String url, String language, LogDB logDb, LanguageDetector ld) {
        this.url = url;
        this.language = language;
        this.logDb = logDb;
        this.ld = ld;

    }

    public void fetchAndSave() throws Exception {
        Document doc = Jsoup.connect(this.url).userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/33.0.1750.152 Safari/537.36").get();
        boolean valid = Jsoup.isValid(doc.html(), Whitelist.basic());

        if (!valid) {
            doc = new Cleaner(Whitelist.basic()).clean(doc);
        }

        String title = doc.title();
        String content = doc.text();

        try {
            Result result = ld.detectLanguage(content, language);
            if (result.languageCode.equals(language) && result.isReliable) {
                FileSaver file = new FileSaver(content, this.language, "soup", this.url, this.url, String.valueOf(content.hashCode()));
                String fileName = file.getFileName();
                BBNPost post = new BBNPost(content, this.language, null, "soup", this.url, this.url, fileName);
                if (DAO.saveEntry(post)) {
                    log.info("Saving " + this.url);
                    file.save(this.logDb);
                }
                else {
                    log.info("Duplicated url: " + this.url);
                }
            }
            else {
                log.info("Wrong lang code [" + result.languageCode + "]: " + this.url);
            }
        } catch (Exception e) {
            log.error(e);
        }
    }

    public static SimpleEntry<Double, Integer> fetchAndCount(String url, HashMap<String, Double> unigram_freq) throws Exception {
        double web_precision = 0.0;
        int count_tokens = 0;

        Document doc = getDocument(url);
        boolean valid = Jsoup.isValid(doc.html(), Whitelist.basic());

        if (!valid) {
            doc = new Cleaner(Whitelist.basic()).clean(doc);
        }

        String content = doc.title() + " " + doc.text();
        for (String token : content.split(" ")) {
            if (unigram_freq.containsKey(token)) {
                web_precision += unigram_freq.get(token);
            }
            count_tokens++;
        }

        log.info("[SUMMARY] " + url + " web_prec=" + web_precision + " count_tokens=" + count_tokens);
        return new SimpleEntry<>(web_precision, count_tokens);
    }

    private static Document getDocument(String url_str) throws IOException {
        Document doc = Jsoup.connect(url_str).timeout(10*1000).userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/33.0.1750.152 Safari/537.36").get();
        return doc;
    }
}
