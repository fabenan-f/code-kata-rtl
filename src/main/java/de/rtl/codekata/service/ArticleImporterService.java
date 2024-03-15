package de.rtl.codekata.service;

import java.util.List;

import de.rtl.codekata.model.Article;

public interface ArticleImporterService {
    List<Article> retrieveArticles();
}
