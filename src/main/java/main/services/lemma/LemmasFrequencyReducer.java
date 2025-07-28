package main.services.lemma;

import lombok.NoArgsConstructor;
import main.data.model.Lemma;
import main.data.repository.LemmaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Component
@NoArgsConstructor(force = true)
public class LemmasFrequencyReducer {

    private final LemmaRepository lemmaRepository;

    @Autowired
    public LemmasFrequencyReducer(LemmaRepository lemmaRepository) {
        this.lemmaRepository = lemmaRepository;
    }

    @Transactional
    public void reduceLemmasFrequency(HashMap<String, Lemma> existingLemmas) {
        List<Lemma> lemmasToUpdate = existingLemmas.values().stream()
                .peek(Lemma::decreaseFrequency)
                .filter(lemma -> lemma.getFrequency() > 0)
                .collect(Collectors.toList());

        List<Integer> lemmasToDelete = existingLemmas.values().stream()
                .filter(lemma -> lemma.getFrequency() <= 0)
                .map(Lemma::getId)
                .collect(Collectors.toList());

        if (!lemmasToUpdate.isEmpty()) {
            lemmaRepository.saveAll(lemmasToUpdate);
        }

        if (!lemmasToDelete.isEmpty()) {
            lemmaRepository.deleteAllByIdInBatch(lemmasToDelete);
        }
    }
}