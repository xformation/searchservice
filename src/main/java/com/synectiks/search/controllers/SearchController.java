/**
 * 
 */
package com.synectiks.search.controllers;

import java.util.List;
import java.util.Map;

import org.codehaus.jettison.json.JSONObject;
import org.elasticsearch.action.search.SearchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.synectiks.commons.constants.IConsts;
import com.synectiks.commons.entities.SourceEntity;
import com.synectiks.commons.entities.search.ESEvent;
import com.synectiks.commons.interfaces.IApiController;
import com.synectiks.commons.utils.IUtils;
import com.synectiks.search.manager.SearchManager;
import com.synectiks.search.queries.Aggregator;
import com.synectiks.search.receiver.SearchESEventReceiver;
import com.synectiks.search.utils.IESUtils;

/**
 * @author Rajesh
 */
@RestController
@RequestMapping(path = IApiController.API_PATH
		+ IApiController.URL_SEARCH, method = RequestMethod.POST)
@CrossOrigin
public class SearchController {

	private static final Logger logger = LoggerFactory
			.getLogger(SearchController.class);

	@Autowired
	private SearchManager searchManger;
	@Autowired
	private SearchESEventReceiver receiver;

	/**
	 * Api to get the elastic documents source json as list,
	 * by elastic documents ids list.
	 * @param cls
	 * @param ids
	 * @return
	 */
	@RequestMapping("/getDocs")
	public ResponseEntity<Object> getDocsById(
			@RequestParam(value = "cls") String cls,
			@RequestParam List<String> ids) {

		logger.info(cls + ", " + ids);
		List<String> docs = null;
		try {
			// Search in specified fields with page numbers
			docs  = searchManger.getDocsById(cls, ids);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			return new ResponseEntity<>(IUtils.getFailedResponse(ex),
					HttpStatus.PRECONDITION_FAILED);
		}
		return new ResponseEntity<>(docs, HttpStatus.OK);
	}

	/**
	 * Api to create a new index in elastic if not index not exists.
	 * Also add the index mappings for new entity. We can call it to
	 * update then existing index mappings too using isUpdate field.
	 * @param cls
	 * @param mappings
	 * @param isUpdate
	 * @return
	 */
	@RequestMapping("/setIndexMapping")
	public ResponseEntity<Object> putMapping(
			@RequestParam(value = "cls") String cls,
			@RequestParam(value = "mappings") String mappings,
			@RequestParam(name = "isUpdate",
					required = false) boolean isUpdate) {

		logger.info(cls + ", " + mappings + ", " + isUpdate);
		boolean searchResults = false;
		try {
			// Search in specified fields with page numbers
			searchResults  = searchManger.putMapping(cls, mappings, isUpdate);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			return new ResponseEntity<>(IUtils.getFailedResponse(ex),
					HttpStatus.PRECONDITION_FAILED);
		}
		return new ResponseEntity<>(searchResults, HttpStatus.OK);
	}

	/**
	 * API to return mappings for entity class.
	 * @param cls
	 * @param fieldsOnly if true then you will get list of fieldnames
	 * @return
	 */
	@RequestMapping(path = "/getIndexMapping", method = RequestMethod.GET)
	public ResponseEntity<Object> getMapping(
			@RequestParam(value = "cls") String cls,
			@RequestParam(name = "fieldsOnly",
					required = false) boolean fieldsOnly) {
		logger.info(cls + ", " + fieldsOnly);
		Object res = null;
		try {SourceEntity.class.getName();
			// Search in specified fields with page numbers
			@SuppressWarnings("rawtypes")
			Map mappings  = searchManger.gettMapping(cls);
			if (fieldsOnly) {
				res = IESUtils.getFieldsFromMappings(mappings);
			} else {
				res = mappings;
			}
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			return new ResponseEntity<>(IUtils.getFailedResponse(ex),
					HttpStatus.PRECONDITION_FAILED);
		}
		return new ResponseEntity<>(res, HttpStatus.OK);
	}

