# searchservice

### What is this repository for? ###
common search services project for any application based on elasticsearch

### How to import project for editing ###

* Import as maven project in your IDE

### Build, install and run application ###

To get started build the build the latest sources with Maven 3 and Java 8 
(or higher). 

	$ cd search
	$ mvn clean install 

You can run this application as spring-boot app by following command:

	$ mvn spring-boot:run

Once done you can run the application by executing 

	$ java -jar target/search-exec.jar

## Application api's documentation ##

### /search/setIndexMapping

Api to create a new index in elastic if not index not exists. Also add the index mappings for new entity. We can call it to update then existing index mappings too using isUpdate field.

	Method: POST
	Params:
		cls	*			String 	fully qualified name of entity class
		mappings*		String		json object string for mappings of document
		isUpdate		Boolean	send true if you would like to update the existing index mappings.
	Response:
		true if new index and mapping get updated successfully in elasticsearch.


### /search/elsQuery ###

Api to get the list of child nodes by absolute node path.

	Method: POST
	Params:
		query*		String 	absolute node path
		pageNo		Integer	page number, 1 - based i.e. 1 for first
		pageSize	Integer	size of page in result
	Response:
		{}		Json string of org.elasticsearch.action.search.SearchResponse objects

### /search/query ###

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

### /search/list ###

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

### /search/count ###

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

### /search/aggregateCounts ###

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

### /search/fireEvent ###

Api to call an index entity event.

	Method: POST
	Params:
		cls	*			String fully qualified entity class name
		eventType*	String EventType name from enum",
		entity	*		JSon	 [entity object in json string form]
	Response:
		{}		event result or error details

### Who do I talk to? ###
	Please mail us on
	info@syenctiks.com
