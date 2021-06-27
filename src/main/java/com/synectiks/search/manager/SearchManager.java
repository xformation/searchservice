/**
 * 
 */
package com.synectiks.search.manager;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.action.admin.indices.get.GetIndexResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.IdsQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.WrapperQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.histogram.InternalHistogram;
import org.elasticsearch.search.aggregations.bucket.histogram.InternalHistogram.Bucket;
import org.elasticsearch.search.aggregations.bucket.range.Range;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.InternalNumericMetricsAggregation.SingleValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.ResultsExtractor;
import org.springframework.data.elasticsearch.core.ScrolledPage;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.stereotype.Component;

import com.synectiks.commons.constants.IConsts;
import com.synectiks.commons.interfaces.IESEntity;
import com.synectiks.commons.utils.IUtils;
import com.synectiks.search.queries.Aggregator;
import com.synectiks.search.queries.ESExpression.FiltersQueryBuilder;
import com.synectiks.search.queries.ESExpression.StringQueryBuilder;
import com.synectiks.search.utils.ClassFinder;
import com.synectiks.search.utils.IESUtils;

/**
 * @author Rajesh
 */
@Component
public class SearchManager {

	private static final Logger logger = LoggerFactory.getLogger(SearchManager.class);
	private static final String ENTITY_PKG = "com.synectiks.cms.entities";
	private static final int SCROLL_SIZE = 5000;

	@Autowired
	private ElasticsearchTemplate esTemplate;

	public ElasticsearchTemplate getESTemplate() {
		return esTemplate;
	}

	/**
	 * Method to find and return mappings for entity.
	 * @param cls
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public Map gettMapping(String cls) {
		Class<?> clazz = IUtils.getClass(cls);
		if (esTemplate.indexExists(clazz)) {
			logger.info("Elastic Index exists");
			return esTemplate.getMapping(clazz);
		} else {
			logger.info("Creating model mapping");
			return getAllFieldsMapping(clazz);
		}
	}

	/**
	 * Method to get all class fields list from a class including super class
	 * fields.
	 * @param clazz
	 * @return
	 */
	private Map<String, Object> getAllFieldsMapping(Class<?> clazz) {
		List<Field> list = getAllFields(clazz);
		Map<String, Object> pMap = new HashMap<>();
		List<Map<String, Object>> props = new ArrayList<>();
		for (Field fld : list) {
			props.add(addField(fld.getName(), fld.getType()));
		}
		pMap.put("properties", props);
		return pMap;
	}

	/**
	 * Method to get list of fields from class using reflection.
	 * @param clazz
	 * @return
	 */
	private List<Field> getAllFields(Class<?> clazz) {
		if (clazz == null) {
			return Collections.emptyList();
		}
		List<Field> result = new ArrayList<>(getAllFields(clazz.getSuperclass()));
		List<Field> filteredFields = Arrays.stream(clazz.getDeclaredFields())
				.filter(f -> (Modifier.isPublic(f.getModifiers())
						|| Modifier.isProtected(f.getModifiers())
						|| Modifier.isPrivate(f.getModifiers()))
						&& !Modifier.isStatic(f.getModifiers()))
				.collect(Collectors.toList());
		result.addAll(filteredFields);
		return result;
	}

	/**
	 * Method to add a field details into string.
	 * @param name
	 * @param clz
	 * @return
	 */
	private Map<String, Object> addField(String name, Class<?> clz) {
		Map<String, Object> map = new HashMap<>();
		Map<String, Object> fmap = new HashMap<>();
		String tp = clz.getSimpleName();
		fmap.put("type", tp);
		if (Date.class.getSimpleName().equals(tp)) {
			fmap.put("ignore_malformed", true);
			fmap.put("format", IConsts.DEF_DATE_FORMAT);
		}
		map.put(name, fmap);
		return map;
	}

