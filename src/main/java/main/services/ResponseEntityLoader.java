package main.services;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import main.data.model.Site;
import main.data.model.Status;
import main.data.repository.SiteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@NoArgsConstructor(force = true)
public class ResponseEntityLoader {

    private final SiteRepository siteRepository;
    private final Map<String, ResponseEntity<Map<String, Object>>> responseCache = new ConcurrentHashMap<>();

    @Autowired
    public ResponseEntityLoader(SiteRepository siteRepository) {
        this.siteRepository = siteRepository;
        initializeResponseCache();
    }

    private void initializeResponseCache() {
        // Статические ответы
        responseCache.put("indexingAlreadyStarted", createResponse(false, "Индексация уже запущена", HttpStatus.FORBIDDEN));
        responseCache.put("controllerMethodStart", createResponse(true, null, HttpStatus.OK));
        responseCache.put("indexingNotStarted", createResponse(false, "Индексация не запущена", HttpStatus.FORBIDDEN));
        responseCache.put("pageOutOfRange", createResponse(false, "Данная страница находится за пределами сайтов, указанных в конфигурационном файле", HttpStatus.NOT_FOUND));
        responseCache.put("emptySearchQuery", createResponse(false, "Отсутствует поисковый запрос", HttpStatus.NOT_FOUND));
        responseCache.put("siteNotFound", createResponse(false, "Запрашиваемый сайт отсутствует в базе данных", HttpStatus.NOT_FOUND));
        responseCache.put("indexedSitesNotFound", createResponse(false, "Отсутствуют проиндексированные сайты", HttpStatus.NOT_FOUND));
        responseCache.put("searchMatchesNotFound", createResponse(false, "Отсутствуют совпадения", HttpStatus.NOT_FOUND));
        responseCache.put("relevantPagesNotFound", createResponse(false, "Отсутствует вывод найденных совпадений", HttpStatus.NOT_FOUND));
    }

    private ResponseEntity<Map<String, Object>> createResponse(boolean result, String error, HttpStatus status) {
        return new ResponseEntity<>(Map.of(
                "result", result,
                "error", error != null ? error : ""
        ), status);
    }

    public ResponseEntity<Map<String, Object>> getIndexingAlreadyStartResponse() {
        return responseCache.get("indexingAlreadyStarted");
    }

    public ResponseEntity<Map<String, Object>> getControllerMethodStartResponse() {
        return responseCache.get("controllerMethodStart");
    }

    public ResponseEntity<Map<String, Object>> getIndexingNotStartResponse() {
        return responseCache.get("indexingNotStarted");
    }

    public ResponseEntity<Map<String, Object>> getPageOutOfRangeResponse() {
        return responseCache.get("pageOutOfRange");
    }

    public ResponseEntity<Map<String, Object>> getEmptySearchQueryResponse() {
        return responseCache.get("emptySearchQuery");
    }

    public ResponseEntity<Map<String, Object>> getSiteNotFoundResponse() {
        return responseCache.get("siteNotFound");
    }

    public ResponseEntity<Map<String, Object>> getIndexedSitesNotFoundResponse() {
        return responseCache.get("indexedSitesNotFound");
    }

    public ResponseEntity<Map<String, Object>> getSearchMatchesNotFoundResponse() {
        return responseCache.get("searchMatchesNotFound");
    }

    public ResponseEntity<Map<String, Object>> getRelevantPagesNotFoundResponse() {
        return responseCache.get("relevantPagesNotFound");
    }

    // Методы с динамической логикой
    public ResponseEntity<Map<String, Object>> getSiteIndexingOrEmptyPagesResponse(Site targetSite) {
        Site site = siteRepository.findById(targetSite.getId()).orElse(null);
        if (site == null) {
            return responseCache.get("siteNotFound");
        }

        return site.getStatus() == Status.INDEXING ?
                responseCache.get("indexingAlreadyStarted") :
                createResponse(false, "Запрашиваемый сайт не имеет страниц в базе данных", HttpStatus.NOT_FOUND);
    }

    public ResponseEntity<Map<String, Object>> getSiteIndexingOrEmptyLemmasResponse(Site targetSite) {
        Site site = siteRepository.findById(targetSite.getId()).orElse(null);
        if (site == null) {
            return responseCache.get("siteNotFound");
        }

        return site.getStatus() == Status.INDEXING ?
                responseCache.get("indexingAlreadyStarted") :
                createResponse(false, "Запрашиваемый сайт не имеет лемм в базе данных", HttpStatus.NOT_FOUND);
    }

    public ResponseEntity<Map<String, Object>> getSiteIndexingOrEmptyIndexesResponse(Site targetSite) {
        Site site = siteRepository.findById(targetSite.getId()).orElse(null);
        if (site == null) {
            return responseCache.get("siteNotFound");
        }

        return site.getStatus() == Status.INDEXING ?
                responseCache.get("indexingAlreadyStarted") :
                createResponse(false, "Запрашиваемый сайт не имеет индексов в базе данных", HttpStatus.NOT_FOUND);
    }
}