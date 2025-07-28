package searchengine.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.config.Site;
import searchengine.model.page;

public interface PageRepository extends JpaRepository<page, Integer> {
    boolean existsBySiteAndPath(Site site, String path);
    long countBySite(Site site);
}
