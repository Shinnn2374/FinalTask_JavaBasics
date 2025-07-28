package searchengine.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.model.page;

public interface PageRepository extends JpaRepository<page, Integer> {
}
