package main.data.repository;

import main.data.model.Site;
import main.data.model.Status;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SiteRepository extends CrudRepository<Site, Integer> {
    List<Site> findByStatus(Status status);
}