	/**
	 * Method to get list of indexes either from entities or from elastic
	 * @param fromElastic
	 * @param pkg
	 * @param json
	 * @return
	 */
	public List<String> listIndicies(boolean fromElastic, String pkg, boolean json) {
		List<String> lst = null;
		if (fromElastic) {
			try {
				GetIndexResponse indexes = esTemplate.getClient().admin().indices()
						.getIndex(new GetIndexRequest()).get();
				// logger.info("Aliases: " + indexes.aliases());
				return Arrays.asList(indexes.getIndices());
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			if (IUtils.isNullOrEmpty(pkg)) {
				pkg = ENTITY_PKG;
			}
			List<Class<?>> classes = ClassFinder.find(pkg, IESEntity.class);
			lst = new ArrayList<>();
			for (Class<?> cls : classes) {
				StringBuilder sb = new StringBuilder();
				if (json) {
					sb.append("{ \"" + cls.getSimpleName() + "\": {");
					if (cls.isAnnotationPresent(Document.class)) {
						Document doc = cls.getAnnotation(Document.class);
						sb.append("\"cls\": \"" + cls.getName() + "\",");
						sb.append("\"indexName\": \"" + doc.indexName() + "\",");
						sb.append("\"indexType\": \"" + doc.type() + "\"");
					}
					sb.append("} }");
				} else {
					sb.append(cls.getName());
				}
				lst.add(sb.toString());
			}
		}
		return lst;
	}

	/**
	 * Method to return documents by elastic-search document ids.
	 * @param cls
	 * @param ids
	 * @return
	 */
	public List<String> getDocsById(String cls, List<String> ids) {
		String type = esTemplate.getPersistentEntityFor(IUtils.getClass(cls))
				.getIndexType();
		IdsQueryBuilder sQry = QueryBuilders.idsQuery(type)
				.addIds(IUtils.isNull(ids) ? null : ids.toArray(new String[ids.size()]));
		NativeSearchQuery nsqb = new NativeSearchQueryBuilder()
				.withQuery(sQry).withIndices(esTemplate
						.getPersistentEntityFor(IUtils.getClass(cls)).getIndexName())
				.build();
		List<String> res = esTemplate.query(nsqb, new SearchResultExtractor());
		return res;
	}

	/**
	 * Method to add an index with mappings or update the existing index
	 * mapping.
	 * @param cls Entity class
	 * @param mappings string mapping json
	 * @param isUpdate set true to update existing mapping.
	 * @return true only if mapping updated successfully;
	 */
	public boolean putMapping(String cls, String mappings, boolean isUpdate) {
		boolean created = false;
		Class<?> clazz = IUtils.getClass(cls);
		if (!esTemplate.indexExists(clazz)) {
			created = esTemplate.createIndex(clazz);
		}
		// add mapping only it newly created index or we have to update it.
		if (created || isUpdate) {
			return esTemplate.putMapping(clazz, mappings);
		}
		return false;
	}

	/**
	 * Method to create an index with the given index name. It creates a new
	 * index if index does not exists.
	 * @param indexName
	 * @return true only if index created successfully
	 */
	public boolean createIndex(String indexName) {
		boolean created = false;
		if (!esTemplate.indexExists(indexName)) {
			created = esTemplate.createIndex(indexName);
		}
		return created;
	}

	/**
	 * Method to search for <b>q</b> string in the <b>cls</b> class's
	 * <b>fields</b>
	 * @param q search string
	 * @param cls fully qualified name of entity class
	 * @param fields comma separated list of field name of entity class
	 * @param page
	 * @param size
	 * @return list of entity objects which match query criteria
	 */
	public List<?> search(String q, String cls, String fields, int page, int size) {
		SearchQuery sQry = new StringQueryBuilder(q).withClass(cls).withFields(fields)
				.withPageNo(page).withPageSize(size).build();
		// Search the query string
		List<?> lst = null;
		if (!IUtils.isNull(cls)) {
			lst = executeQuery(sQry, IUtils.getClass(cls), page, size);
		} else {
			lst = esTemplate.query(sQry, new SearchResultExtractor());
		}
		return lst;
	}

	/**
	 * Method to execute elastic search query string in json format.
	 * @param elsQuery
	 * @param cls
	 * @param pageNo
	 * @param pageSize
	 * @return
	 */
	public SearchResponse elsSearch(String elsQuery, String cls, int pageNo,
			int pageSize) {
		// return esTemplate.getClient().prepareSearch(
		// esTemplate.getPersistentEntityFor(IUtils.getClass(cls)).getIndexName())
		// .setQuery(QueryBuilders.wrapperQuery(elsQuery))
		// .setSize(pageSize)
		// .setFrom(pageNo * pageSize)
		// .execute()
		// .actionGet();

		// Following code run only elastic below 5.0
		PageRequest pageReq = IESUtils.getPageRequest(pageNo, pageSize);
		// Finally create a bool query builder with query type
		elsQuery = IESUtils.getElsQuery(elsQuery);
		WrapperQueryBuilder wqb = QueryBuilders.wrapperQuery(elsQuery);
		NativeSearchQuery nsqb = new NativeSearchQueryBuilder()
				// .withTypes(cls)
				.withIndices(esTemplate.getPersistentEntityFor(IUtils.getClass(cls))
						.getIndexName())
				.withQuery(wqb).withPageable(pageReq).build();
		return esTemplate.query(nsqb, new ResultsExtractor<SearchResponse>() {
			@Override
			public SearchResponse extract(SearchResponse response) {
				return response;
			}
		});
	}

	/**
	 * Method to get the list of entities match the specified query filters
	 * @param json filters
	 * @param cls Entity class
	 * @param page
	 * @param size
	 * @return List of Entity class instances.
	 */
	public List<?> search(String json, String cls, int page, int size) {

		logger.info("Cls: " + cls + ", json: " + json);
		SearchQuery sQry = FiltersQueryBuilder.create(cls, json, page, size).build();
		// Search the query string
		List<?> lst = executeQuery(sQry, IUtils.getClass(cls), page, size);
		return lst;
	}

	/**
	 * Method to get the count of entities match the specified query filters
	 * @param json string filters
	 * @param cls Entity class
	 * @return count of Entity class instances.
	 */
	public long count(String json, String cls) {

		logger.info("Cls: " + cls + ", json: " + json);
		SearchQuery sQry = FiltersQueryBuilder.create(cls, json, 0, 0).build();
		// Search the query string
		long lst = esTemplate.count(sQry, IUtils.getClass(cls));
		return lst;
	}

	/**
	 * Method to get the count of entities match the specified query filters
	 * @param json filters
	 * @param cls Entity class
	 * @param aggre Aggregator object
	 * @return count of Entity class instances.
	 */
	public Map<String, Object> aggreCounts(String json, String cls, Aggregator aggre) {

		logger.info("Cls: " + cls + ", filterJson: " + json + ", aggre: " + aggre);
		SearchQuery sQry = FiltersQueryBuilder.create(cls, json, 0, 0)
				.withAggregator(aggre).build();
		// Search the query string with aggregation result extractor
		Map<String, Object> lst = esTemplate.query(sQry,
				new AggregationResultExtractor(aggre));
		return lst;
	}

	/**
	 * Method to handle search for deep paging request
	 * @param sQry
	 * @param cls
	 * @param page
	 * @param size
	 * @return
	 */
	private List<?> executeQuery(SearchQuery sQry, Class<?> cls, int page, int size) {
		logger.info("Query: " + sQry.getQuery());
		logger.info("Filters: " + sQry.getFilter());
		if (!IUtils.isNull(cls)) {
			if (IESUtils.isScrollQuery(page, size)) {
				return getScrollResults(sQry, cls, page, size);
			} else {
				return esTemplate.queryForList(sQry, cls);
			}
		}
		return null;
	}

	/**
	 * Method to collect scroll results
	 * @param sQry
	 * @param cls
	 * @param page
	 * @param size
	 * @return
	 */
	private List<?> getScrollResults(SearchQuery sQry, Class<?> cls, int page, int size) {

		List<Object> lst = null;
		if (!IUtils.isNull(cls) && !IUtils.isNull(sQry)) {
			List<Object> pages = new ArrayList<>();
			Page<?> scroll = esTemplate.startScroll(IConsts.ES_SCROLL_TIMEOUT, sQry, cls);
			String scrollId = ((ScrolledPage<?>) scroll).getScrollId();
			while (true) {
				Page<?> pg = esTemplate.continueScroll(scrollId,
						IConsts.ES_SCROLL_TIMEOUT, cls);
				if (pg.hasContent()) {
					pages.addAll(pg.getContent());
				} else {
					break;
				}
			}
			// Check if user required some specific page
			if (page > 0) {
				int to = page * size;
				int from = (to - size) + 1;
				lst = new ArrayList<>(size);
				if (pages.size() > from) {
					// Set the required page contents in result list
					for (int cnt = from; cnt < to; cnt++) {
						lst.add(pages.get(cnt));
					}
				}
			} else {
				// return whole results list
				lst = pages;
			}
		}
		return lst;
	}

	/**
	 * Elastic search response parser class to create a list of json objects
	 * @author Rajesh Upadhyay
	 */
	private static class SearchResultExtractor implements ResultsExtractor<List<String>> {

		@Override
		public List<String> extract(SearchResponse response) {
			List<String> results = new ArrayList<>();
			for (SearchHit hit : response.getHits()) {
				if (hit != null) {
					if (!IUtils.isNullOrEmpty(hit.getSourceAsString())) {
						JSONObject json = new JSONObject(hit.getSourceAsMap());
						results.add(json.toString());
					}
				}
			}
			return results;
		}
	}

	/**
	 * Elastic search response parser class to create a list from aggregation
	 * results.
	 * @author Rajesh Upadhyay
	 */
	private static class AggregationResultExtractor
			implements ResultsExtractor<Map<String, Object>> {

		private Aggregator aggregator;

		public AggregationResultExtractor(Aggregator aggre) {
			this.aggregator = aggre;
		}

		@Override
		public Map<String, Object> extract(SearchResponse response) {
			Map<String, Object> results = null;
			Aggregations aggres = response.getAggregations();
			if (!IUtils.isNull(aggres)) {
				logger.info("Aggres: " + aggres);
				for (String key : aggres.asMap().keySet()) {
					logger.info("Key: " + key);
					Aggregation aggre = aggres.asMap().get(key);
					logger.info("cls: " + aggre.getClass().getName());
				}
				// Here get the histogram buckets and add in results
				if (!IUtils.isNull(aggres.get(aggregator.getAggreKey()))) {
					Aggregation aggre = aggres.get(aggregator.getAggreKey());
					results = new HashMap<>();
					if (aggre instanceof InternalHistogram) {
						InternalHistogram dtHist = (InternalHistogram) aggre;
						List<Bucket> buckets = dtHist.getBuckets();
						for (Bucket b : buckets) {
							String key = b.getKeyAsString();
							if (!IUtils.isNullOrEmpty(aggregator.getFormat())) {
								key = IUtils.getFormatedDateFromLongString(key,
										aggregator.getFormat(), aggregator.getLocale());
							}
							results.put(key, b.getDocCount());
						}
					} else if (aggre instanceof SingleValue) {
						results.put(aggregator.getAggreKey(),
								((SingleValue) aggre).getValueAsString());
					} else if (aggre instanceof Terms) {
						Terms terms = (Terms) aggre;
						for (Terms.Bucket entry : terms.getBuckets()) {
							results.put(entry.getKeyAsString(), entry.getDocCount());
						}
					} else if (aggre instanceof Range) {
						Range range = (Range) aggre;
						for (Range.Bucket entry : range.getBuckets()) {
							results.put(entry.getKeyAsString(), entry.getDocCount());
						}
					} else {
						logger.error("Unknown grouping result type class");
						results.put(aggregator.getAggreKey(), aggre);
					}
				} else {
					logger.info(
							"No aggregation found for key: " + aggregator.getAggreKey());
				}
			} else {
				logger.info("No aggregations found in response");
			}
			logger.info("Res: " + results);
			return results;
		}
	}

	/**
	 * Method to update entities by query
	 * @param type
	 * @param index
	 * @param searchKey
	 * @param searchValue
	 * @param updateKey
	 * @param updateValue
	 * @return
	 */
	public List<?> updateWithQuery(String type, String index, String searchKey,
			String searchValue, String updateKey, String updateValue) {

		int scrollSize = 5000;
		int i = 0;
		Long totalRec = getTotalRecords(type, index);
		if (totalRec > scrollSize) {
			scrollSize = totalRec.intValue() + scrollSize;
		}
		SearchResponse response = esTemplate.getClient().prepareSearch(index)
				.setTypes(type).setQuery(QueryBuilders.matchAllQuery())
				.setSize(scrollSize).setFrom(i * scrollSize).execute().actionGet();
		String docId = null;
		JSONObject jsonObj = null;
		for (SearchHit hit : response.getHits()) {
			try {
				jsonObj = new JSONObject(hit.getSourceAsString());
				if (jsonObj.getString(searchKey).equalsIgnoreCase(searchValue)) {
					docId = hit.getId();
					break;
				}
			} catch (JSONException e) {
				logger.error("Exception : ", e);
			}
			logger.info("Document id: " + hit.getId());
			logger.info("Source Document : " + hit.getSourceAsString());
		}

		if (!StringUtils.isBlank(docId)) {
			try {
				jsonObj.put(updateKey, updateValue);
			} catch (JSONException e) {
			}
			UpdateResponse updateResponse = esTemplate.getClient()
					.prepareUpdate(index, type, docId)
					.setDoc(jsonObj.toString(), XContentType.JSON)
					.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE)
					.setDocAsUpsert(true).get();
			logger.info("Record updated in elastic : Response " + updateResponse);
		}

		response = esTemplate.getClient().prepareSearch(index).setTypes(type)
				.setQuery(QueryBuilders.matchAllQuery()).setSize(scrollSize)
				.setFrom(i * scrollSize).execute().actionGet();

		List<?> results = new SearchResultExtractor().extract(response);
		return results;
	}

