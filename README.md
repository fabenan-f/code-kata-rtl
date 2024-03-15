Aufgabe: 
 
Eine Java Spring Boot Applikation (✅) schreiben mit folgenden Anforderungen: 
Der Bean ArticleImporterService steht zur Verfügung (via eine Maven-Dependency) und liefert eine Liste von Artikeln zurück, wenn die Methode retrieveArticles() aufgerufen wird (✅): 

(❕Annahme: Artikeldaten sind kurzlebig, nicht für immer aufrufbar -> siehe Zwischenspeicher in MongoDB bei fehlgeschlagenen CMS Import)

```
public interface ArticleImporterService { 
	List<Article> retrieveArticles();
}
```
	 
Article ist ein Pojo: 

 ```
public class Article { 
	private String id; 
	private String title; 
	private String kicker; 
	private Markup articleText; 
	private Date creationDate; 
	private List<Picture> pictures; 
	private Status status; 
... etc 
}
```
      
Die Applikation sollte periodisch (alle 5 Minuten z.B.) die obige Methode abrufen, um Artikel zu bekommen. 
(✅ Cron-Job) 

Die von dem obigen Service gelieferten Artikel sollten via einer Rest API in einem CMS erstellt werden: 
Create (POST):  https:/cms-rest-api/create 

```
{ 
	"title": "xxx", 
	"kicker": "yyy", 
	"articleText": "zzzy 
	"creationDate": "12/09/2022", 
	"pictures": { 
		  ... 
	}, 
}
```
(✅ Rest-Client)

Wenn der Create-Aufruf fehlschlägt, sollte die Applikation nochmal versuchen (nach 10 Minuten z.B.) den jeweiligen Artikel zu erstellen.  
(✅ Retry-Mechanismus: Exponential Backoff + Daten werden in MongoDB zwischengespeichert, wenn Import trotzdem fehlschlägt, und mit zweiten Cron-Job importiert)

Die Applikation sollte zukünftig in Kubernetes Clusters deployed werden, deshalb sollte die verschiedenen Parameter konfigurierbar sein (Rest API Endpoint, die Zeiten für die periodischen Abrufe, etc ).
(✅ Werte können in application.properties konfiguriert werden) 

Die Applikation muss nicht lauffähig sein.

Zwei Möglichkeiten Applikation in Aktion zu sehen:
- Integrationstests: `mvn test`
- Simples Docker Compose: `docker-compose up`
