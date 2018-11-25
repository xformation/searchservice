/**
 * 
 */
package com.synectiks.search.queries;

import java.io.Serializable;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramAggregationBuilder ;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.elasticsearch.search.aggregations.bucket.range.RangeAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.IncludeExclude;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.max.MaxAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.min.MinAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.sum.SumAggregationBuilder;
import org.joda.time.DateTimeZone;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.synectiks.commons.constants.IConsts;
import com.synectiks.commons.utils.IUtils;

/**
 * @author Rajesh Upadhyay
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Aggregator implements Serializable {

	private static final long serialVersionUID = -3614131812579108112L;

	private String aggreType;
	private String fieldType;
	private String fieldName;
	private String interval;
	private String ranges;
	private String values;
	private String locale;
	private String format;

	public String getAggreType() {
		return aggreType;
	}

	public String getFieldName() {
		return fieldName;
	}

	public String getInterval() {
		return interval;
	}

	public String getLocale() {
		return locale;
	}

	public String getFormat() {
		return format;
	}

	public String getFieldType() {
		return fieldType;
	}

	public String getRanges() {
		return ranges;
	}

	public String getValues() {
		return values;
	}

	public void setValues(String values) {
		this.values = values;
	}

	public void setFieldType(String fieldType) {
		this.fieldType = fieldType;
	}

	public void setRanges(String ranges) {
		this.ranges = ranges;
	}

	public void setAggreType(String type) {
		this.aggreType = type;
	}

	public void setFieldName(String fieldName) {
		this.fieldName = fieldName;
	}

	public void setInterval(String interval) {
		this.interval = interval;
	}

	public void setLocale(String locale) {
		this.locale = locale;
	}

	public void setFormat(String format) {
		this.format = format;
	}

	public String getAggreKey() {
		if (!IUtils.isNullOrEmpty(aggreType) &&
				!IUtils.isNullOrEmpty(fieldName)) {
			// Create a combined key for aggregation query
			String key = aggreType + "_" + fieldName;
			if (!IUtils.isNullOrEmpty(interval)) {
				 key += "_" + interval;
			}
			return key;
		}
		return null;
	}

	/**
	 * Method generate {@code DateHistogramBuilder} object from parameters
	 * @return {@code DateHistogramBuilder} object or null
	 */
	public AbstractAggregationBuilder createAggregationBuilder() {
		AbstractAggregationBuilder aggBuilder = null;
		if (!IUtils.isNullOrEmpty(aggreType)) {
			switch (aggreType) {
			case "count":
				aggBuilder = getCountAggreBuilder();
				break;
			case "terms":
				aggBuilder = getTermsAggreBuilder();
				break;
			case "avg":
				aggBuilder = AggregationBuilders.avg(
						getAggreKey()).field(fieldName);
				break;
			case "ranges":
				aggBuilder = getRangesAggreBuilder();
				break;
			case "min":
				MinAggregationBuilder min = AggregationBuilders.min(getAggreKey());
				min.field(fieldName);
				if (!IUtils.isNullOrEmpty(format)) {
					min.format(format);
				}
				aggBuilder = min;
				break;
			case "max":
				MaxAggregationBuilder max = AggregationBuilders.max(getAggreKey());
				max.field(fieldName);
				if (!IUtils.isNullOrEmpty(format)) {
					max.format(format);
				}
				aggBuilder = max;
				break;
			case "sum":
				SumAggregationBuilder sum = AggregationBuilders.sum(getAggreKey());
				sum.field(fieldName);
				if (!IUtils.isNullOrEmpty(format)) {
					sum.format(format);
				}
				aggBuilder = sum;
				break;
			default:
				break;
			}
		}
		return aggBuilder;
	}

	/**
	 * Method to generate Ranges aggregation builder
	 * @return
	 */
	private AbstractAggregationBuilder getRangesAggreBuilder() {
		RangeAggregationBuilder builder = AggregationBuilders.range(getAggreKey());
		builder.field(fieldName);
		if (!IUtils.isNullOrEmpty(ranges)) {
			try {
				JSONArray jArr = new JSONArray(ranges);
				for (int i = 0; i < jArr.length(); i ++) {
					JSONObject obj = jArr.getJSONObject(i);
					double from = -1d, to = -1d;
					if (!IUtils.isNull(obj)) {
						if (obj.has(IConsts.FROM.toLowerCase())) {
							from = obj.getDouble(IConsts.FROM.toLowerCase());
						}
						if (obj.has(IConsts.TO.toLowerCase())) {
							to = obj.getDouble(IConsts.TO.toLowerCase());
						}
					}
					if (from != -1d && to != -1d) {
						builder.addRange(from, to);
					}
				}
			} catch (Throwable th) {
				th.printStackTrace();
			}
		}
		if (!IUtils.isNullOrEmpty(format)) {
			builder.format(format);
		}
		return builder;
	}

	/**
	 * Method to generate terms aggregation builder
	 * @return
	 */
	private AbstractAggregationBuilder getTermsAggreBuilder() {
		TermsAggregationBuilder builder = AggregationBuilders.terms(getAggreKey());
		builder.field(fieldName);
		builder.includeExclude(new IncludeExclude(
				IUtils.getArrayFromJsonString(values), null));
		builder.size(0);
		return builder;
	}

	/**
	 * Method to generate aggregation count builder
	 * @return
	 */
	private AbstractAggregationBuilder getCountAggreBuilder() {
		if (!IUtils.isNullOrEmpty(fieldType)
				&& fieldType.equalsIgnoreCase("date")) {
			DateHistogramAggregationBuilder builder = null;
			if (!IUtils.isNullOrEmpty(fieldName) &&
					!IUtils.isNullOrEmpty(interval)) {
				builder = AggregationBuilders.dateHistogram(getAggreKey());
				builder.field(fieldName);
				builder.timeZone(DateTimeZone.forID(locale));
				builder.dateHistogramInterval(new DateHistogramInterval(interval));
			}
			return builder;
		} else {
			// Create value count builder
			return AggregationBuilders.count(getAggreKey()).field(fieldName);
		}
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Aggregator [aggreType=");
		builder.append(aggreType);
		builder.append(", fieldType=");
		builder.append(fieldType);
		builder.append(", fieldName=");
		builder.append(fieldName);
		builder.append(", interval=");
		builder.append(interval);
		builder.append(", ranges=");
		builder.append(ranges);
		builder.append(", locale=");
		builder.append(locale);
		builder.append(", format=");
		builder.append(format);
		builder.append(", values=");
		builder.append(values);
		builder.append("]");
		return builder.toString();
	}

}
