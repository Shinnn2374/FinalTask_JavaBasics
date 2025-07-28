package searchengine.services;

import searchengine.dto.index.IndexingResponse;

public interface IndexService {
    IndexingResponse startIndexing();
    IndexingResponse stopIndexing();
    IndexingResponse indexPage(String url);
}
