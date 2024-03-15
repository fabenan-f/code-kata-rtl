package de.rtl.codekata.service;

import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;

import de.rtl.codekata.model.Article;

@Service
public class MockArticleImporterService implements ArticleImporterService {

    private int id = 20;

    @Override
    public List<Article> retrieveArticles() {
        return List.of(
            new Article(++id, "title", "kicker", new Date()),
            new Article(++id, "title", "kicker", new Date())
        );
    }
    
}
