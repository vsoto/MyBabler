package edu.columbia.main.bing.bbn_scraper;

import edu.columbia.main.*;
import edu.columbia.main.HashMapSorting;

import org.apache.log4j.Logger;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

/**
 *  * Created by Gideon on 10/2/14. * Provides a wrapper for file opening
 *
 */
public class InitialDocumentCountRetriever {
    private static final Logger log = Logger.getLogger(InitialDocumentCountRetriever.class);
    private final static int max_ngram_order_ = 4;

    public static void start(String pathBuildTranscripts, String pathRankingFile) {
        File folder = new File(pathBuildTranscripts);
        File[] listOfFiles = folder.listFiles();

        HashSet<String> vocabulary = new HashSet<>();
        HashMap<String, Integer> document_counts = new HashMap<>();

        log.info("Input directory: " + pathBuildTranscripts);
        log.info("# documents: " + listOfFiles.length);
        int numDocuments = 0;
        // Read every file and update vocabulary and document counts
        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                numDocuments++;

                String filename = pathBuildTranscripts + "/" + listOfFiles[i].getName();
                HashSet<String> document_vocabulary = getDocumentNGrams(filename, max_ngram_order_);
                // Update vocabulary
                vocabulary.addAll(document_vocabulary);
                // Update document counts for DF(t)
                for (String term : document_vocabulary) {
                    int count = 0;
                    if (document_counts.containsKey(term)) {
                        count = document_counts.get(term);
                    }
                    document_counts.put(term, count + 1);
                }
            }
        }
        log.info("# processed documents: " + numDocuments);
        log.info("Max n-gram order extracted: " + max_ngram_order_);
        log.info("Vocabulary size (1-gram to " + max_ngram_order_ + "-gram: " + vocabulary.size());

        log.info("Computing ranking...");
        // Compute DF(t)
        HashMap<String, Double> scores = new HashMap<>();
        for (String term : vocabulary) {
            Double df = document_counts.get(term) * 1.0;
            Double p0 = term.length() * term.length() * 0.0025;
            if (p0 > 1.0) {
                p0 = 1.0;
            }
            scores.put(term, df * p0);
        }

        log.info("Outputting ranked terms to " + pathRankingFile);
        Set<Entry<String, Double>> sorted_entries = HashMapSorting.sort(scores);
        writeToFile(pathRankingFile, sorted_entries);

    }

    private static void writeToFile(String pathRankingFile, Set<Entry<String, Double>> sorted_entries) {
        try {
            File file = new File(pathRankingFile);
            FileWriter fileWriter = new FileWriter(file);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

            for (Entry<String, Double> entry : sorted_entries) {
                bufferedWriter.write(entry.getKey() + "\t" + entry.getValue() + "\n");
            }

            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static ArrayList<String> getHighestRankedNGrams(String pathRankingFile, int max_entries) {
        ArrayList<String> entries = new ArrayList<>();
        try {
            File file = new File(pathRankingFile);
            FileReader fileReader = new FileReader(file);
            BufferedReader bufferedReader = new BufferedReader(fileReader);

            String line;
            int num_ngrams = 0;
            while (((line = bufferedReader.readLine()) != null) && (num_ngrams < max_entries)) {
                String[] tokens = line.split("\t");
                String word = tokens[0];
                double score = Double.parseDouble(tokens[1]);
                entries.add(word);
                num_ngrams++;
            }

        } catch (IOException e) {
            log.error(e);
        }
        return entries;

    }

    private static HashSet<String> getDocumentNGrams(String filepath, Integer max_ngram) {
        HashSet<String> ngrams = new HashSet<String>();
        try {
            File file = new File(filepath);
            FileReader fileReader = new FileReader(file);
            BufferedReader bufferedReader = new BufferedReader(fileReader);

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (!line.startsWith("[")) {
                    ArrayList<String> tokens = new ArrayList<String>();
                    for (String token : line.split(" ")) {
                        if (!(token.startsWith("<") || token.startsWith("(") || token.startsWith("-") || token.endsWith("-") || token.equals("~"))) {
                            tokens.add(token.replace("_", ""));
                        }
                    }
                    HashSet<String> ngrams_line = getLineNGrams(tokens, max_ngram);
                    ngrams.addAll(ngrams_line);
                }
            }
            fileReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ngrams;
    }

    private static HashSet<String> getLineNGrams(ArrayList<String> tokens, int max_ngram) {
        HashSet<String> ngrams = new HashSet<>();
        for (int i = 0; i < tokens.size(); ++i) {
            String aux_ngram = tokens.get(i);
            ngrams.add(aux_ngram);
            for (int n = 1; (n < max_ngram) && (i + n < tokens.size()); ++n) {
                aux_ngram += " " + tokens.get(i + n);
                ngrams.add(aux_ngram);
            }
        }
        return ngrams;
    }

}
