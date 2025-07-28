package main.services.index;

import lombok.NoArgsConstructor;
import main.data.model.*;
import main.data.repository.*;
import main.services.lemma.*;
import main.services.result.*;
import main.services.site.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ForkJoinPool;

@Service
@NoArgsConstructor(force = true)
@Transactional
public class IndexingServices {

    @Value("${user-agent.name}")
    private String userAgent;

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final FieldRepository fieldRepository;
    private final SiteConditionsChanger siteConditionsChanger;
    private final ResultPageLoader resultPageLoader;
    private final Indexer indexer;
    private final IndexingPageClone indexingPageClone;
    private final IndexLoader indexLoader;
    private final LemmasLoader lemmasLoader;
    private final LemmasFrequencyReducer lemmasFrequencyReducer;

    @Autowired
    public IndexingServices(IndexingPageClone indexingPageClone,
                            SiteRepository siteRepository,
                            PageRepository pageRepository,
                            LemmaRepository lemmaRepository,
                            IndexRepository indexRepository,
                            FieldRepository fieldRepository,
                            SiteConditionsChanger siteConditionsChanger,
                            ResultPageLoader resultPageLoader,
                            LemmasFrequencyReducer lemmasFrequencyReducer,
                            Indexer indexer,
                            IndexLoader indexLoader,
                            LemmasLoader lemmasLoader) {
        this.indexingPageClone = indexingPageClone;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        this.fieldRepository = fieldRepository;
        this.siteConditionsChanger = siteConditionsChanger;
        this.resultPageLoader = resultPageLoader;
        this.lemmasFrequencyReducer = lemmasFrequencyReducer;
        this.indexer = indexer;
        this.indexLoader = indexLoader;
        this.lemmasLoader = lemmasLoader;
    }

    public void indexTargetSite(Site targetSite) {
        ForkJoinPool pagingPool = new ForkJoinPool();
        SiteCrawler siteCrawler = new SiteCrawler(targetSite.getUrl(), userAgent, siteRepository);
        pagingPool.execute(siteCrawler);
        TreeMap<String, Page> results = pagingPool.invoke(siteCrawler);

        if (results.isEmpty() || checkSiteFailed(targetSite.getId())) {
            handleEmptyResults(targetSite);
            return;
        }

        savePages(results.values());
        processLemmas(targetSite, results.values());
    }

    public void indexTargetPage(String url) {
        Site targetSite = new Site();
        siteConditionsChanger.cloneSiteConditionPageIndexing(targetSite, url);

        String targetUrl = targetSite.getUrl().equals(url) ? "/" : url.replace(targetSite.getUrl(), "");
        Page targetPage = prepareTargetPage(targetSite, targetUrl);

        if (targetPage.getId() != 0) {
            cleanupExistingData(targetPage);
        }

        processPageIndexing(targetSite, targetUrl, targetPage);
    }

    private boolean checkSiteFailed(int siteId) {
        return siteRepository.findById(siteId)
                .map(site -> site.getStatus().equals(Status.FAILED))
                .orElse(true);
    }

    private void handleEmptyResults(Site targetSite) {
        if (siteRepository.findById(targetSite.getId())
                .map(site -> site.getLastError() == null)
                .orElse(false)) {
            siteConditionsChanger.changeSiteConditionsEmptyPages(targetSite);
        }
    }

    private void savePages(Collection<Page> pages) {
        pageRepository.saveAll(pages);
    }

    private void processLemmas(Site targetSite, Collection<Page> pages) {
        List<Field> fields = (List<Field>) fieldRepository.findAll();
        TreeMap<Integer, TreeMap<Lemma, Float>> lemmasResult = processLemmatization(pages, fields, targetSite.getId());

        if (lemmasResult.isEmpty() || checkSiteFailed(targetSite.getId())) {
            handleEmptyLemmas(targetSite);
            return;
        }

        saveLemmasAndIndexes(targetSite, lemmasResult);
    }

    private TreeMap<Integer, TreeMap<Lemma, Float>> processLemmatization(Collection<Page> pages,
                                                                         List<Field> fields,
                                                                         int siteId) {
        ForkJoinPool lemmaPool = new ForkJoinPool();
        Lemmatizer lemmatizer = new Lemmatizer(
                resultPageLoader.getCorrectlyResponsivePages(pages),
                fields,
                siteId
        );
        return lemmaPool.invoke(lemmatizer);
    }

