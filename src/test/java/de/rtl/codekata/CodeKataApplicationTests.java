package de.rtl.codekata;

import de.rtl.codekata.model.Article;
import de.rtl.codekata.service.MockArticleImporterService;

import org.junit.jupiter.api.Test;
import org.mockserver.client.server.MockServerClient;
import org.mockserver.model.HttpStatusCode;
import org.mockserver.verify.VerificationTimes;
import org.springframework.data.mongodb.core.MongoTemplate;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.util.List;

import static org.mockserver.matchers.Times.exactly;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;

import de.flapdoodle.embed.mongo.transitions.Mongod;
import de.flapdoodle.embed.mongo.transitions.RunningMongodProcess;
import de.flapdoodle.reverse.StateID;
import de.flapdoodle.reverse.TransitionWalker;
import de.flapdoodle.reverse.Transitions;

import de.rtl.codekata.config.ImporterConfig;

import de.flapdoodle.embed.mongo.distribution.Version;

import static org.assertj.core.api.Assertions.*;

class CodeKataApplicationTests {

	private int CMS_PORT = 9876;
	private String CMS_ENDPOINT = "/cms-rest-api/create";
	private String CMS_URL = "http://localhost:" + CMS_PORT + CMS_ENDPOINT;

	private String MONGODB_SCHEME = "mongodb://";
	private String MONGODB_DATABASE = "database";

	private MockServerClient setupServer(int statusCode, int times) {
		MockServerClient mockServerClient = startClientAndServer(CMS_PORT);
		mockServerClient.when(request().withPath(CMS_ENDPOINT), exactly(times))
			.respond(response().withStatusCode(statusCode));
		return mockServerClient;
	}

	@Test
	void testSuccessfulImportRequest() throws InterruptedException {
		MockServerClient mockServer = setupServer(HttpStatusCode.OK_200.code(), 1);

		ImporterConfig config = new ImporterConfig();
		config.setUrl(CMS_URL);
		MockArticleImporterService articleService = new MockArticleImporterService();
		CMSImporter cmsImporter = new CMSImporter(articleService, config);
		cmsImporter.importArticles();

		// verify successful import
		mockServer.verify(request().withPath(CMS_ENDPOINT), VerificationTimes.exactly(1));

		mockServer.stop();
	}

	@Test
	void testFailedImportRequest() throws InterruptedException {
		MockServerClient mockServer = setupServer(HttpStatusCode.INTERNAL_SERVER_ERROR_500.code(), 2);
		
		Transitions transitions = Mongod.instance().transitions(Version.V6_0_8);
		try (TransitionWalker.ReachedState<RunningMongodProcess> running = transitions.walker().initState(StateID.of(RunningMongodProcess.class))) {
			String mongoDBAddress = running.current().getServerAddress().toString();

			ImporterConfig config = new ImporterConfig();
			config.setUrl(CMS_URL);
			config.setMongoDBConnection(MONGODB_SCHEME + mongoDBAddress);
			config.setMongoDBDatabase(MONGODB_DATABASE);
			MockArticleImporterService articleService = new MockArticleImporterService();
			CMSImporter cmsImporter = new CMSImporter(articleService, config);
			cmsImporter.importArticles();

			MongoTemplate mongoTemplate = new MongoTemplate(MongoClients.create(config.getMongoDBConnection()), config.getMongoDBDatabase());
			List<Article> storedArticles = mongoTemplate.findAll(Article.class);

			assertThat(storedArticles.size()).isEqualTo(articleService.retrieveArticles().size());
		}

		// verify failed import even with exponential backoff strategy
		mockServer.verify(request().withPath(CMS_ENDPOINT), VerificationTimes.exactly(3));
		mockServer.stop();
	}

	@Test
	void testSuccessfulRetryImportRequest() {
		MockServerClient mockServer = setupServer(HttpStatusCode.OK_200.code(), 1);
		
		Transitions transitions = Mongod.instance().transitions(Version.V6_0_8);
		try (TransitionWalker.ReachedState<RunningMongodProcess> running = transitions.walker().initState(StateID.of(RunningMongodProcess.class))) {
			String mongoDBAddress = running.current().getServerAddress().toString();
			
			ImporterConfig config = new ImporterConfig();
			config.setUrl(CMS_URL);
			config.setMongoDBConnection(MONGODB_SCHEME + mongoDBAddress);
			config.setMongoDBDatabase(MONGODB_DATABASE);
			
			// given that articles are temporarely stored because of failed first import
			try (MongoClient mongoClient = MongoClients.create(config.getMongoDBConnection())) {
				MongoTemplate mongoTemplate = new MongoTemplate(mongoClient, config.getMongoDBDatabase());
				MockArticleImporterService articleService = new MockArticleImporterService();
				mongoTemplate.insertAll(articleService.retrieveArticles());
	
				// when retry import happens
				CMSImporter cmsImporter = new CMSImporter(null, config);
				cmsImporter.retryImportArticles();
				
				List<Article> storedArticles = mongoTemplate.findAll(Article.class);
	
				// then after successful retry import no articles are stored anymore
				assertThat(storedArticles.size()).isEqualTo(0);
			}
		}
		
		// verify successful retry import
		mockServer.verify(request().withPath(CMS_ENDPOINT), VerificationTimes.exactly(1));
		mockServer.stop();
	}

	@Test
	void testFailedRetryImportRequest() {
		MockServerClient mockServer = setupServer(HttpStatusCode.INTERNAL_SERVER_ERROR_500.code(), 1);
		
		Transitions transitions = Mongod.instance().transitions(Version.V6_0_8);
		try (TransitionWalker.ReachedState<RunningMongodProcess> running = transitions.walker().initState(StateID.of(RunningMongodProcess.class))) {
			String mongoDBAddress = running.current().getServerAddress().toString();
			
			ImporterConfig config = new ImporterConfig();
			config.setUrl(CMS_URL);
			config.setMongoDBConnection(MONGODB_SCHEME + mongoDBAddress);
			config.setMongoDBDatabase(MONGODB_DATABASE);
			
			// given that articles are temporarely stored because of failed first import
			try (MongoClient mongoClient = MongoClients.create(config.getMongoDBConnection())) {
				MongoTemplate mongoTemplate = new MongoTemplate(mongoClient, config.getMongoDBDatabase());
				MockArticleImporterService articleService = new MockArticleImporterService();
				mongoTemplate.insertAll(articleService.retrieveArticles());
	
				// when retry import happens
				CMSImporter cmsImporter = new CMSImporter(null, config);
				cmsImporter.retryImportArticles();
				
				List<Article> storedArticles = mongoTemplate.findAll(Article.class);
	
				// then after failed retry import articles are still stored
				assertThat(storedArticles.size()).isEqualTo(articleService.retrieveArticles().size());
			}
		}
		
		// verify failed retry import
		mockServer.verify(request().withPath(CMS_ENDPOINT), VerificationTimes.exactly(1));
		mockServer.stop();
	}
}
