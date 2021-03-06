package edu.columbia.main.normalization;

import com.aliasi.util.Strings;
import edu.columbia.main.language_id.LanguageDetector;
import edu.columbia.main.language_id.Result;
import edu.columbia.main.Utils;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import edu.columbia.main.configuration.BabelConfig;
import static edu.columbia.main.normalization.tagBBN.lp;

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
public class NISTLanguageTagger {

    static Logger log = Logger.getLogger(NISTLanguageTagger.class);
    static LanguageDetector lp = new LanguageDetector();

    public static void start(String dirIn, String dirOut, String langCode) throws Exception {
        File dir = new File(dirIn);
        File[] listOfFiles = dir.listFiles();

        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                String filename = listOfFiles[i].getName();
                System.out.println(filename);
                if (filename.endsWith(".txt")) {
                    processTextFile(dirIn + "/" + filename, dirOut + "/" + filename, langCode);
                }
            }
        }
    }

    private static void processTranscriptionFile(String path, String saveTo, String langCode) throws Exception {
        File newLangFile = new File(saveTo);
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(newLangFile), StandardCharsets.UTF_8));

        File contentFile = new File(path);
        BufferedReader br = new BufferedReader(new FileReader(contentFile));

        String line;
        HashSet<String> langAnchors = loadAnchors(langCode, langCode);
        HashSet<String> engAnchors = loadAnchors(langCode, "eng");

        while ((line = br.readLine()) != null) {
            if (line.startsWith("[")) {
                bw.write(line + "\n");
            } else {
                String outputBlock = outputTranscriptionLine(langCode, line, langAnchors, engAnchors);
                bw.write(outputBlock);
            }
        }
        br.close();
        bw.close();
    }

    private static void processTextFile(String path, String saveTo, String langCode) throws Exception {
        File newLangFile = new File(saveTo);
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(newLangFile), StandardCharsets.UTF_8));

        File contentFile = new File(path);
        BufferedReader br = new BufferedReader(new FileReader(contentFile));

        String line;

        HashSet<String> langAnchors = loadAnchors(langCode, langCode);
        HashSet<String> engAnchors = loadAnchors(langCode, "eng");

        String outputBlock = "";
        String text = "";
        while ((line = br.readLine()) != null) {
            if (!line.isEmpty()) {
                text += line + "\n";
                outputBlock += outputTaggingLine(langCode, line, langAnchors, engAnchors);
            }
        }
        bw.write(outputTaggingDoc(langCode, text,langAnchors, engAnchors));
        bw.write(outputBlock);
        bw.write(outputEndDoc());
        
        br.close();
        bw.close();
    }

    public static HashSet<String> loadAnchors(String primaryLang, String langCode) throws Exception {
        HashSet<String> anchors = new HashSet<String>();
        String anchorsFilename = "weak_anchors/" + primaryLang + "/" + langCode + "_anchors.txt";
        InputStream is = NISTLanguageTagger.class.getClassLoader().getResourceAsStream(anchorsFilename);
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String line = "";
        while ((line = br.readLine()) != null) {
            anchors.add(line);
        }
        return anchors;
    }

    public static String outputTaggingLines(String langCode, ArrayList<String> lines, HashSet<String> langAnchors, HashSet<String> engAnchors) {
        String output = "";
        for (String line : lines) {
            output += outputTaggingLine(langCode, line, langAnchors, engAnchors);
        }
        return output;
    }

    public static String outputTaggingLine(String langCode, String line, HashSet<String> langAnchors, HashSet<String> engAnchors) {
        Result res = lp.detectLanguage(line, langCode);
        String output = "<s> " + processLine(line, langCode, langAnchors, engAnchors) + " </s " + (makeAttribute("engine", res.engine) + makeAttribute("languageCode", res.languageCode) + makeAttribute("score", String.valueOf(res.confidence))) + " > \n";
        return output;
    }

    public static String outputTaggingDoc(String langCode, String doc, HashSet<String> langAnchors, HashSet<String> engAnchors) {
        Result res = lp.detectLanguage(doc, langCode);
        String output = "<doc " + (makeAttribute("engine", res.engine) + makeAttribute("languageCode", res.languageCode) + makeAttribute("score", String.valueOf(res.confidence))) + " > \n";
        return output;
    }
    
    public static String outputEndDoc() {
        return "</doc>\n";
    }
    
    public static String outputTranscriptionLine(String langCode, String line, HashSet<String> langAnchors, HashSet<String> engAnchors) {
        Pattern pattern = Pattern.compile("[0-9]*\\.?[0-9]+");
        String output;
        if (line.contains("inLine") || line.contains("outLine")) {
            int idx = line.indexOf("Line");
            String prefix = line.substring(0, idx + 4);
            String text = line.substring(idx + 4);
            String untagged_text = text.replaceAll("<.*>", "");
            Result res = lp.detectLanguage(untagged_text, langCode);
            output = prefix + " <s> " + processLine(text, langCode, langAnchors, engAnchors) + " </s " + (makeAttribute("engine", res.engine) + makeAttribute("languageCode", res.languageCode) + makeAttribute("score", String.valueOf(res.confidence))) + " > \n";
        } else if (line.matches("[0-9]*\\.?[0-9]+.*")) {
            output = line + "\n";
        } else {
            String untagged_text = line.replaceAll("<.*>", "");
            Result res = lp.detectLanguage(untagged_text, langCode);
            output = "<s> " + processLine(line, langCode, langAnchors, engAnchors) + " </s " + (makeAttribute("engine", res.engine) + makeAttribute("languageCode", res.languageCode) + makeAttribute("score", String.valueOf(res.confidence))) + " > \n";
        }
        return output;
    }

    public static String processLine(String line, String langCode, HashSet<String> langAnchors, HashSet<String> engAnchors) {
        String[] tokens = line.split(" ");
        String newLine = "";
        for (String token : tokens) {
            token = token.toLowerCase();
            if (langAnchors.contains(token)) {
                newLine += "<" + token + ":" + langCode + "> ";
            } else if (engAnchors.contains(token)) {
                newLine += "<" + token + ":eng> ";
            } else {
                newLine += token + " ";
            }
        }
        return newLine;
    }

    public static int countWords(String s) {
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

    private static String makeAttribute(String att, String value) {
        return att + "=\"" + value + "\" ";
    }
    
    public static void main(String[] args) throws Exception {
        start(args[0], args[1], args[2]);
  }

}
