/**
 * 
 */
package com.synectiks.search.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.lucene.search.join.ScoreMode;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;

import com.synectiks.commons.constants.IConsts;
import com.synectiks.commons.entities.dynamodb.Entity;
import com.synectiks.commons.entities.search.ESEvent;
import com.synectiks.commons.entities.search.ESEvent.EventType;
import com.synectiks.commons.utils.IUtils;
import com.synectiks.search.queries.ESExpression.Filters;
import com.synectiks.search.queries.RangeFilter;

/**
 * @author Rajesh
 */
public interface IESUtils {

	Logger logger = LoggerFactory.getLogger(IESUtils.class);

	/**
	 * Enum to hold elastic search bool query type
	 * @author Rajesh
	 */
	enum ESQryType {
		FILTER,
		MATCH,
		NOT,
		SHOULD;
	}

	/**
	 * Method to get id from entity
	 * @param entity
	 * @return string id
	 */
	static String getESID(Object entity) {
		if (!IUtils.isNull(entity) && entity instanceof Entity) {
			return ((Entity) entity).getId();
		}
		return null;
	}

	/**
	 * Get index name from entity annotation
	 * @param entity
	 * @return
	 */
	static String getIndexName(Object entity) {
		if (!IUtils.isNull(entity)) {
			Document doc = entity.getClass().getAnnotation(Document.class);
			if (!IUtils.isNull(doc)) {
				return doc.indexName();
			}
		}
		return null;
	}

	/**
	 * Return index type from entity annotation.
	 * @param entity
	 * @return
	 */
	static String getIndexType(Object entity) {
		if (!IUtils.isNull(entity)) {
			Document doc = entity.getClass().getAnnotation(Document.class);
			if (!IUtils.isNull(doc)) {
				return doc.type();
			}
		}
		return null;
	}

	/**
	 * Method to create {@code SearchQuery} object for {@code QueryBuilder}.
	 * To search in specified indices and with {@code Pageable} page size.
	 * We can also pass aggregation builder to add into current seaarch.
	 * @param qBuilder {@code BoolQueryBuilder} object
	 * @param indxName comma separated names of indices to searc
	 * @param pageReq {@code PageRequest} object with page number and size
	 * @param aggre {@code AbstractAggregationBuilder} object
	 * @return {@code SearchQuery} instance
	 */
	static SearchQuery getNativeSearchQuery(BoolQueryBuilder qBuilder,
			String indxName, Pageable pageReq, AbstractAggregationBuilder<?> aggre) {
		NativeSearchQueryBuilder sQryBuilder = new NativeSearchQueryBuilder();
		if (!IUtils.isNull(qBuilder)) {
			sQryBuilder.withQuery(qBuilder);
		}
		if (!IUtils.isNull(pageReq)) {
			sQryBuilder.withPageable(pageReq);
		}
		if (!IUtils.isNullOrEmpty(indxName)) {
			sQryBuilder.withIndices(indxName);
		}
		if (!IUtils.isNull(aggre)) {
			sQryBuilder.addAggregation(aggre);
		}
		logger.info("Final Query: " + sQryBuilder.toString());
		return sQryBuilder.build();
	}

