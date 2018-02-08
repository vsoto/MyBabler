package edu.columbia.main.language_id;
import com.sun.org.apache.bcel.internal.ExceptionConstants;
import com.sun.org.apache.regexp.internal.RE;
import edu.columbia.main.configuration.BabelConfig;
import edu.columbia.main.language_id.cld.Cld2;
import edu.columbia.main.language_id.lingpipe.LingPipe;
import edu.columbia.main.Utils;
import edu.columbia.main.language_id.textcat.TextCategorizer;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.*;

/**
 * Created by Gideon on 9/4/15.
 */


/**
 * Language detection API that manages all supported classifiers
 */
public class LanguageDetector {

    /* LingPipe classifier */
    LingPipe lp = new LingPipe("completeModel3.gm");
    /* CLD2 Classifier */
    Cld2 cld = null;

    /* TextCat Classifier */
    edu.columbia.main.language_id.textcat.TextCategorizer tc = null;
    private ArrayList<String> lpLangs = new ArrayList<String>();
    Logger log = Logger.getLogger(LanguageDetector.class);


    /* Constructor */
    public LanguageDetector(){
        tc = new edu.columbia.main.language_id.textcat.TextCategorizer();
        try {
            cld = new Cld2();
        }
        catch (IOException e)
        {
            log.error(e);
        }
        catch (ClassNotFoundException e)
        {
            log.error(e);
        }
        catch (Error e){
            log.info("failed to load CLD. Continuing using LP language classifier. ");
        }

    }

    /**
     * Preforms language detection using majority vote approach over all classifiers
     * If CLD2 is not installed than fallback to standard language detection detectLanguage()
     * @param text to preform language detection on
     * @param lang what language is this text should be in? Used to pick classifiers
     * @return Classification results
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public Result detectMajorityVote(String text, String lang) throws IOException, ClassNotFoundException {

        if(cld == null){ //if we can't load CLD no point in doing majority vote
            return detectLanguage(text,lang);
        }

        ArrayList<Result> results = new ArrayList<>();
        LanguageCode code = new LanguageCode(lang, LanguageCode.CodeTypes.ISO_639_2);
	
        if(lp.getSupportedLanguages().contains(code) || (code.getLanguageCode().equals("swa") && lp.getSupportedLanguages().contains(new LanguageCode("swh", LanguageCode.CodeTypes.ISO_639_2)))){
	    Result pred = lp.detectLanguage(text);
	    if (pred.languageCode.equals("swh")){
		pred.languageCode = "swa";
	    }
            results.add(pred);
	}
        if(tc.getSupportedLanguages().contains(code) || (code.getLanguageCode().equals("swa") && tc.getSupportedLanguages().contains(new LanguageCode("swh", LanguageCode.CodeTypes.ISO_639_2)))){
	    Result pred = tc.detectLanguage(text);
	    if (pred.languageCode.equals("swh"))
                pred.languageCode = "swa";
            results.add(pred);
	}
        if(cld.getSupportedLanguages().contains(code) || (code.getLanguageCode().equals("swa") && cld.getSupportedLanguages().contains(new LanguageCode("swh", LanguageCode.CodeTypes.ISO_639_2)))) {
	    Result pred = cld.detectLanguage(text);
	    if (pred.languageCode.equals("swh"))
                pred.languageCode = "swa";
            results.add(pred);
	}

        Result res = mostCommon(results);
        if(res == null)
            return new Result(null,false,0);
        else
            return res;

    }

    /**
     * Preforms language detection with the best available classifier (based on measured accuracy)
     * @param text to preform language detection on
     * @param lang what language is this text should be in? Used to pick classifiers
     * @return Classification results
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public Result detectHierarchy(String text, String lang) throws IOException, ClassNotFoundException {
        if(text == null)
            return null;

        LanguageCode code = new LanguageCode(lang, LanguageCode.CodeTypes.ISO_639_2);


        if(cld != null && cld.getSupportedLanguages().contains(code)) {
            return cld.detectLanguage(text);
        }
        else if (lp.getSupportedLanguages().contains(code)){
            return lp.detectLanguage(text);
        }
        else if(tc.getSupportedLanguages().contains(code)){
            return tc.detectLanguage(text);
        }
        else{
            log.info("Language: " + lang + " not supported!");
        }

        return null;

    }

    /**
     * Main entry point for identification. Will choose approach based on configuration provided in config file
     * @param text to preform language detection on
     * @param lang what language is this text should be in? Used to pick classifiers
     * @return Classification results
     */
    public Result detectLanguage(String text, String lang) {
        try {
            if (BabelConfig.getInstance().getConfigFromFile().useMajorityVote() && cld!=null)
                return detectMajorityVote(text, lang);
            else
                return detectHierarchy(text, lang);

            }
        catch(NullPointerException np){
            log.error(np);
            return null;
        }
        catch (Exception e) {
            log.error("Can't run language id - > Shutting down!");
            log.error(e);
            System.exit(0);
        }
        return null;
    }

    @Deprecated
    public Result detectOld(String text, String lang){
        LanguageCode code = new LanguageCode(lang, LanguageCode.CodeTypes.ISO_639_2);

        if(lang == null || lp.getSupportedLanguages().contains(code)) {
            try {
                return lp.detectLanguage(text);
            } catch (IOException e) {
                log.error(e);
            } catch (ClassNotFoundException e) {
                log.error(e);
            }
        }

        else{
            return new Result(tc.categorize(Utils.removePuntuation(text)), true, 2);
        }

        return null;
    }


    private Result mostCommon(List<Result> list) {

        if(list == null || list.size() == 0)
            return null;
        
	if(list.size() == 1)
            return list.get(0);

        Map<Result, Integer> map_count = new HashMap<>();
	Map<String, Double> map_conf = new HashMap<>();
	Result maxScoring = null;
	double max_confidence_score = -10.0;
        for (Result t : list) {
            Integer val = map_count.get(t);
            map_count.put(t, val == null ? 1 : val + 1);
	    Double conf = map_conf.get(t.languageCode);
	    map_conf.put(t.languageCode, conf == null ? t.confidence : conf + t.confidence);
	    if (t.confidence > max_confidence_score) {
		max_confidence_score = t.confidence;
		maxScoring = t;
	    }
        }

	if(map_count.size() == list.size()){  //if sizes are the same it means that all the values in list are unique
	    return maxScoring;
        }

        Map.Entry<Result, Integer> max = null;
        for (Map.Entry<Result, Integer> e : map_count.entrySet()) {
            if (max == null || e.getValue() > max.getValue())
                max = e;
        }

	Result maj_vote = max.getKey();
	
	double average_confidence = map_conf.get(maj_vote.languageCode) / max.getValue();
	//int count = max.getValue(); 
	//for (Result t: list) {
	//   if (t.languageCode.equals(maj_vote.languageCode)){
	//   	average_confidence += t.confidence;
	//   }
	//}
	//average_confidence /= count;

	maj_vote.engine = "maj_vote";
	maj_vote.confidence = average_confidence;
        return maj_vote;
    }
}
