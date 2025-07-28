package main.services.lemma;

import lombok.NoArgsConstructor;
import main.data.model.Index;
import main.data.model.Lemma;
import main.data.model.Site;
import main.data.model.Status;
import main.data.repository.LemmaRepository;
import main.data.repository.SiteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@NoArgsConstructor(force = true)
public class LemmasLoader {

    @Value("${lemma-frequency.percent}")
    private int percent;

    private final LemmaRepository lemmaRepository;
    private final SiteRepository siteRepository;

    @Autowired
    public LemmasLoader(LemmaRepository lemmaRepository, SiteRepository siteRepository) {
        this.lemmaRepository = lemmaRepository;
        this.siteRepository = siteRepository;
    }

    public Map<String, Lemma> loadSiteLemmasFromDB(int siteId) {
        return lemmaRepository.findBySiteId(siteId)
                .stream()
                .collect(Collectors.toMap(
                        Lemma::getLemma,
                        lemma -> lemma,
                        (existing, replacement) -> existing,
                        HashMap::new
                ));
    }

    public Map<String, Lemma> loadLemmasFromDBWithIndex(Set<Integer> existingIndexes) {
        return lemmaRepository.findByIdIn(existingIndexes)
                .stream()
                .collect(Collectors.toMap(
                        Lemma::getLemma,
                        lemma -> lemma,
                        (existing, replacement) -> existing,
                        HashMap::new
                ));
    }

    public Map<Integer, Lemma> loadSiteLemmasFromDBWithFreq(int siteId, long allPageCount) {
        return lemmaRepository.findBySiteId(siteId)
                .stream()
                .filter(lemma -> !lemmaFrequencyIsOften(lemma, allPageCount))
                .collect(Collectors.toMap(
                        Lemma::getId,
                        lemma -> lemma,
                        (existing, replacement) -> existing,
                        HashMap::new
                ));
    }

    public Map<Integer, Lemma> loadLemmasFromDBWithFreqAndIndexedSites(long allPageCount) {
        List<Integer> indexedSiteIds = siteRepository.findByStatus(Status.INDEXED)
                .stream()
                .map(Site::getId)
                .collect(Collectors.toList());

        return lemmaRepository.findBySiteIdIn(indexedSiteIds)
                .stream()
                .filter(lemma -> !lemmaFrequencyIsOften(lemma, allPageCount))
                .collect(Collectors.toMap(
                        Lemma::getId,
                        lemma -> lemma,
                        (existing, replacement) -> existing,
                        HashMap::new
                ));
    }

    private boolean lemmaFrequencyIsOften(Lemma lemma, long allPageCount) {
        if (percent >= 100) {
            return false;
        }
        return lemma.getFrequency() > (allPageCount) - (allPageCount / 100.00) * (100 - percent);
    }
}