package main.controllers;

import main.data.model.*;
import main.data.repository.*;
import main.services.*;
import main.services.db.DBCleaner;
import main.services.db.DBParamLoader;
import main.services.index.IndexingPageChecker;
import main.services.index.IndexingServices;
import main.services.site.SiteConditionsChanger;
import main.services.site.SiteStatusChecker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
public class IndexController {
    private final SiteRepository siteRepository;
    private final ResponseEntityLoader responseEntityLoader;
    private final DBCleaner dbCleaner;
    private final DBParamLoader dbParamLoader;
    private final SiteStatusChecker siteStatusChecker;
    private final IndexingServices indexingServices;
    private final SiteConditionsChanger siteConditionsChanger;
    private final IndexingPageChecker indexingPageChecker;
    private final ExecutorService executorService;

    @Autowired
    public IndexController(SiteRepository siteRepository,
                           ResponseEntityLoader responseEntityLoader,
                           DBCleaner dbCleaner,
                           DBParamLoader dbParamLoader,
                           SiteStatusChecker siteStatusChecker,
                           IndexingServices indexingServices,
                           SiteConditionsChanger siteConditionsChanger,
                           IndexingPageChecker indexingPageChecker) {
        this.siteRepository = siteRepository;
        this.responseEntityLoader = responseEntityLoader;
        this.dbCleaner = dbCleaner;
        this.dbParamLoader = dbParamLoader;
        this.siteStatusChecker = siteStatusChecker;
        this.indexingServices = indexingServices;
        this.siteConditionsChanger = siteConditionsChanger;
        this.indexingPageChecker = indexingPageChecker;
        this.executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<Map<String, Object>> startIndexing() {
        if (siteStatusChecker.indexingSitesExist()) {
            return responseEntityLoader.getIndexingAlreadyStartResponse();
        }
        dbCleaner.cleanDB();
        dbParamLoader.loadStartParam();
        for (Site siteFromDB : siteRepository.findAll()) {
            siteConditionsChanger.changeSiteConditionsStartIndexing(siteFromDB);
            executorService.execute(() -> indexingServices.indexTargetSite(siteFromDB));
        }
        return responseEntityLoader.getControllerMethodStartResponse();
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<Map<String, Object>> stopIndexing() {
        if(!siteStatusChecker.indexingSitesExist()) {
            return responseEntityLoader.getIndexingNotStartResponse();
        }
        siteConditionsChanger.changeSitesConditionStopIndex();
        return responseEntityLoader.getControllerMethodStartResponse();
    }

    @PostMapping("/indexPage")
    public ResponseEntity<Map<String, Object>> indexPage(@RequestParam String url) {
        if(!indexingPageChecker.indexingPageInRange(url)) {
            return responseEntityLoader.getPageOutOfRangeResponse();
        }
        executorService.execute(() -> indexingServices.indexTargetPage(url));
        return responseEntityLoader.getControllerMethodStartResponse();
    }

    @PreDestroy
    public void shutdownExecutor() {
        executorService.shutdown();
    }
}