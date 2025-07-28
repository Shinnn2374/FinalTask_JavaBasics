package searchengine.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.config.Site;
import searchengine.model.site;

@Repository
public interface SiteRepository extends JpaRepository<site, Integer> {
    Site findByUrl(String url);
}
