package main.data.repository;

import main.data.model.Page;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface PageRepository extends CrudRepository<Page, Integer> {
    List<Page> findBySiteId(int siteId);
    List<Page> findBySiteIdAndIdIn(HashMap<Integer, Page> siteId, Set<Integer> ids);
    Optional<Page> findByPathAndSiteId(String path, int siteId);

}
