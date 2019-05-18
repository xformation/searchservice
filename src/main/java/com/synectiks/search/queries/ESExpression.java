/**
 * 
 */
package com.synectiks.search.queries;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.query.SearchQuery;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.synectiks.commons.utils.IUtils;
import com.synectiks.search.utils.IESUtils;
import com.synectiks.search.utils.IESUtils.ESQryType;


/**
 * @author Rajesh
 */
public class ESExpression {

	/**
	 * Class to generate search query form params 
	 * @author Rajesh
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class FiltersQueryBuilder implements Serializable {

		private static final long serialVersionUID = -9037665749748247257L;

		private List<Filters> and;
		private List<Filters> or;
		private List<Filters> not;
		private List<Filters> filters;
		private String clazz;
		private int pageNo;
		private int pageSize;
		private Aggregator aggre;

		public List<Filters> getAnd() {
			return and;
		}

		public List<Filters> getOr() {
			return or;
		}

		public List<Filters> getNot() {
			return not;
		}

		public List<Filters> getFilters() {
			return filters;
		}

		public void setAnd(List<Filters> and) {
			this.and = and;
		}

		public void setOr(List<Filters> or) {
			this.or = or;
		}

		public void setNot(List<Filters> not) {
			this.not = not;
		}

		public void setFilters(List<Filters> filters) {
			this.filters = filters;
		}

		public String getClazz() {
			return clazz;
		}

		public int getPageNo() {
			return pageNo;
		}

		public int getPageSize() {
			return pageSize;
		}

		public void setClazz(String clazz) {
			this.clazz = clazz;
		}

		public void setPageNo(int pageNo) {
			this.pageNo = pageNo;
		}

		public void setPageSize(int pageSize) {
			this.pageSize = pageSize;
		}

		public FiltersQueryBuilder withAggregator(Aggregator aggre) {
			this.aggre = aggre;
			return this;
		}

		/**
		 * Method to build Elastic {@code SearchQuery} from input params
		 * @return
		 */
		public SearchQuery build() {
			PageRequest pageReq = IESUtils.getPageRequest(pageNo, pageSize);
			String indexName = null;
			if (IUtils.isNullOrEmpty(indexName ) && !IUtils.isNullOrEmpty(clazz)) {
				indexName = IESUtils.getIndexName(IUtils.getClass(clazz));
			}
			// Finally create a bool query builder with query type
			BoolQueryBuilder boolQB = QueryBuilders.boolQuery();
			IESUtils.processFilter(and, ESQryType.MATCH, boolQB);
			IESUtils.processFilter(or, ESQryType.SHOULD, boolQB);
			IESUtils.processFilter(not, ESQryType.NOT, boolQB);
			IESUtils.processFilter(filters, ESQryType.FILTER, boolQB);
			AbstractAggregationBuilder aggreBuilder = null;
			if (!IUtils.isNull(aggre)) {
				aggreBuilder = aggre.createAggregationBuilder();
			}
			return IESUtils.getNativeSearchQuery(boolQB, indexName, pageReq, aggreBuilder);
		}

		/**
		 * Create {@code FiltersQueryBuilder} from input params
		 * @param cls
		 * @param filters
		 * @param pageNo
		 * @param pageSize
		 * @return
		 */
		public static FiltersQueryBuilder create(
				String cls, String filters, int pageNo, int pageSize) {
			FiltersQueryBuilder builder = null;
			try {
				builder = IUtils.OBJECT_MAPPER.readValue(
						filters, FiltersQueryBuilder.class);
				builder.setClazz(cls);
				builder.setPageNo(pageNo);
				builder.setPageSize(pageSize);
			} catch (IOException e) {
				e.printStackTrace();
			}
			return builder;
		}
	}

	/**
	 * Class to generate a simple string search query
	 * @author Rajesh
	 */
	public static class StringQueryBuilder {

		private String query;
		private String fields;
		private String clazz;
		private String indexName;
		private int pageNo;
		private int pageSize;

		public StringQueryBuilder(String query) {
			this.query = query;
		}

		public StringQueryBuilder withFields(String fields) {
			this.fields = fields;
			return this;
		}

		public StringQueryBuilder withClass(String clazz) {
			this.clazz = clazz;
			return this;
		}

		public StringQueryBuilder withIndexName(String indexName) {
			this.indexName = indexName;
			return this;
		}

		public StringQueryBuilder withPageNo(int pageNo) {
			this.pageNo = pageNo;
			return this;
		}

		public StringQueryBuilder withPageSize(int pageSize) {
			this.pageSize = pageSize;
			return this;
		}

		/**
		 * Method builds a {@code SearchQery} instance for search
		 * @return
		 */
		public SearchQuery build() {
			PageRequest pageReq = IESUtils.getPageRequest(pageNo, pageSize);
			List<String> lst = IUtils.getListFromString(fields, null);
			// Create string query builder
			QueryStringQueryBuilder qStrBuilder = QueryBuilders.queryStringQuery(query)
					.analyzeWildcard(true);
			// Add fields into search query
			lst.forEach((item) -> {
				qStrBuilder.field(item);
			});
			if (IUtils.isNullOrEmpty(indexName) && !IUtils.isNullOrEmpty(clazz)) {
				indexName = IESUtils.getIndexName(IUtils.getClass(clazz));
			}
			// Finally create a bool query builder with query type
			BoolQueryBuilder qb = IESUtils.createBoolQuery(
					IESUtils.ESQryType.SHOULD, qStrBuilder);
			return IESUtils.getNativeSearchQuery(qb, indexName, pageReq, null);
		}
	}

	/**
	 * Class to hold all filters key values
	 * @author Rajesh
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Filters implements Serializable {

		private static final long serialVersionUID = 1723527749522551204L;

		private Map<String, String> filters;

		public Map<String, String> getFilters() {
			return filters;
		}

		public void setFilters(Map<String, String> filters) {
			this.filters = filters;
		}
	}

}
