package edu.columbia.main.bing.bbn_scraper;

import edu.columbia.main.LanguageDataManager;
import edu.columbia.main.LogDB;
import edu.columbia.main.configuration.BabelConfig;
import edu.columbia.main.collection.BabelBroker;
import edu.columbia.main.collection.BabelProducer;
import org.apache.log4j.Logger;

import java.net.*;
import java.util.*;
import java.io.*;
import javax.net.ssl.HttpsURLConnection;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import static edu.columbia.main.bing.bbn_scraper.BBNSearchProducer.numOfRequests;
import edu.columbia.main.collection.SoupScraper;
import java.util.AbstractMap.SimpleEntry;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Gideon on 4/24/15.
 */
/**
 * Searches bing API for a word and puts all results into broker
 */
public class BBNSearchProducer extends BabelProducer {

    // Verify the endpoint URI.  At this writing, only one endpoint is used for Bing
    // search APIs.  In the future, regional endpoints may be available.  If you
    // encounter unexpected authorization errors, double-check this value against
    // the endpoint for your Bing Web search instance in your Azure dashboard.
    static String subscriptionKey; // = "7112e369e3ad4d3aa00c2db08664f8f7";
    static String host = "https://api.cognitive.microsoft.com";
    static String path = "/bing/v7.0/search";

    static AtomicInteger numOfRequests;
    static private final Logger log = Logger.getLogger(BBNSearchProducer.class);
    
    static private final int max_page_size = 50;
    static private final int max_num_scraped_urls = 100;
    static private final int query_top_ngrams = 4;
    
    public BBNSearchProducer(BabelBroker broker, String language, String ranked_ngrams_filename) {
        this.broker = broker;
        this.lang = language;
        this.words = InitialDocumentCountRetriever.getHighestRankedNGrams(ranked_ngrams_filename, query_top_ngrams);
        log.info("Querying top " + this.words.size() + " ngrams");
        this.logDb = new LogDB(this.lang);
        numOfRequests = new AtomicInteger();
    }

    private static SearchResults SearchWeb(String searchQuery, int count, String offset) throws Exception {
        URL url = new URL(host + path + "?q=" + URLEncoder.encode(searchQuery, "UTF-8") + "&count=" + count + "&offset=" + offset);
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        subscriptionKey = BabelConfig.getInstance().getConfigFromFile().bing();
        connection.setRequestProperty("Ocp-Apim-Subscription-Key", subscriptionKey);

        // receive JSON body
        InputStream stream = connection.getInputStream();
        String response = new Scanner(stream).useDelimiter("\\A").next();

        // construct result object for return
        SearchResults results = new SearchResults(new HashMap<String, String>(), response);

        // extract Bing-related HTTP headers
        Map<String, List<String>> headers = connection.getHeaderFields();
        for (String header : headers.keySet()) {
            if (header == null) {
                continue;      // may have null key
            }
            if (header.startsWith("BingAPIs-") || header.startsWith("X-MSEdge-")) {
                results.relevantHeaders.put(header, headers.get(header).get(0));
            }
        }
        stream.close();
        return results;
    }

    // pretty-printer for JSON; uses GSON parser to parse and re-serialize
    private static String prettify(String json_text) {
        JsonParser parser = new JsonParser();
        JsonObject json = parser.parse(json_text).getAsJsonObject();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(json);
    }

    // pretty-printer for JSON; uses GSON parser to parse and re-serialize
    private static ArrayList<String> getURLs(String json_text) {
        JsonParser parser = new JsonParser();
        JsonObject json = parser.parse(json_text).getAsJsonObject();
        ArrayList<String> urls = new ArrayList<String>();

        if (json.has("webPages")) {
            JsonObject webPages = json.getAsJsonObject("webPages");
            for (JsonElement entry : webPages.getAsJsonArray("value")) {
                JsonObject result = entry.getAsJsonObject();
                String url = result.getAsJsonPrimitive("url").getAsString();
                urls.add(url);
            }
        }
        return urls;
    }

    private static int getTotalEstimatedMatches(String json_text) {
        JsonParser parser = new JsonParser();
        JsonObject json = parser.parse(json_text).getAsJsonObject();

        if (json.has("webPages")) {
            JsonObject webPages = json.getAsJsonObject("webPages");
            return webPages.getAsJsonPrimitive("totalEstimatedMatches").getAsInt();
        }
        return 0;
    }

    @Override
    protected void searchWordAndSave(String ngram) {
        boolean breakFlag = false;
        int counter = 0;
        String searchQuery = "\"" + ngram + "\"" + " NOT lang:en";
        try {
            for (int i = 0; !breakFlag; i++) {
                SearchResults result = SearchWeb(searchQuery, max_page_size, String.valueOf(i * max_page_size));
                ArrayList<String> urls = getURLs(result.jsonResponse);
                log.info(ngram + " [" + i + "] : " + urls.size() + " urls retrieved.");
                if (counter++ == max_num_scraped_urls || urls.isEmpty()) {
                    breakFlag = true;
                }
                numOfRequests.getAndIncrement();

                for (String url : urls) {
                    BBNJob job = new BBNJob(url, lang, logDb);
                    try {
                        if (job.isValid()) {
                            broker.put(job);
                        } else {
                            log.debug("Job not valid: " + job);
                        }
                    } catch (InterruptedException e) {
                        log.error(e);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace(System.out);
            System.exit(1);
        }
    }

    protected static SearchStats searchWordAndRetrieveStats(String term, HashMap<String, Double> unigram_freq) {
        int document_freq = 0;
        double unigram_score = 0.0;
        int total_num_tokens = 0;
        int count_processed_docs = 0;
        
        String searchQuery = "\"" + term + "\"" + " NOT lang:en";

        try {

            SearchResults result = SearchWeb(searchQuery, max_page_size, "0");
            ArrayList<String> urls = getURLs(result.jsonResponse);
            document_freq = getTotalEstimatedMatches(result.jsonResponse);

            for (String url : urls) {
                try {
                    SimpleEntry<Double, Integer> r = SoupScraper.fetchAndCount(url, unigram_freq);
                    unigram_score += r.getKey();
                    total_num_tokens += r.getValue();
                    count_processed_docs++;
                } catch (Exception e) {
                    log.error(e);
                }
            }

        } catch (Exception e) {
            log.error(e);
            System.exit(1);
        }
        double web_precision = unigram_score * 1.0 / total_num_tokens;
        log.info(term + "\t" + count_processed_docs + "\t" + document_freq + "\t" + unigram_score + "\t" + total_num_tokens + "\t" + web_precision);
        SearchStats st = new SearchStats(document_freq, web_precision);
        return st;
    }
}

// Container class for search results encapsulates relevant headers and JSON data
class SearchResults {

    HashMap<String, String> relevantHeaders;
    String jsonResponse;

    SearchResults(HashMap<String, String> headers, String json) {
        relevantHeaders = headers;
        jsonResponse = json;
    }
}

class SearchStats {

    int document_freq;
    double web_precision;

    SearchStats(int document_freq, double web_precision) {
        this.document_freq = document_freq;
        this.web_precision = web_precision;
    }
}
