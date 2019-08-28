# search

This application was generated using JHipster 6.2.0, you can find documentation and help at [https://www.jhipster.tech/documentation-archive/v6.2.0](https://www.jhipster.tech/documentation-archive/v6.2.0).

## Development

Before you can build this project, you must install and configure the following dependencies on your machine:

1. [Node.js][]: We use Node to run a development web server and build the project.
   Depending on your system, you can install Node either from source or as a pre-packaged bundle.

After installing Node, you should be able to run the following command to install development tools.
You will only need to run this command when dependencies change in [package.json](package.json).

    npm install

We use npm scripts and [Webpack][] as our build system.

Run the following commands in two separate terminals to create a blissful development experience where your browser
auto-refreshes when files change on your hard drive.

    ./mvnw
    npm start

Npm is also used to manage CSS and JavaScript dependencies used in this application. You can upgrade dependencies by
specifying a newer version in [package.json](package.json). You can also run `npm update` and `npm install` to manage dependencies.
Add the `help` flag on any command to see how you can use it. For example, `npm help update`.

The `npm run` command will list all of the scripts available to run for this project.

### PWA Support

JHipster ships with PWA (Progressive Web App) support, and it's disabled by default. One of the main components of a PWA is a service worker.

The service worker initialization code is commented out by default. To enable it, uncomment the following code in `src/main/webapp/index.html`:

```html
<script>
  if ('serviceWorker' in navigator) {
    navigator.serviceWorker.register('./service-worker.js').then(function() {
      console.log('Service Worker Registered');
    });
  }
</script>
```

Note: [Workbox](https://developers.google.com/web/tools/workbox/) powers JHipster's service worker. It dynamically generates the `service-worker.js` file.

### Managing dependencies

For example, to add [Leaflet][] library as a runtime dependency of your application, you would run following command:

    npm install --save --save-exact leaflet

To benefit from TypeScript type definitions from [DefinitelyTyped][] repository in development, you would run following command:

    npm install --save-dev --save-exact @types/leaflet

Then you would import the JS and CSS files specified in library's installation instructions so that [Webpack][] knows about them:
Note: There are still a few other things remaining to do for Leaflet that we won't detail here.

For further instructions on how to develop with JHipster, have a look at [Using JHipster in development][].

## Building for production

### Packaging as jar

To build the final jar and optimize the search application for production, run:

    ./mvnw -Pprod clean verify

This will concatenate and minify the client CSS and JavaScript files. It will also modify `index.html` so it references these new files.
To ensure everything worked, run:

    java -jar target/*.jar

Then navigate to [http://localhost:8080](http://localhost:8080) in your browser.

Refer to [Using JHipster in production][] for more details.

### Packaging as war

To package your application as a war in order to deploy it to an application server, run:

    ./mvnw -Pprod,war clean verify

## Testing

To launch your application's tests, run:

    ./mvnw verify

### Client tests

Unit tests are run by [Jest][] and written with [Jasmine][]. They're located in [src/test/javascript/](src/test/javascript/) and can be run with:

    npm test

For more information, refer to the [Running tests page][].

### Code quality

Sonar is used to analyse code quality. You can start a local Sonar server (accessible on http://localhost:9001) with:

```
docker-compose -f src/main/docker/sonar.yml up -d
```

You can run a Sonar analysis with using the [sonar-scanner](https://docs.sonarqube.org/display/SCAN/Analyzing+with+SonarQube+Scanner) or by using the maven plugin.

Then, run a Sonar analysis:

```
./mvnw -Pprod clean verify sonar:sonar
```

If you need to re-run the Sonar phase, please be sure to specify at least the `initialize` phase since Sonar properties are loaded from the sonar-project.properties file.

```
./mvnw initialize sonar:sonar
```

or

For more information, refer to the [Code quality page][].

## Using Docker to simplify development (optional)

You can use Docker to improve your JHipster development experience. A number of docker-compose configuration are available in the [src/main/docker](src/main/docker) folder to launch required third party services.

You can also fully dockerize your application and all the services that it depends on.
To achieve this, first build a docker image of your app by running:

    ./mvnw -Pprod verify jib:dockerBuild

Then run:

    docker-compose -f src/main/docker/app.yml up -d

For more information refer to [Using Docker and Docker-Compose][], this page also contains information on the docker-compose sub-generator (`jhipster docker-compose`), which is able to generate docker configurations for one or several JHipster applications.

## Continuous Integration (optional)

To configure CI for your project, run the ci-cd sub-generator (`jhipster ci-cd`), this will let you generate configuration files for a number of Continuous Integration systems. Consult the [Setting up Continuous Integration][] page for more information.

[jhipster homepage and latest documentation]: https://www.jhipster.tech
[jhipster 6.2.0 archive]: https://www.jhipster.tech/documentation-archive/v6.2.0
[using jhipster in development]: https://www.jhipster.tech/documentation-archive/v6.2.0/development/
[using docker and docker-compose]: https://www.jhipster.tech/documentation-archive/v6.2.0/docker-compose
[using jhipster in production]: https://www.jhipster.tech/documentation-archive/v6.2.0/production/
[running tests page]: https://www.jhipster.tech/documentation-archive/v6.2.0/running-tests/
[code quality page]: https://www.jhipster.tech/documentation-archive/v6.2.0/code-quality/
[setting up continuous integration]: https://www.jhipster.tech/documentation-archive/v6.2.0/setting-up-ci/
[node.js]: https://nodejs.org/
[yarn]: https://yarnpkg.org/
[webpack]: https://webpack.github.io/
[angular cli]: https://cli.angular.io/
[browsersync]: http://www.browsersync.io/
[jest]: https://facebook.github.io/jest/
[jasmine]: http://jasmine.github.io/2.0/introduction.html
[protractor]: https://angular.github.io/protractor/
[leaflet]: http://leafletjs.com/
[definitelytyped]: http://definitelytyped.org/

# searchservice

### What is this repository for?

common search services project for any application based on elasticsearch

### How to import project for editing

- Import as maven project in your IDE

### Build, install and run application

To get started build the build the latest sources with Maven 3 and Java 8
(or higher).

    $ cd search
    $ mvn clean install

You can run this application as spring-boot app by following command:

    $ mvn spring-boot:run

Once done you can run the application by executing

    $ java -jar target/search-exec.jar

## Application api's documentation

### /search/getDocs

Api to get the elastic documents source json as list, by elastic documents ids list.

    Method: POST
    Params:
    	cls	*		String 	fully qualified name of entity class
    	ids 		List<String> list or arrays of string ids
    Response:
    	Entity class mappings or List of field names if fieldsOnly is true

### /search/getIndexMapping

Api to create a new index in elastic if not index not exists. Also add the index mappings for new entity. We can call it to update then existing index mappings too using isUpdate field.

    Method: POST
    Params:
    	cls	*			String 	fully qualified name of entity class
    	fieldsOnly if true then you will get list of fieldnames
    Response:
    	Entity class mappings or List of field names if fieldsOnly is true

### /search/setIndexMapping

Api to create a new index in elastic if not index not exists. Also add the index mappings for new entity. We can call it to update then existing index mappings too using isUpdate field.

    Method: POST
    Params:
    	cls	*			String 	fully qualified name of entity class
    	mappings*		String		json object string for mappings of document
    	isUpdate		Boolean	send true if you would like to update the existing index mappings.
    Response:
    	true if new index and mapping get updated successfully in elasticsearch.

### /search/elsQuery

Api to get the list of child nodes by absolute node path.

    Method: POST
    Params:
    	query*		String 	absolute node path
    	pageNo		Integer	page number, 1 - based i.e. 1 for first
    	pageSize	Integer	size of page in result
    Response:
    	{}		Json string of org.elasticsearch.action.search.SearchResponse objects

### /search/query

Api for searching a query string in all entities or in provided entities. with all fields or in provided fields. The result in form of entity class List

    Method: POST
    Params:
    	q*			String		query string which needs to be search in repository
    	cls			String		fully qualified name of entity class
    	fields		String		comma separated list of entity class fields
    	pageNo		Integer	page number, 1 - based i.e. 1 for first
    	pageSize	Integer	size of page in result
    Response:
    	{}		List of search result entities

### /search/list

Api to get list of entities based on filters provided as json object.

    Method: POST
    Params:
    	cls (* required) fully qualified name of entity class
    	filters Json object to hold filter conditions, json format:
    		{
    			"and": [{
    				"field-name-1": "value-1",
    				...
    			}],
    			"or": [{
    				"field-name-1": "value-1",
    				"field-name-2": "value-2",
    				...
    			}],
    			"not": [{
    				"field-name-1": "value-1",
    				...
    			}],
    			"filters": [{
    				"field-name-1": "value-1",
    				"ranges": [{
    					"type": "[Number|Date|String]",
    					"fieldName": "field-name",
    					"from": "start from value",
    					"to": "end to value",
    					"locale": "local-value if any i.e en_IN for india",
    					"format": "value format if date string is not in format
    						yyyy-MM-dd'T'HH:mm:ss.SSSz"
    				}]
    			}]
    		}
    	pageNo		Integer
    	pageSize	Integer
    Response:
    	{}		Json list of result objects

### /search/count

Api to get count of entities based on filters provided as json object.

    Method: POST
    Params:
    	cls (* required) fully qualified name of entity class
    	filters Json object to hold filter conditions, json format:
    		{
    			"and": [{
    				"field-name-1": "value-1",
    				...
    			}],
    			"or": [{
    				"field-name-1": "value-1",
    				"field-name-2": "value-2",
    				...
    			}],
    			"not": [{
    				"field-name-1": "value-1",
    				...
    			}],
    			"filters": [{
    				"field-name-1": "value-1",
    				"ranges": [{
    					"type": "[Number|Date|String]",
    					"fieldName": "field-name",
    					"from": "start from value",
    					"to": "end to value",
    					"locale": "local-value if any i.e en_IN for india",
    					"format": "value format if date string is not in format
    						yyyy-MM-dd'T'HH:mm:ss.SSSz"
    				}]
    			}]
    		}
    	pageNo		Integer
    	pageSize	Integer
    Response:
    	xx		Long result count

### /search/aggregateCounts

Api to get the list of child nodes by absolute node path.

    Method: POST
    Params:
    	cls (* required) fully qualified name of entity class
    	aggregator json object with aggregation filters, json format:
    		{
    			"aggreType": "count | terms | avg | ranges | min | max | sum",
    			"fieldType": "field-type",
    			"fieldName": "field-name",
    			"interval": "1d" -- "[\d+][s|m|h|d|w|M|q|y]"
    			"ranges": [{"from": X, "to": Y}], -- only used for numbers
    			"values": ["ABC", "DEF"],
    			"locale": "local-value if any",
    			"format": "value format if needs formated key"
    		}
    	filters Json object to hold filter conditions, json format:
    		{
    			"and": [{
    				"field-name-1": "value-1",
    				...
    			}],
    			"or": [{
    				"field-name-1": "value-1",
    				"field-name-2": "value-2",
    				...
    			}],
    			"not": [{
    				"field-name-1": "value-1",
    				...
    			}],
    			"filters": [{
    				"field-name-1": "value-1",
    				"ranges": [{
    					"type": "[Number|Date|String]",
    					"fieldName": "field-name",
    					"from": "start from value",
    					"to": "end to value",
    					"locale": "local-value if any i.e en_IN for india",
    					"format": "value format if date string is not in format
    						yyyy-MM-dd'T'HH:mm:ss.SSSz"
    				}]
    			}]
    		}
    	pageNo		Integer
    	pageSize	Integer
    Response:
    	{}		Json Map of aggregation key, doc_count values

### /search/fireEvent

Api to call an index entity event.

    Method: POST
    Params:
    	cls	*			String fully qualified entity class name
    	eventType*	String EventType name from enum",
    	entity	*		JSon	 [entity object in json string form]
    Response:
    	{}		event result or error details

### Who do I talk to?

    Please mail us on
    info@syenctiks.com