	/**
	 * API to search for elastic query json string
	 * @param elsQuery
	 * @param pageNo
	 * @param pageSize
	 * @return {@code SearchResponse} object
	 */
	@RequestMapping("/elsQuery")
	public ResponseEntity<Object> elsQuerySearch(
			@RequestParam(name = "query") String elsQuery,
			@RequestParam(name = "cls", required = false) String cls,
			@RequestParam(name = "pageNo",
					required = false, defaultValue = "1") int pageNo,
			@RequestParam(name = "pageSize",
					required = false, defaultValue = "10") int pageSize) {
		Object res = null;
		try {
			logger.info("ElsQuery: " + elsQuery);
			// Search in specified fields with page numbers
			SearchResponse searchResults = searchManger.elsSearch(
					elsQuery, cls, pageNo, pageSize);
			res = IUtils.createFromSearchResponse(searchResults);
			logger.info("Result: " + res);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			return new ResponseEntity<>(IUtils.getFailedResponse(ex),
					HttpStatus.PRECONDITION_FAILED);
		}
		return new ResponseEntity<>(res.toString(), HttpStatus.OK);
	}

	/**
	 * API {@code /search/query} for searching a query string in all
	 * entities or in provided entities. with all fields or in provided 
	 * fields. The result in form of entity class {@code List}
	 * @param q query string which needs to be search in repository
	 * @param cls fully qualified name of entity class
	 * @param fields comma separated list of entity class fields
	 * @param pageNo page number to send in result, 1 - based i.e. 1 for first
	 * @param pageSize size of page in result
	 * @return {@code List} of entities in {@code ResponseEntity} body
	 */
	@RequestMapping("/query")
	public ResponseEntity<Object> searchString(@RequestParam(value = "q") String q,
			@RequestParam(name = "cls", required = false) String cls,
			@RequestParam(name = "fields", required = false) String fields,
			@RequestParam(name = "pageNo",
					required = false, defaultValue = "0") int pageNo,
			@RequestParam(name = "pageSize",
					required = false, defaultValue = "0") int pageSize) {
		List<?> searchResults = null;
		try {
			// Search in specified fields with page numbers
			searchResults = searchManger.search(
					q, cls, fields, pageNo, pageSize);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			return new ResponseEntity<>(IUtils.getFailedResponse(ex),
					HttpStatus.PRECONDITION_FAILED);
		}
		return new ResponseEntity<>(searchResults, HttpStatus.OK);
	}

	/**
	 * API {@code /api/v1/search/list} for get list of entities based on
	 * filters provided as json object.
	 * @param cls (<b>*</b> required) fully qualified name of entity class
	 * @param filters Json object to hold filter conditions<br/>
	 * Filters json format:<br/>
	 * <pre>
	 * {
	 * 	"and": [{
	 * 		"field-name-1": "value-1",
	 * 		...
	 * 	}],
	 * 	"or": [{
	 * 		"field-name-1": "value-1",
	 * 		"field-name-2": "value-2",
	 * 		...
	 * 	}],
	 * 	"not": [{
	 * 		"field-name-1": "value-1",
	 * 		...
	 * 	}],
	 * 	"filters": [{
	 * 		"field-name-1": "value-1",
	 * 		"ranges": [{
	 * 			"<b>type</b>": "[Number|Date|String]",
	 * 			"<b>fieldName</b>": "field-name",
	 * 			"<b>from</b>": "start from value",
	 * 			"<b>to</b>": "end to value",
	 * 			"locale": "local-value if any i.e <b>en_IN</b> for india",
	 * 			"format": "value format if date string is not in format
	 * 				<b>yyyy-MM-dd'T'HH:mm:ss.SSSz</b>"
	 * 		}]
	 * 	}]
	 * }
	 * </pre>
	 * @param pageNo page number to send in result, 1 - based i.e. 1 for first
	 * @param pageSize size of page in result
	 * @return {@code List} of entities in {@code ResponseEntity} body
	 */
	@RequestMapping(value = "/list", method = RequestMethod.POST)
	public ResponseEntity<Object> searchEntities(
			@RequestParam(value = "cls") String cls,
			@RequestParam(name = "filters",
					required = false, defaultValue = "{}") String filters,
			@RequestParam(name = "pageNo",
					required = false, defaultValue = "0") int pageNo,
			@RequestParam(name = "pageSize",
					required = false, defaultValue = "0") int pageSize) {
		List<?> searchResults = null;
		try {
				// Search in specified fields
				searchResults = searchManger.search(filters, cls, pageNo, pageSize);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			return new ResponseEntity<>(IUtils.getFailedResponse(ex),
					HttpStatus.PRECONDITION_FAILED);
		}
		return new ResponseEntity<>(searchResults, HttpStatus.OK);
	}

