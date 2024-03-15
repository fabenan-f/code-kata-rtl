package de.rtl.codekata.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Configuration
@Data
public class ImporterConfig {
    
    @Value("${rest.post}")
    private String url;

    @Value("${mongodb.connection}")
    private String mongoDBConnection;

    @Value("${mongodb.database}")
    private String mongoDBDatabase;
}
