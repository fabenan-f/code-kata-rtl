package de.rtl.codekata;

import java.util.List;

import de.rtl.codekata.config.ImporterConfig;
import de.rtl.codekata.model.Article;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.mongodb.MongoClientException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import de.rtl.codekata.service.ArticleImporterService;
import lombok.extern.slf4j.Slf4j;

@Component
@EnableScheduling
@Slf4j
public class CMSImporter {
    private static int QUERY_LIMIT = 10;

    private ArticleImporterService articleService;
    private ImporterConfig config;
    private RestClient restClient;

    public CMSImporter(ArticleImporterService service, ImporterConfig config) {
        articleService = service;
        this.config = config;
        restClient = RestClient.create();
    }

    @Scheduled(cron = "${cron.post}")
    public void importArticles() throws InterruptedException {
        List<Article> articles = articleService.retrieveArticles();
        int retries = 0;
        while (retries < 3) {
            try {
                restClient.post()
                    .uri(config.getUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(articles)
                    .retrieve()
                    .toBodilessEntity();
                break;
            } catch (RestClientException exception) {
                log.error("Exception {} while trying to import articles", exception.getMessage());
                retries++;
                if (retries == 3) {
                    log.info("After 3 failed retries {} articles will be temporarely stored", articles.size());
                    storeArticles(articles); // temporary persistent storage for later retry
                } else {
                    Thread.sleep((long) Math.pow(retries, 2) * 1000); // exponential backoff
                }
            }
        }
    }

    private void storeArticles(List<Article> articles) {
        try (MongoClient mongoClient = MongoClients.create(config.getMongoDBConnection())) {
            MongoTemplate mongoTemplate = new MongoTemplate(mongoClient, config.getMongoDBDatabase());
            mongoTemplate.insertAll(articles);
        } catch (MongoClientException mongoDBException) {
            log.error("Exception {} while inserting articles", mongoDBException);
        }
    }

    @Scheduled(cron = "${cron.retry}")
    public void retryImportArticles() {
        try (MongoClient mongoClient = MongoClients.create(config.getMongoDBConnection())) {
            MongoTemplate mongoTemplate = new MongoTemplate(mongoClient, config.getMongoDBDatabase());
            Query query = new Query().limit(QUERY_LIMIT);
            Query countQuery = new Query();
            long count = mongoTemplate.count(countQuery, Article.class);
            while (count > 0) {
                List<Article> articles = mongoTemplate.find(query, Article.class);
                restClient.post()
                    .uri(config.getUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(articles)
                    .retrieve()
                    .toBodilessEntity();
                mongoTemplate.remove(query, Article.class);
                count = mongoTemplate.count(countQuery, Article.class);
            }
        } catch(RestClientException | MongoClientException exception) {
            log.error("Exception {} while retrying importing articles", exception.getMessage());
        }
    }
}