	/**
	 * API {@code /api/v1/search/count} for get count of entities based on
	 * filters provided as json object.
	 * <br/>
	 * For params doc {@see #searchEntities(String, String, int, int)}
	 * <br/>
	 * @return {@code Long} count of entities in {@code ResponseEntity} body
	 */
	@RequestMapping(value = "/count", method = RequestMethod.POST)
	public ResponseEntity<Object> countEntities(
			@RequestParam(name = "cls") String cls,
			@RequestParam(name = "filters",
					required = false, defaultValue = "{}") String filters) {
		long searchResults = 0l;
		try {
			// Search in specified fields
			searchResults = searchManger.count(filters, cls);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			return new ResponseEntity<>(IUtils.getFailedResponse(ex),
					HttpStatus.PRECONDITION_FAILED);
		}
		return new ResponseEntity<>(searchResults, HttpStatus.OK);
	}

	/**
	 * API {@code /api/v1/search/aggregateCounts} to get aggregate count of
	 * entities based on filters provided as json object.
	 * @param cls fully qualified name of entity class i.e.
	 * {@code com.girnarsoft.delite.lms.entities.es.TestEnquiry}
	 * @param aggregator json object with aggregation filters<br/>
	 * Aggregator json format:<br/>
	 * <pre>
	 * {
	 * 	<b>"aggreType"</b>: "count | terms | avg | ranges | min | max | sum",
	 * 	<b>"fieldType"</b>: "field-type",
	 * 	<b>"fieldName"</b>: "field-name",
	 * 	"interval": "1d" -- "[\d+][s|m|h|d|w|M|q|y]"
	 * 	"ranges": [{"from": X, "to": Y}], -- only used for numbers
	 * 	"values": ["ABC", "DEF"],
	 * 	"locale": "local-value if any",
	 * 	"format": "value format if needs formated key"
	 * }
	 * </pre>
	 * <br/>
	 * For params doc {@see #searchEntities(String, String, int, int)}
	 * <br/>
	 * @return {@code Map} of aggregation key, doc_count values
	 * in {@code ResponseEntity} body
	 */
	@RequestMapping(value = "/aggregateCounts", method = RequestMethod.POST)
	public ResponseEntity<Object> aggregateCounts(
			@RequestParam(name = "cls") String cls,
			@RequestParam(name = "aggregator") JSONObject aggregator,
			@RequestParam(name = "filters",
					required = false, defaultValue = "{}") String filters) {
		Map<String, Object> res = null;
		try {
			Aggregator aggre = IUtils.OBJECT_MAPPER
					.readerFor(Aggregator.class)
					.readValue(aggregator.toString());
			// Fetch aggregated results
			res = searchManger.aggreCounts(filters, cls, aggre);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			return new ResponseEntity<>(IUtils.getFailedResponse(ex),
					HttpStatus.PRECONDITION_FAILED);
		}
		return new ResponseEntity<>(res, HttpStatus.OK);
	}

	/**
	 * API {@code /api/v1/search/fireEvent} to call an index entity event.
	 * @param cls fully qualified entity class name
	 * @param eventType {@code EventType} name from enum",
	 * @param entity [entity object in json string form]
	 * @return event result or error details in {@code ResponseEntity} body.
	 */
	@RequestMapping(value = "/fireEvent", method = RequestMethod.POST)
	public ResponseEntity<Object> fireEvent(
			@RequestParam(name = IConsts.PRM_CLASS) String cls,
			@RequestParam(name = IConsts.PRM_EV_TYPE) String eventType,
			@RequestParam(name = IConsts.PRM_ENTITY) String entity) {
		Object res = null;
		try {
			logger.info("Event[ cls = " +
					cls + ", type: " + eventType + ", entity: " + entity + "]");
			ESEvent event = IESUtils.createEvent(cls, eventType, entity);
			logger.info("fire: " + event);
			res = receiver.handleEvent(event);
		} catch (Throwable ex) {
			logger.error(ex.getMessage(), ex);
			return new ResponseEntity<>(IUtils.getFailedResponse(ex),
					HttpStatus.PRECONDITION_FAILED);
		}
		logger.info("Res: " + res);
		return new ResponseEntity<>(res, HttpStatus.OK);
	}

}
