package edu.columbia.main.configuration;

import org.aeonbits.owner.Config;
/**
 * Created by Gideon on 9/29/15.
 */

public interface ConfigFromFile extends Config {

    @Separator(";")
    String [] twitter_consumer();

    @Separator(";")
    String [] twitter_secret();

    @Separator(";")
    String [] twitter_access();

    @Separator(";")
    String [] twitter_token_secret();


    @DefaultValue("scraping/")
    String productionScrapingFolderPath();

    @DefaultValue("true")
    Boolean useMajorityVote();

    String bing();

    String google_cse_key();

    String google_cse_cx();

    String diffbot();

    String lang();

    String rankedNGramFile();

    String module();

    String xml_file();

    @DefaultValue("false")
    Boolean debug();

    @DefaultValue("INFO")
    String console_log_level();

    @DefaultValue("INFO")
    String file_log_level();

    @DefaultValue("logs/log.txt")
    String logs_path();

    @DefaultValue("1")
    int ngram();

    String pathToWordsFile();

}
