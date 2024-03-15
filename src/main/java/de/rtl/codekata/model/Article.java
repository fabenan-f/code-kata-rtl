package de.rtl.codekata.model;

import java.util.Date;

public record Article(int id, String title, String kicker, Date creationDate) {}
