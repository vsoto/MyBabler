package edu.columbia.main.collection;

import com.sun.syndication.feed.synd.SyndContentImpl;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;
import edu.columbia.main.FileSaver;
import edu.columbia.main.LogDB;
import edu.columbia.main.db.DAO;
import edu.columbia.main.db.Models.BlogPost;
import edu.columbia.main.language_id.LanguageDetector;
import edu.columbia.main.language_id.Result;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.net.URL;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;

/**
 * This class scrapes data from a RSS feed, checks that it is in the desired
 * languageCode and finally saves it Created by Gideon on 9/28/14.
 */
public class SoupScraper {

    private final LanguageDetector ld;
    public String language = "";
    public int numOfFiles = 0;
    public int wrongCount = 0;
    private LogDB logDb;
    private String url;
    static Logger log = Logger.getLogger(SoupScraper.class);

    public SoupScraper(String url, String language, LogDB logDb, LanguageDetector ld) {
        this.url = url;
        this.language = language;
        this.logDb = logDb;
        this.ld = ld;

    }

    public AbstractMap.SimpleEntry<Integer, Integer> fetchAndSave() throws Exception {

        URL url = new URL(this.url);

        Document doc = Jsoup.connect(this.url).get();
        String title = doc.title();
        String content = doc.text();
        
        System.out.println("Fetching and saving " + this.url);
        System.out.println(content);

        try {
            Result result = ld.detectLanguage(content, language);
            if (result.languageCode.equals(language) && result.isReliable) {
                FileSaver file = new FileSaver(content, this.language, "soup", this.url, this.url, String.valueOf(content.hashCode()));
                String fileName = file.getFileName();
                BlogPost post = new BlogPost(content, this.language, null, "soup", this.url, this.url, fileName);
                if (DAO.saveEntry(post)) {
                    file.save(this.logDb);
                    numOfFiles++;
                    wrongCount = 0;
                }

            } else {
                log.info("Item " + title + "is in a diff languageCode, skipping this post  " + result.languageCode);
                wrongCount++;
                if (wrongCount > 3) {
                    log.info("Already found 3 posts in the wrong languageCode, skipping this blog");
                }
                
            }

        } catch (Exception e) {
            log.error(e);
        }
        return new AbstractMap.SimpleEntry<>(numOfFiles, wrongCount);
    }

//    public static List getAllPostsFromFeed(String urlToGet, String source) throws IOException, FeedException {
//
//        ArrayList<BlogPost> posts = new ArrayList<BlogPost>();
//
//        URL url = new URL(urlToGet);
//        SyndFeedInput input = new SyndFeedInput();
//        try {
//            SyndFeed feed = input.build(new XmlReader(url));
//
//            int items = feed.getEntries().size();
//
//            if (items > 0) {
//                log.info("Attempting to parse rss feed: " + urlToGet);
//                log.info("This Feed has " + items + " items");
//                List<SyndEntry> entries = feed.getEntries();
//
//                for (SyndEntry item : entries) {
//                    if (item.getContents().size() > 0) {
//                        SyndContentImpl contentHolder = (SyndContentImpl) item.getContents().get(0);
//                        String content = contentHolder.getValue();
//                        if (content != null && !content.isEmpty()) {
//                            BlogPost post = new BlogPost(content, null, null, source, item.getLink(), item.getUri(), null);
//                            posts.add(post);
//                        }
//                    }
//                }
//            }
//            return posts;
//        } catch (Exception ex) {
//            log.error(ex);
//            return posts;
//        }
//
//    }

}