	/**
	 * Method to check if we are scrolling result further {@link Index#}
	 * @param page
	 * @param size
	 * @return
	 */
	static boolean isScrollQuery(int page, int size) {
		if (page > 0 || size > 0) {
			long res = (page < 1 ? 1 : page) * size;
			if (res > IConsts.MAX_ES_RESULT_SIZE) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Method to generate {@code PageRequest} object
	 * @param pageNo
	 * @param pageSize
	 * @return
	 */
	static PageRequest getPageRequest(int pageNo, int pageSize) {
		PageRequest pgReq = null;
		if (pageNo > 0 || pageSize > 0) {
			pageNo = (pageNo > 0 ? (pageNo - 1) : IConsts.DEF_PAGE);
			pageSize = (pageSize > 1 ? pageSize : IConsts.PAGE_SIZE);
			pgReq = PageRequest.of(pageNo, pageSize);
		}
		return pgReq;
	}

	/**
	 * Method to generate a bool query from QueryBuilder
	 * @param qryType
	 * @param qStrBuilder
	 * @return 
	 */
	static BoolQueryBuilder createBoolQuery(ESQryType qryType, QueryBuilder qBuilder) {
		BoolQueryBuilder boolQB = QueryBuilders.boolQuery();
		return boolQueryBuilder(boolQB, qryType, qBuilder);
	}

	/**
	 * Method sets query builder in bool query builder with clause.
	 * @param boolQB
	 * @param qryType
	 * @param qBuilder
	 * @return
	 */
	static BoolQueryBuilder boolQueryBuilder(BoolQueryBuilder boolQB, ESQryType qryType,
			QueryBuilder qBuilder) {
		if (!IUtils.isNull(boolQB) && !IUtils.isNull(qBuilder)) {
			switch (qryType) {
			case FILTER:
				boolQB.filter(qBuilder);
				break;
			case MATCH:
				boolQB.must(qBuilder);
				break;
			case NOT:
				boolQB.mustNot(qBuilder);
				break;
			case SHOULD:
			default:
				boolQB.should(qBuilder);
				break;
			}
		}
		return boolQB;
	}

	/**
	 * Method to process {@code Filters} to crate a query builder
	 * @param filters
	 * @param qryType
	 * @param boolQB
	 * @return
	 */
	static void processFilter(List<Filters> filters,
			ESQryType qryType, BoolQueryBuilder boolQB) {
		if (!IUtils.isNull(filters) && ! filters.isEmpty()) {
			for (Filters filter : filters) {
				processFilterMap(filter, qryType, boolQB);
			}
		}
	}

	/**
	 * Process filter map to generate query
	 * @param filter
	 */
	static void processFilterMap(Filters filter,
			ESQryType qryType, BoolQueryBuilder boolQB) {
		if (!IUtils.isNull(filter)) {
			Map<String, String> map = filter.getFilters();
			if (!IUtils.isNull(map) && !map.isEmpty()) {
				for (String key : map.keySet()) {
					if (IConsts.RANGES.equals(key)) {
						addRangeFilters(key, map.get(key), qryType, boolQB);
					} else {
						addSearchQuery(key, map.get(key), qryType, boolQB);
					}
				}
			}
		}
	}

	/**
	 * Method to create a range query filter based on json string
	 * @param key String property name
	 * @param value json string
	 * @param qryType 
	 * @param bool Boolean Query builder
	 * @return {@code RangeQueryBuilder} or null
	 */
	static void addRangeFilters(String key,
			String value, ESQryType qryType, BoolQueryBuilder qb) {
		if (!IUtils.isNullOrEmpty(key)) {
			String[] arr = IUtils.getArrayFromJsonString(value);
			for (int indx = 0; indx < arr.length; indx++) {
				try {
					RangeFilter range = IUtils.OBJECT_MAPPER
							.readValue(arr[indx], RangeFilter.class);
					if (!IUtils.isNull(range)) {
						RangeQueryBuilder builder = range.createQueryBuilder();
						boolQueryBuilder(qb, null, builder);
					}
				} catch (Throwable e) {
					logger.error(e.getMessage(), e);
				}
			}
		}
	}

	/**
	 * Method to create a query based on key and value for it in json object
	 * @param key String property name
	 * @param value json string
	 * @param qryType 
	 * @param bool Boolean Query builder
	 * @return {@code QueryBuilder} or null
	 */
	static void addSearchQuery(String key,
			String value, ESQryType qryType, BoolQueryBuilder qb) {
		QueryBuilder builder = null;
		if (!IUtils.isNullOrEmpty(key)) {
			// Create a term filter for it
			if (!IUtils.isNullOrEmpty(value)) {
				builder = getQueryBuilderFromKeyValue(key, value, qryType);
			} else {
				builder = QueryBuilders.existsQuery(key);
			}
		}
		// Add query into bool query
		boolQueryBuilder(qb, qryType, builder);
	}

	/**
	 * Method to generate a {@code QueryBuilder} from key and value
	 * @param key query key field name
	 * @param val string to be searched
	 * @param qryType 
	 * @return {@code QueryBuilder} object
	 */
	static QueryBuilder getQueryBuilderFromKeyValue(
			String key, String val, ESQryType qryType) {
		QueryBuilder builder = null;
		if (!IUtils.isNullOrEmpty(key) &&
				!IUtils.isNullOrEmpty(val)) {
			// Get help on creating nested field for entity in java doc of IESEntity
			if (key.startsWith("_Nst") && key.contains(".")) {
				builder = createNestedQueryBuilder(key.substring(4), val, qryType);
			} else if (val.contains("*") || val.contains("?")) {
				builder = QueryBuilders.wildcardQuery(key, val);
			} else {
				builder = getTermQueryBuilder(key, val, qryType);
			}
		}
		return builder;
	}

	/**
	 * Method to create {@code QueryBuilder} for nested query string
	 * @param key nested query key must have "." separated path
	 * @param val string to be search in field
	 * @param qryType 
	 * @return {@code NestedQueryBuilder} instance
	 */
	static QueryBuilder createNestedQueryBuilder(
			String key, String val, ESQryType qryType) {
		QueryBuilder builder = null;
		if (!IUtils.isNullOrEmpty(key) &&
				!IUtils.isNullOrEmpty(val) &&
				key.indexOf(".") != -1) {
			String path = key.substring(0, key.lastIndexOf("."));
			builder = QueryBuilders.nestedQuery(
					path, getTermQueryBuilder(key, val, qryType), ScoreMode.None);
		}
		return builder;
	}

	/**
	 * Method to create a term query for must clause and
	 * common term query for should clause
	 * @param key
	 * @param val
	 * @param qryType
	 * @return
	 */
	static QueryBuilder getTermQueryBuilder(String key, String val, ESQryType qryType) {
		QueryBuilder qb = null;
		if (ESQryType.SHOULD != qryType) {
			logger.info(key + ": " + val);
			if (!IUtils.isNullOrEmpty(val) && val.contains(",")) {
				qb = QueryBuilders.termsQuery(key,
						IUtils.getArrayFromJsonString(val));
			} else {
				qb = QueryBuilders.matchQuery(key, val);
			}
		} else {
			qb = QueryBuilders.commonTermsQuery(key, val);
		}
		return qb;
	}

	/**
	 * Method to generate new event object form params
	 * @param cls
	 * @param eventType
	 * @param entity
	 * @return
	 * @throws Exception 
	 */
	static ESEvent createEvent(String cls, String eventType, String entity) throws Exception {
		ESEvent event = null;
		if (!IUtils.isNullOrEmpty(cls)) {
			Class<?> clazz = IUtils.getClass(cls);
			Object obj = IUtils.getObjectFromValue(entity, clazz);
			event = new ESEvent(EventType.valueOf(eventType), obj);
		}
		return event;
	}

	/**
	 * Method to iterate map object and extract keys including nested.
	 * @param mappings
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	static List<String> getFieldsFromMappings(Map map) {
		List<String> list = new ArrayList<>();;
		JSONObject json = new JSONObject(map);
		findFields(null, json, list);
		return list;
	}

	/**
	 * Method to find fields into json object.
	 * @param parent
	 * @param json
	 * @param list
	 */
	@SuppressWarnings("rawtypes")
	static void findFields(String parent, JSONObject json, List<String> list) {
		if (!IUtils.isNull(json) && json.length() > 0) {
			Iterator it = json.keys();
			while (it.hasNext()) {
				try {
					String key = it.next().toString();
					JSONObject val = json.getJSONObject(key);
					if ("properties".equals(key)) {
						addFields(parent, val, list);
					} else {
						findFields(parent, val, list);
						// ignore all others.
					}
				} catch (JSONException e) {
					// ignore it
				}
			}
		}
	}

	/**
	 * Method to add fields into list
	 * @param parent
	 * @param val
	 * @param list
	 */
	@SuppressWarnings("rawtypes")
	static void addFields(String parent, JSONObject val, List<String> list) {
		if (!IUtils.isNull(val)) {
			Iterator fields = val.keys();
			while (fields.hasNext()) {
				try {
					String field = fields.next().toString();
					JSONObject fldVal = val.getJSONObject(field);
					String key = (IUtils.isNullOrEmpty(parent) ?
							field : (parent + "." + field));
					list.add(key);
					if (!IUtils.isNull(fldVal) && fldVal.has("properties")) {
						addFields(key, fldVal.getJSONObject("properties"), list);
					}
				} catch (JSONException e) {
					// ignore it
				}
			}
		}
	}

	/**
	 * Method to make sure that query format is as expected of els 5.5+
	 * @param elsQuery
	 * @return
	 */
	static String getElsQuery(String elsQuery) {
		String res = elsQuery;
		JSONObject json = IUtils.getJSONObject(elsQuery);
		if (!IUtils.isNull(json)) {
			if (json.has(IConsts.PRM_QUERY)) {
				res = json.optString(IConsts.PRM_QUERY).toString();
				logger.info("Updated query: " + res);
			}
		}
		return res;
	}
}
