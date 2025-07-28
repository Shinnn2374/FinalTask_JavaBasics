package main.services.site;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;


@Slf4j
public class SiteConnector {

    private Connection.Response cachedResource;
    private String userAgent;
    private String siteUrl;

    public SiteConnector(String userAgent, String siteUrl) {
        this.userAgent = userAgent;
        this.siteUrl = siteUrl;
        setCachedResource();
    }

    private void setCachedResource(){
        try {
           cachedResource = Jsoup.connect(siteUrl).userAgent(userAgent).referrer("http://www.google.com").ignoreHttpErrors(true).maxBodySize(0).execute();
           Thread.sleep(650);
        } catch (Exception exception ) {
            log.info("Could not connect to site at " + siteUrl, exception);
        }
    }

    public Connection.Response getCachedResource() {
        return cachedResource;
    }

    public int getStatusCode(){
        int stat = 0;
        try {
            if (this.cachedResource != null){
                stat = this.cachedResource.statusCode();
            }
        } catch (Exception ex){
            log.info("Could not connect to site at " + siteUrl, ex);
        }
        return stat;
    }

    public Document getSiteDocument(){
        if(this.cachedResource == null){
            return new Document("");
        }
        Document result = null;
        try {
            result = this.cachedResource.parse();
        } catch (Exception exception) {
            log.info("Could not connect to site at " + siteUrl, exception);
        }
        return result;
    }

}
