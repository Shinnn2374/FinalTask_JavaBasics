package main.data.repository;

import main.data.model.Index;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@Repository
public interface IndexRepository extends CrudRepository<Index, Integer> {

    List<Index> findByPageId(int pageId);
    List<Index> findByLemmaIdIn(Set<Integer> lemmaIds);
    List<Index> findByPageIdInAndLemmaIdIn(Collection<Integer> pageIds, Set<Integer> lemmaIds);

}
