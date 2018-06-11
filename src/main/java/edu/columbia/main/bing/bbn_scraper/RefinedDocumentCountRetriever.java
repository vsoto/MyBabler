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
import java.util.Map.Entry;
import java.util.AbstractMap.SimpleEntry;
import java.util.Set;

/**
 *  * Created by Gideon on 10/2/14. * Provides a wrapper for file opening
 *
 */
public class RefinedDocumentCountRetriever {
    private static final Logger log = Logger.getLogger(RefinedDocumentCountRetriever.class);
    private static final int num_top_terms_ = 4;

    public static void start(String pathBuildTranscripts, String initialRankingFile, String refinedRankingFile) {
        log.info("Reading initial ranking of terms...");
        ArrayList<SimpleEntry<String, Double>> topTerms = readHighestRankedNGrams(initialRankingFile, Integer.MAX_VALUE);
        log.info("Total number of terms read: " + topTerms.size());
        
        log.info("Computing corpus unigram frequency.");
        HashMap<String, Double> unigram_freq = getUnigramFrequency(pathBuildTranscripts);
        log.info("Found " + unigram_freq.size() + " unigram terms in the corpus.");

        // Compute DF(t)
        log.info("Will refine top " + num_top_terms_ + " terms");
        HashMap<String, Double> scores = new HashMap<>();
        int term_counter = 0;
        for (Entry<String, Double> entry : topTerms) {
            String term = entry.getKey();
            Double unrefined_score = entry.getValue();
            if (term_counter < num_top_terms_) {
                SearchStats st = BBNSearchProducer.searchWordAndRetrieveStats(term, unigram_freq);
                int df = st.document_freq;
                if (df > 500) {
                    df = 500;
                }
                double p = st.web_precision;
                scores.put(term, df * p);
                term_counter++;
            } else {
                break;
                //scores.put(term, unrefined_score);
            }
            
        }
        log.info("Re-ranking top terms...");
        Set<Entry<String, Double>> sorted_entries = HashMapSorting.sort(scores);
        log.info("Outputting top re-ranked terms to refinedRankingFile");
        writeToFile(refinedRankingFile, sorted_entries);

    }

    private static void writeToFile(String pathRankingFile, Set<Entry<String, Double>> sorted_entries) {
        try {
            File file = new File(pathRankingFile);
            FileWriter fileWriter = new FileWriter(file);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            for (Entry<String, Double> entry : sorted_entries) {
                bufferedWriter.write(entry.getKey() + "\t" + entry.getValue() + "\n");
            }
            bufferedWriter.close();
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static ArrayList<SimpleEntry<String, Double>> readHighestRankedNGrams(String pathRankingFile, int max_entries) {
        ArrayList<SimpleEntry<String, Double>> entries = new ArrayList<>();
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
                entries.add(new SimpleEntry<>(word, score));
                num_ngrams++;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return entries;

    }

//    private static HashSet<String> computeDocumentNGrams(String filepath, Integer max_ngram) {
//        HashSet<String> ngrams = new HashSet<String>();
//        try {
//            File file = new File(filepath);
//            FileReader fileReader = new FileReader(file);
//            BufferedReader bufferedReader = new BufferedReader(fileReader);
//
//            String line;
//            while ((line = bufferedReader.readLine()) != null) {
//                if (!line.startsWith("[")) {
//                    ArrayList<String> tokens = new ArrayList<String>();
//                    for (String token : line.split(" ")) {
//                        if (!(token.startsWith("<") || token.startsWith("(") || token.startsWith("-") || token.endsWith("-") || token.equals("~"))) {
//                            tokens.add(token.replace("_", ""));
//                        }
//                    }
//                    HashSet<String> ngrams_line = getLineNGrams(tokens, max_ngram);
//                    ngrams.addAll(ngrams_line);
//                }
//            }
//            fileReader.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return ngrams;
//    }

//    private static HashSet<String> getLineNGrams(ArrayList<String> tokens, int max_ngram) {
//        HashSet<String> ngrams = new HashSet<String>();
//        for (int i = 0; i < tokens.size(); ++i) {
//            String aux_ngram = tokens.get(i);
//            ngrams.add(aux_ngram);
//            for (int n = 1; (n < max_ngram) && (i + n < tokens.size()); ++n) {
//                aux_ngram += " " + tokens.get(i + n);
//                ngrams.add(aux_ngram);
//            }
//        }
//        return ngrams;
//    }

    private static HashMap<String, Double> getUnigramFrequency(String path_corpus) {
        HashMap<String, Double> ngrams_count = new HashMap<>();

        File folder = new File(path_corpus);
        File[] listOfFiles = folder.listFiles();

        // Read every file and update vocabulary and document counts
        int total_num_tokens = 0;
        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                String filename = path_corpus + "/" + listOfFiles[i].getName();
                try {
                    File file = new File(filename);
                    FileReader fileReader = new FileReader(file);
                    BufferedReader bufferedReader = new BufferedReader(fileReader);

                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        if (!line.startsWith("[")) {
                            ArrayList<String> tokens = new ArrayList<>();
                            for (String token : line.split(" ")) {
                                if (!(token.startsWith("<") || token.startsWith("(") || token.startsWith("-") || token.endsWith("-") || token.equals("~"))) {
                                    token = token.replace("_", "");
                                    total_num_tokens++;
                                    if (ngrams_count.containsKey(token)) {
                                        double count = ngrams_count.get(token);
                                        ngrams_count.put(token, count + 1.0);
                                    } else {
                                        ngrams_count.put(token, 1.0);
                                    }
                                }
                            }
                        }
                    }
                    fileReader.close();

                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
        HashMap<String, Double> ngrams_freq = new HashMap<String, Double>();
        for (Entry<String, Double> entry: ngrams_count.entrySet()) {
            ngrams_freq.put(entry.getKey(), entry.getValue()/total_num_tokens);
        }
        
        return ngrams_freq;
    }

}