	/**
	 * Method to delete document by query
	 * @param type
	 * @param index
	 * @param searchKey
	 * @param searchValue
	 * @return
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	public List<?> deleteWithQuery(String type, String index, String searchKey,
			String searchValue) throws InterruptedException, ExecutionException {
		Long totalRec = getTotalRecords(type, index);
		if (!IUtils.isNull(totalRec) && totalRec.intValue() > SCROLL_SIZE) {
			totalRec = totalRec.longValue() + SCROLL_SIZE;
		}
		SearchResponse response = esTemplate.getClient().prepareSearch(index)
				.setTypes(type).setQuery(QueryBuilders.matchAllQuery())
				.setSize(totalRec.intValue()).setFrom(0).execute().actionGet();
		String docId = null;
		JSONObject jsonObj = null;
		for (SearchHit hit : response.getHits()) {
			try {
				jsonObj = new JSONObject(hit.getSourceAsString());
				if (jsonObj.getString(searchKey).equalsIgnoreCase(searchValue)) {
					docId = hit.getId();
					break;
				}
			} catch (JSONException e) {
				logger.error("Exception : ", e);
			}
			logger.info("Document id: " + hit.getId());
			logger.info("Source Document : " + hit.getSourceAsString());
		}

		esTemplate.getClient()
				.prepareDelete(index, type, docId)
				.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE).execute().get();

		response = esTemplate.getClient().prepareSearch(index).setTypes(type)
				.setQuery(QueryBuilders.matchAllQuery()).setSize(totalRec.intValue())
				.setFrom(0)
				.execute().actionGet();

		List<?> results = new SearchResultExtractor().extract(response);
		return results;
	}

	/**
	 * Method to count all documents in an index.
	 * @param type
	 * @param index
	 * @return
	 */
	public Long getTotalRecords(String type, String index) {
		if (!isIndexExists(index)) {
			return 0L;
		}
		SearchResponse response = esTemplate.getClient().prepareSearch(index)
				.setTypes(type).setQuery(QueryBuilders.matchAllQuery())
				.setSize(0).execute().actionGet();
		if (!IUtils.isNull(response) && !IUtils.isNull(response.getHits())) {
			logger.debug("Total records : " + response.getHits().getTotalHits());
			return response.getHits().getTotalHits();
		} else {
			return 0L;
		}

	}