    private void handleEmptyLemmas(Site targetSite) {
        if (siteRepository.findById(targetSite.getId())
                .map(site -> site.getLastError() == null)
                .orElse(false)) {
            siteConditionsChanger.changeSiteConditionsEmptyLemmas(targetSite);
        }
    }

    private void saveLemmasAndIndexes(Site targetSite, TreeMap<Integer, TreeMap<Lemma, Float>> lemmasResult) {
        ResultLemmaLoader resultLemmaLoader = new ResultLemmaLoader(lemmasResult.values());
        lemmaRepository.saveAll(resultLemmaLoader.getLemmaResultToDB().values());

        List<Index> indexResult = new ArrayList<>(
                indexer.getIndexes(lemmasResult, resultLemmaLoader.getLemmaResultToDB())
        );

        if (!indexResult.isEmpty()) {
            indexRepository.saveAll(indexResult);
            siteConditionsChanger.changeSiteConditionsSuccessIndexed(targetSite);
        } else {
            siteConditionsChanger.changeSiteConditionsEmptyIndex(targetSite);
        }
    }

    private Page prepareTargetPage(Site targetSite, String targetUrl) {
        Page targetPage = new Page();
        indexingPageClone.partiallyCloneTargetIndexingPage(
                targetPage,
                new Page(targetUrl, targetSite.getId())
        );
        return targetPage;
    }

    private void cleanupExistingData(Page targetPage) {
        Map<Integer, Index> existingIndexes = indexLoader.loadIndexFromDB(targetPage.getId());
        lemmasFrequencyReducer.reduceLemmasFrequency(
                (HashMap<String, Lemma>) lemmasLoader.loadLemmasFromDBWithIndex((Set<Integer>) existingIndexes)
        );
        indexRepository.deleteAll(existingIndexes.values());
    }

    private void processPageIndexing(Site targetSite, String targetUrl, Page targetPage) {
        SiteConnector siteConnector = new SiteConnector(userAgent, targetSite.getUrl() + targetUrl);
        updatePageContent(targetPage, siteConnector);

        List<Page> resultPagesList = Collections.singletonList(
                resultPageLoader.getCorrectlyResponsivePage(targetPage)
        );

        processPageLemmas(targetSite, resultPagesList);
    }

    private void updatePageContent(Page targetPage, SiteConnector siteConnector) {
        targetPage.setAnswerCode(siteConnector.getStatusCode());
        targetPage.setPageContent(siteConnector.getSiteDocument().toString());
        pageRepository.save(targetPage);
    }

    private void processPageLemmas(Site targetSite, List<Page> pages) {
        List<Field> fields = (List<Field>) fieldRepository.findAll();
        TreeMap<Integer, TreeMap<Lemma, Float>> lemmasResult = processLemmatization(pages, fields, targetSite.getId());

        if (lemmasResult.isEmpty() || checkSiteFailed(targetSite.getId())) {
            handlePageLemmasFailure(targetSite);
            return;
        }

        savePageLemmasAndIndexes(targetSite, lemmasResult);
    }

    private void handlePageLemmasFailure(Site targetSite) {
        if (siteRepository.findById(targetSite.getId())
                .map(site -> site.getLastError() == null)
                .orElse(false)) {
            siteConditionsChanger.changeSiteConditionsEmptyLemmasOnPage(targetSite);
        }
    }

    private void savePageLemmasAndIndexes(Site targetSite, TreeMap<Integer, TreeMap<Lemma, Float>> lemmasResult) {
        ResultLemmasNormalizer normalizer = new ResultLemmasNormalizer(
                new ResultLemmaLoader(lemmasResult.values()).getLemmaResultToDB(),
                (HashMap<String, Lemma>) lemmasLoader.loadSiteLemmasFromDB(targetSite.getId())
        );

        lemmaRepository.saveAll(normalizer.getLemmaNormalizedResult().values());

        List<Index> indexResult = new ArrayList<>(
                indexer.getIndexes(lemmasResult, normalizer.getLemmaNormalizedResult())
        );

        if (!indexResult.isEmpty()) {
            indexRepository.saveAll(indexResult);
            siteConditionsChanger.changeSiteConditionsSuccessIndexed(targetSite);
        } else {
            siteConditionsChanger.changeSiteConditionsEmptyIndexOnPage(targetSite);
        }
    }
}