package edu.columbia.main.normalization;

import com.aliasi.util.Strings;
import edu.columbia.main.language_id.LanguageDetector;
import edu.columbia.main.language_id.Result;
import edu.columbia.main.Utils;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import edu.columbia.main.configuration.BabelConfig;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

/**
 * Created by Gideon on 2/24/15.
 */
public class tagBBN {

    static Logger log = Logger.getLogger(tagBBN.class);
    static LanguageDetector lp = new LanguageDetector();

    public static void start(String path, String saveTo, String langCode) throws Exception {

        File newLangFile = new File(saveTo);
    	BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(newLangFile), StandardCharsets.UTF_8));
        File contentFile = new File(path);
        
	String patternString = "<doc id=\"(.*)\">";
	Pattern pattern = Pattern.compile(patternString);

        log.info("LP LOADED");
        BufferedReader br = new BufferedReader(new FileReader(contentFile));
        String line = "", docId = "";
	ArrayList<String> lines = new ArrayList<String>();

	HashSet<String> langAnchors = loadAnchors(langCode);
	HashSet<String> engAnchors = loadAnchors("eng");

	while ((line = br.readLine()) != null) {
	    Matcher matcher = pattern.matcher(line);
	    if (matcher.find()) {
		docId = matcher.group(1);
	    }
	    else if (line.equals("</doc>")) {
		String outputBlock = outputTagging(langCode, docId, lines, langAnchors, engAnchors);
	        bw.write(outputBlock); 
		docId = "";
		lines = new ArrayList<String>();
	    }
	    else {
		lines.add(line);
	    }
        }
        br.close();
	bw.close();
    }

    public static HashSet<String> loadAnchors(String langCode) throws Exception {
	HashSet<String> anchors = new HashSet<String>();
	String anchorsFilename = "anchors/" + langCode + "_anchors.txt";	
	InputStream is =  tagBBN.class.getClassLoader().getResourceAsStream(anchorsFilename);
	BufferedReader br = new BufferedReader(new InputStreamReader(is));
	String line = "";
	while ((line = br.readLine()) != null) {
		anchors.add(line);
	}
	return anchors;
    }

    public static String outputTagging(String langCode, String docId, ArrayList<String> lines, HashSet<String> langAnchors, HashSet<String> engAnchors) {
	String output = "";
	String docString = "";
	for (String line: lines) {
       		String untagged_text = line.replaceAll("<s>", "").replaceAll("</s.*>", "");
		String clean_text = untagged_text.replaceAll("<NUM.*>", "");
		Result res = lp.detectLanguage(clean_text, langCode);
		output += "<s> " + processLine(untagged_text, langCode, langAnchors, engAnchors) + " </s " + (makeAttribute("engine", res.engine) + makeAttribute("languageCode",res.languageCode) + makeAttribute("score", String.valueOf(res.confidence)))+" > \n";
		
		docString += clean_text + "\n";
	}
	Result res = lp.detectLanguage(docString, langCode);
	String header = "<doc " + (makeAttribute("id", docId) + makeAttribute("engine", res.engine) + makeAttribute("languageCode",res.languageCode) + makeAttribute("score", String.valueOf(res.confidence))) + " >\n";
	String tail = "</doc>\n";
	return header + output + tail;
    }

    public static String processLine(String line, String langCode, HashSet<String> langAnchors, HashSet<String> engAnchors) {
	String[] tokens = line.split(" ");
	String newLine = "";
	for (String token: tokens) {
		if (langAnchors.contains(token)) {
			newLine += "<" + token + ":" + langCode + "> ";
		} else if (engAnchors.contains(token)) {
			newLine += "<" + token + ":eng> ";
		}
		else {
			newLine += token + " ";
		}
	}
	return newLine;
    }


    public static int countWords(String s){

        int wordCount = 0;

        boolean word = false;
        int endOfLine = s.length() - 1;

        for (int i = 0; i < s.length(); i++) {
            // if the char is a letter, word = true.
            if (Character.isLetter(s.charAt(i)) && i != endOfLine) {
                word = true;
                // if char isn't a letter and there have been letters before,
                // counter goes up.
            } else if (!Character.isLetter(s.charAt(i)) && word) {
                wordCount++;
                word = false;
                // last word of String; if it doesn't end with a non letter, it
                // wouldn't count without this.
            } else if (Character.isLetter(s.charAt(i)) && i == endOfLine) {
                wordCount++;
            }
        }
        return wordCount;
    }

    /*
    public static void main(String[] args) throws Exception {

        String path = "/local2/babel/data/columbia-data/normalized/12152014/";
        File dir = new File(path);
        lingpipe ld = new lingpipe();

        for(File langFile : dir.listFiles()){
            File newLangFile = new File("/Users/Gideon/Documents/dev/Babel/bbn/12152014/tagged/"+langFile.getName());
            int langCode = Integer.parseInt(langFile.getName().substring(0,3));

            BufferedReader br = new BufferedReader(new FileReader(langFile));
            String line;
            while ((line = br.readLine()) != null) {

                String text = line.replaceAll("<s>","").replaceAll("</s>","");

                Result res;
                String engine;
                if(langCode < 302) { //Kurmanji,TokPisin,Cebuano
                    //res = ld.detectLanguage(text.replaceAll("<NUM>",""));
                    //engine = "lingPipe";
                    break;
                }
                else{ //Kazakh, Lithuaninan, Telugu
                    res = CLDLanguaeDetector.detect(text.replaceAll("<NUM>",""));
                    engine = "cld";
                }

                text = "<s>"+text+"</s "+makeAttribute("engine",engine)+makeAttribute("languageCode",res.languageCode)+makeAttribute("isReliable", String.valueOf(res.isReliable)) + makeAttribute("score", String.valueOf(res.confidence))+" >";
                FileUtils.writeStringToFile(newLangFile,text, Strings.UTF8,true);

            }
            br.close();
        }

    }
*/
    private static String makeAttribute(String att, String value){

        return att+"=\""+value+"\" ";

    }

}