	/**
	 * Method to search first match document by query
	 * @param type
	 * @param index
	 * @param searchKey
	 * @param searchValue
	 * @return
	 */
	public JSONObject searchWithQuery(String type, String index, String searchKey,
			String searchValue) {
		if (!isIndexExists(index)) {
			logger.warn("Index : " + index + ", not exists. Returning null object");
			return null;
		}
		Long totalRec = getTotalRecords(type, index);
		if (!IUtils.isNull(totalRec) && totalRec.intValue() > SCROLL_SIZE) {
			totalRec = totalRec.longValue() + SCROLL_SIZE;
		}
		SearchResponse response = esTemplate.getClient().prepareSearch(index)
				.setTypes(type).setQuery(QueryBuilders.matchAllQuery())
				.setSize(totalRec.intValue()).setFrom(0).execute().actionGet();
		JSONObject jsonObj = null;
		for (SearchHit hit : response.getHits()) {
			try {
				jsonObj = new JSONObject(hit.getSourceAsString());
				if (jsonObj.getString(searchKey).equalsIgnoreCase(searchValue)) {
					break;
				}
			} catch (JSONException e) {
				logger.error("Exception : ", e);
			}
		}
		logger.info("Searched document : " + jsonObj);
		return jsonObj;
	}

	/**
	 * Method to list all documents by class
	 * @param cls
	 * @return
	 */
	public List<?> searchWithClass(String cls) {
		Class<?> clazz = IUtils.getClass(cls);
		if (esTemplate.indexExists(clazz)) {
			String index = esTemplate.getPersistentEntityFor(clazz).getIndexName();
			String type = esTemplate.getPersistentEntityFor(clazz).getIndexType();
			return searchWithIndexAndType(type, index);
		}
		return null;
	}

