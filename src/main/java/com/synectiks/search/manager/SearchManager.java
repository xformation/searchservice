/**
 * 
 */
package com.synectiks.search.manager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.elasticsearch.action.search.SearchResponse;
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
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.ResultsExtractor;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.stereotype.Component;

import com.synectiks.commons.constants.IConsts;
import com.synectiks.commons.utils.IUtils;
import com.synectiks.search.queries.Aggregator;
import com.synectiks.search.queries.ESExpression.FiltersQueryBuilder;
import com.synectiks.search.queries.ESExpression.StringQueryBuilder;
import com.synectiks.search.utils.IESUtils;

/**
 * @author Rajesh
 */
@Component
public class SearchManager {

	private static final Logger logger = LoggerFactory
			.getLogger(SearchManager.class);

	@Autowired
	private ElasticsearchTemplate searchTemplate;

	public ElasticsearchTemplate getESTemplate() {
		return searchTemplate;
	}

	/**
	 * Method to add an index with mappings
	 * or update the existing index mapping.
	 * @param cls Entity class
	 * @param mappings string mapping json
	 * @param isUpdate set true to update existing mapping.
	 * @return true only if mapping updated successfully;
	 */
	public boolean putMapping(String cls, String mappings, boolean isUpdate) {
		Class<?> clazz = IUtils.getClass(cls);
		boolean created = false;
		if (!searchTemplate.indexExists(clazz)) {
			created = searchTemplate.createIndex(clazz);
		}
		// add mapping only it newly created index or we have to update it.
		if (created || isUpdate) {
			return searchTemplate.putMapping(clazz, mappings);
		}
		return false;
	}

	/**
	 * Method to search for <b>q</b> string in the
	 *  <b>cls</b> class's <b>fields</b>
	 * @param q search string
	 * @param cls fully qualified name of entity class
	 * @param fields comma separated list of field name of entity class
	 * @param page
	 * @param size
	 * @return list of entity objects which match query criteria
	 */
	public List<?> search(String q,
			String cls, String fields, int page, int size) {
		SearchQuery sQry = new StringQueryBuilder(q)
				.withClass(cls)
				.withFields(fields)
				.withPageNo(page)
				.withPageSize(size)
				.build();
		// Search the query string
		List<?> lst = null;
		if (!IUtils.isNull(cls)) {
			lst = executeQuery(sQry, IUtils.getClass(cls), page, size);
		} else {
			lst = searchTemplate.query(sQry, new SearchResultExtractor());
		}
		return lst;
	}

	/**
	 * Method to execute elastic search query string in json format.
	 * @param elsQuery
	 * @param pageNo
	 * @param pageSize
	 * @return
	 */
	public SearchResponse elsSearch(String elsQuery, int pageNo,
			int pageSize) {
		PageRequest pageReq = IESUtils.getPageRequest(pageNo, pageSize);
		// Finally create a bool query builder with query type
		WrapperQueryBuilder wqb = QueryBuilders.wrapperQuery(elsQuery);
		NativeSearchQuery nsqb = new NativeSearchQueryBuilder().withQuery(wqb)
				.withPageable(pageReq).build();
		return searchTemplate.query(nsqb, new ResultsExtractor<SearchResponse>() {
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
		long lst = searchTemplate.count(sQry, IUtils.getClass(cls));
		return lst;
	}

	/**
	 * Method to get the count of entities match the specified query filters
	 * @param json filters
	 * @param cls Entity class
	 * @param aggre Aggregator object
	 * @return count of Entity class instances.
	 */
	public Map<String, Object> aggreCounts(
			String json, String cls, Aggregator aggre) {

		logger.info("Cls: " + cls + ", filterJson: " + json + ", aggre: " + aggre);
		SearchQuery sQry = FiltersQueryBuilder.create(cls, json, 0, 0)
				.withAggregator(aggre).build();
		// Search the query string with aggregation result extractor
		Map<String, Object> lst = searchTemplate.query(sQry,
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
	private List<?> executeQuery(
			SearchQuery sQry, Class<?> cls, int page, int size) {
		if (!IUtils.isNull(cls)) {
			if (IESUtils.isScrollQuery(page, size)) {
				return getScrollResults(sQry, cls, page, size);
			} else {
				return searchTemplate.queryForList(sQry, cls);
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
	private List<?> getScrollResults(
			SearchQuery sQry, Class<?> cls, int page, int size) {

		List<Object> lst = null;
		if (!IUtils.isNull(cls) && !IUtils.isNull(sQry)) {
			List<Object> pages = new ArrayList<>();
			String scrollId = searchTemplate.scan(
					sQry, IConsts.ES_SCROLL_TIMEOUT, false);
			while (true) {
				Page<?> pg = searchTemplate.scroll(
						scrollId, IConsts.ES_SCROLL_TIMEOUT, cls);
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
	private static class SearchResultExtractor implements
			ResultsExtractor<List<String>> {

		@Override
		public List<String> extract(SearchResponse response) {
			List<String> results = new ArrayList<>();
			for (SearchHit hit : response.getHits()) {
				if (hit != null) {
					if (!IUtils.isNullOrEmpty(hit.sourceAsString())) {
						results.add(hit.sourceAsString());
					}
				}
			}
			return results;
		}
	}

	/**
	 * Elastic search response parser class to create
	 * a list from aggregation results.
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
						@SuppressWarnings("unchecked")
						InternalHistogram<Bucket> dtHist = (InternalHistogram<Bucket>) aggre;
						List<Bucket> buckets = dtHist.getBuckets();
						for (Bucket b : buckets) {
							String key = b.getKeyAsString();
							if (!IUtils.isNullOrEmpty(aggregator.getFormat())) {
								key = IUtils
										.getFormatedDateFromLongString(
												key, aggregator.getFormat(), aggregator.getLocale());
							}
							results.put(key, b.getDocCount());
						}
					} else if (aggre instanceof SingleValue) {
						results.put(aggregator.getAggreKey(), ((SingleValue) aggre).getValueAsString());
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
					logger.info("No aggregation found for key: " + aggregator.getAggreKey());
				}
			} else {
				logger.info("No aggregations found in response");
			}
			logger.info("Res: " + results);
			return results;
		}
	}

	/**
	 * Method to find and return mappings for entity.
	 * @param cls
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public Map gettMapping(String cls) {
		Class<?> clazz = IUtils.getClass(cls);
		return searchTemplate.getMapping(clazz);
	}
}
