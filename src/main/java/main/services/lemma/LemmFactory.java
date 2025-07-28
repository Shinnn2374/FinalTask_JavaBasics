package main.services.lemma;

import lombok.AllArgsConstructor;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
public class LemmFactory {
    private static final Logger logger = LoggerFactory.getLogger(LemmFactory.class);
    private static LuceneMorphology luceneMorphology;

    static {
        try {
            luceneMorphology = new RussianLuceneMorphology();
        } catch (IOException e) {
            logger.error("Failed to initialize LuceneMorphology", e);
            throw new RuntimeException("Failed to initialize LuceneMorphology", e);
        }
    }

    private final String[] wordsToLemmatize;

    public List<String> getLemms() {
        if (luceneMorphology == null) {
            throw new IllegalStateException("LuceneMorphology not initialized");
        }

        List<String> resultLemms = new ArrayList<>();
        List<String> tempLemms = new ArrayList<>();

        for (String textPart : wordsToLemmatize) {
            if (textPart.isEmpty()) {
                continue;
            }

            tempLemms.addAll(luceneMorphology.getMorphInfo(textPart));
            for (String lemm : tempLemms) {
                if (servicePartExist(lemm)) {
                    continue;
                }
                resultLemms.add(getLemmWord(lemm));
            }
            tempLemms.clear();
        }

        return resultLemms;
    }

    public Map<String, String> getLemmsToRelevantPageLoader() {
        if (luceneMorphology == null) {
            throw new IllegalStateException("LuceneMorphology not initialized");
        }

        Map<String, String> resultLemmsAndBaseWords = new HashMap<>();

        for (String word : wordsToLemmatize) {
            luceneMorphology.getNormalForms(word.toLowerCase())
                    .forEach(normalForm -> resultLemmsAndBaseWords.put(word, normalForm));
        }

        return resultLemmsAndBaseWords;
    }

    private boolean servicePartExist(String lemm) {
        return lemm.matches(".*(СОЮЗ|МЕЖД|ПРЕДЛ|ЧАСТ).*");
    }

    private String getLemmWord(String lemm) {
        return lemm.substring(0, lemm.indexOf("|"));
    }
}