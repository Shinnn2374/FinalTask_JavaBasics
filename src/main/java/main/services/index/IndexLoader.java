package main.services.index;

import lombok.NoArgsConstructor;
import main.data.model.Index;
import main.data.model.Lemma;
import main.data.repository.IndexRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@NoArgsConstructor(force = true)
public class IndexLoader {

    private final IndexRepository indexRepository;

    @Autowired
    public IndexLoader(IndexRepository indexRepository) {
        this.indexRepository = indexRepository;
    }

    public HashMap<Integer, Index> loadIndexFromDB(int pageId) {
        HashMap<Integer, Index> existingIndexes = new HashMap<>();
        List<Index> indexes = indexRepository.findByPageId(pageId);
        indexes.forEach(index -> existingIndexes.put(index.getLemmaId(), index));
        return existingIndexes;
    }

    public ArrayList<Index> loadIndexFromDBByPageIdAndLemmas(Collection<Integer> pagesId, Set<Integer> lemmasId) {
        return new ArrayList<>(indexRepository.findByPageIdInAndLemmaIdIn(pagesId, lemmasId));
    }

    public ArrayList<Index> loadIndexFromDBByLemmas(Set<Integer> lemmasId) {
        return new ArrayList<>(indexRepository.findByLemmaIdIn(lemmasId));
    }

    public ArrayList<Index> loadIndexFromListByLemmas(List<Index> indexList, List<Lemma> searchLemmas) {
        ArrayList<Index> existingIndexes = new ArrayList<>();
        Set<Integer> searchLemmasId = new HashSet<>();
        searchLemmas.forEach(lemma -> searchLemmasId.add(lemma.getId()));

        for (Index indexFromDB : indexList) {
            if(searchLemmasId.contains(indexFromDB.getLemmaId())) {
                existingIndexes.add(indexFromDB);
            }
        }
        return existingIndexes;
    }
}