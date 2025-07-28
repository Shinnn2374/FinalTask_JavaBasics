package main.services.page;

import lombok.NoArgsConstructor;
import main.data.model.Page;
import main.data.repository.PageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Component
@NoArgsConstructor(force = true)
public class PageLoader {

    private final PageRepository pageRepository;

    @Autowired
    public PageLoader(PageRepository pageRepository) {
        this.pageRepository = pageRepository;
    }

    @Transactional(readOnly = true)
    public Map<Integer, Page> loadSitePagesFromDB(int siteId) {
        return pageRepository.findBySiteId(siteId)
                .stream()
                .collect(Collectors.toMap(
                        Page::getId,
                        page -> page,
                        (existing, replacement) -> existing,
                        HashMap::new
                ));
    }

    @Transactional(readOnly = true)
    public List<Page> loadPagesByIdFromTargetPages(HashMap<Integer, Page> siteId, Set<Integer> pageIds) {
        return pageRepository.findBySiteIdAndIdIn(siteId, pageIds);
    }

    @Transactional(readOnly = true)
    public List<Page> loadPagesByIDFromPagesRepository(Set<Integer> pageIds) {
        return (List<Page>) pageRepository.findAllById(pageIds);
    }

    @Transactional(readOnly = true)
    public Optional<Page> findPageByPathAndSiteId(String path, int siteId) {
        return pageRepository.findByPathAndSiteId(path, siteId);
    }
}