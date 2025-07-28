package main.data.repository;

import main.data.model.Lemma;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface LemmaRepository extends CrudRepository<Lemma, Integer> {
    List<Lemma> findBySiteId(int siteId);
    List<Lemma> findByIdIn(Collection<Integer> ids);
    List<Lemma> findBySiteIdIn(Collection<Integer> siteIds);
    void deleteAllByIdInBatch(List<Integer> lemmasToDelete);
}