	/**
	 * Method to list all documents by index and type
	 * @param type
	 * @param index
	 * @return
	 */
	public List<?> searchWithIndexAndType(String type, String index) {
		if (!isIndexExists(index)) {
			logger.warn("Index : " + index + ", not exists. Returning empty list");
			return Collections.emptyList();
		}
		Long totalRec = getTotalRecords(type, index);
		if (!IUtils.isNull(totalRec) && totalRec.intValue() > SCROLL_SIZE) {
			totalRec = totalRec.longValue() + SCROLL_SIZE;
		}
		SearchResponse response = esTemplate.getClient().prepareSearch(index)
				.setTypes(type).setQuery(QueryBuilders.matchAllQuery())
				.setSize(totalRec.intValue()).setFrom(0).execute().actionGet();

		List<?> results = new SearchResultExtractor().extract(response);
		logger.info("Searched document : " + results.size());
		return results;
	}

	/**
	 * Method to check if a index exists.
	 * @param index
	 * @return
	 */
	private boolean isIndexExists(String index) {
		boolean isIndexExists = false;
		try {
			GetIndexResponse indexes = esTemplate.getClient().admin().indices()
					.getIndex(new GetIndexRequest()).get();

			for (String s : indexes.getIndices()) {
				if (s.equals(index)) {
					isIndexExists = true;
					break;
				}
			}
		} catch (Exception e) {
			logger.error("Exception in checking index exists : " + e.getMessage());
		}
		return isIndexExists;
	}

	/**
	 * Method to create index if not exists and then save the docs into it.
	 * @param indx
	 * @param docs
	 */
	public List<String> saveDocs(String indx, List<String> docs) {
		List<String> res = null;
		if (!IUtils.isNullOrEmpty(indx)) {
			if (!isIndexExists(indx)) {
				this.createIndex(indx);
				logger.info("New index '" + indx + "' created.");
			}
			if (!IUtils.isNull(docs) && docs.size() > 0) {
				res = new ArrayList<>();
				for (String doc : docs) {
//					IndexQueryBuilder builder = new IndexQueryBuilder();
//					builder.withIndexName(indx).withObject(IUtils.getJSONObject(doc));
//					res.add(esTemplate.index(builder.build()));
					IndexRequest req = new IndexRequest(indx, indx);
					req.source(IUtils.getMapFromJson(IUtils.getJSONObject(doc)));
					String docId = esTemplate.getClient().index(req).actionGet().getId();
					res.add(docId);
				}
				logger.info(docs.size() + " docs saved into index: " + indx);
			}
		}
		return res;
	}
}
