package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dao.PageRepository;
import searchengine.dao.SiteRepository;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.model.Page;
import searchengine.model.SiteEntity;
import searchengine.model.page;
import searchengine.model.site;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.IndexService;
import searchengine.services.IndexingService;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexService {

    private final SitesList sitesList;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private ForkJoinPool forkJoinPool;
    private boolean isIndexingStopped = false;

    @Override
    public IndexingResponse startIndexing() {
        if (isIndexingRunning()) {
            return new IndexingResponse(false, "Индексация уже запущена");
        }

        isIndexingStopped = false;
        List<Site> sites = sitesList.getSites();

        forkJoinPool = new ForkJoinPool();
        forkJoinPool.submit(() -> sites.parallelStream().forEach(this::indexSite));

        return new IndexingResponse(true);
    }

    private void indexSite(Site siteConfig) {
        SiteEntity siteEntity = new SiteEntity();
        siteEntity.setName(siteConfig.getName());
        siteEntity.setUrl(siteConfig.getUrl());
        siteEntity.setStatus(SiteEntity.Status.INDEXING);
        siteEntity.setStatusTime(LocalDateTime.now());
        siteRepository.save(siteEntity);

        try {
            indexPage(siteConfig.getUrl(), siteEntity);
            siteEntity.setStatus(SiteEntity.Status.INDEXED);
        } catch (Exception e) {
            siteEntity.setStatus(SiteEntity.Status.FAILED);
            siteEntity.setLastError(e.getMessage());
        } finally {
            siteEntity.setStatusTime(LocalDateTime.now());
            siteRepository.save(siteEntity);
        }
    }

    private void indexPage(String url, site site) throws IOException, InterruptedException {
        if (isIndexingStopped) {
            throw new InterruptedException("Индексация остановлена пользователем");
        }

        Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                .referrer("http://www.google.com")
                .get();

        page page = new page();
        page.setSite(site);
        page.setPath(url.replace(site.getUrl(), ""));
        page.setCode(doc.connection().response().statusCode());
        page.setContent(doc.html());
        pageRepository.save(page);

        // Обновляем время статуса
        site.setStatusTime(LocalDateTime.now());
        siteRepository.save(site);

        // Задержка между запросами
        TimeUnit.SECONDS.sleep(1);

        // Рекурсивный обход ссылок
        doc.select("a[href]").forEach(link -> {
            String nextUrl = link.absUrl("href");
            if (shouldIndex(nextUrl, site.getUrl())) {
                try {
                    indexPage(nextUrl, site);
                } catch (Exception e) {
                    // Логируем ошибку, но продолжаем индексацию
                }
            }
        });
    }

    private boolean shouldIndex(String url, String siteUrl) {
        return url.startsWith(siteUrl) &&
                !url.contains("#") &&
                !url.matches("(?i).*\\.(pdf|jpg|jpeg|png|gif|zip|rar)$") &&
                !pageRepository.existsBySiteAndPath(siteRepository.findByUrl(siteUrl),
                        url.replace(siteUrl, ""));
    }

    @Override
    public IndexingResponse stopIndexing() {
        if (!isIndexingRunning()) {
            return new IndexingResponse(false, "Индексация не запущена");
        }

        isIndexingStopped = true;
        forkJoinPool.shutdownNow();

        siteRepository.findAllByStatus(site.status.INDEXING).forEach(site -> {
            site.setStatus(SiteEntity.Status.FAILED);
            site.setLastError("Индексация остановлена пользователем");
            site.setStatusTime(LocalDateTime.now());
            siteRepository.save(site);
        });

        return new IndexingResponse(true);
    }

    @Override
    public IndexingResponse indexPage(String url) {
        // Реализация индексации отдельной страницы
        return null;
    }

    private boolean isIndexingRunning() {
        return forkJoinPool != null && !forkJoinPool.isTerminated();
    }
}