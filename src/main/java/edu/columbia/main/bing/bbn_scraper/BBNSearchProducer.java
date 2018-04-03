package edu.columbia.main.bing.bbn_scraper;

import edu.columbia.main.InitialDocumentCountRetriever;
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

import java.net.URI;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.http.entity.StringEntity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Gideon on 4/24/15.
 */
/**
 * Searches bing API for a word and puts all results into broker
 */
public class BBNSearchProducer extends BabelProducer {

    // Replace the subscriptionKey string value with your valid subscription key.
    static String subscriptionKey = "enter key here";

    // Verify the endpoint URI.  At this writing, only one endpoint is used for Bing
    // search APIs.  In the future, regional endpoints may be available.  If you
    // encounter unexpected authorization errors, double-check this value against
    // the endpoint for your Bing Web search instance in your Azure dashboard.
    static String host = "https://api.cognitive.microsoft.com";
    static String path = "/bing/v7.0/search";

    static AtomicInteger numOfRequests;
    Logger log = Logger.getLogger(BBNSearchProducer.class);
    int ngram = BabelConfig.getInstance().getConfigFromFile().ngram();

    public BBNSearchProducer(BabelBroker broker, String language, String ranked_ngrams_filename) {
        this.broker = broker;
        this.lang = language;
        this.words = InitialDocumentCountRetriever.getHighestRankedNGrams(ranked_ngrams_filename, 2);
        System.out.println("Using top " + this.words.size() + " ngrams");
        this.logDb = new LogDB(this.lang);
        numOfRequests = new AtomicInteger();
    }

    private static SearchResults SearchWeb(String searchQuery, String count, String offset) throws Exception {
        URL url = new URL(host + path + "?q=" + URLEncoder.encode(searchQuery, "UTF-8") + "&count=" + count + "&offset=" + offset);
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
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
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
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

    @Override
    protected void searchWordAndSave(String ngram) {
        boolean breakFlag = false;
        int counter = 0;
        this.subscriptionKey = BabelConfig.getInstance().getConfigFromFile().bing();
        // String searchQuery = "site:blogspot.com " + " \""+word+"\"" + " NOT lang:en";
        String searchQuery = "\"" + ngram + "\"" + " NOT lang:en";
        try {
            System.out.println("Querying: " + ngram);
            for (int i = 0; !breakFlag; i++) {
                System.out.println("Page: " + i);
                SearchResults result = this.SearchWeb(searchQuery, "50", String.valueOf(i * 50));
                ArrayList<String> urls = getURLs(result.jsonResponse);
                if (counter++ == 100 || urls.size() == 0) {
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
        // The results are paged. You can get 50 results per page max.
        //for (int i=1; !breakFlag ; i++) {
        //    aq.setPage(i);
        //    aq.doQuery();
        //    AzureSearchResultSet<AzureSearchWebResult> ars = aq.getQueryResult();
        //    if(counter++ == 100 ||ars.getAsrs().size() == 0)
        //        breakFlag = true;
        //    numOfRequests.getAndIncrement();
        //    for (AzureSearchWebResult anr : ars) {
        //        BBNJob job = new BBNJob(anr.getUrl(), lang, logDb);
        //        try {
        //            if(job.isValid())
        //                broker.put(job);
        //           else{
        //                log.debug("Job not valid: " + job);
        //            }
        //       } catch (InterruptedException e) {
        //            log.error(e);
        //        }
        //    }
        //}
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
