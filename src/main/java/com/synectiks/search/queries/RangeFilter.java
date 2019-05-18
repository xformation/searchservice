/**
 * 
 */
package com.synectiks.search.queries;

import java.io.Serializable;
import java.util.Date;

import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.springframework.util.NumberUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.synectiks.commons.utils.IUtils;

/**
 * @author Rajesh Upadhyay
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RangeFilter implements Serializable {

	private static final long serialVersionUID = 2648057086337881334L;

	private String type;
	private String fieldName;
	private String from;
	private String to;
	private String locale;
	private String format;

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getFieldName() {
		return fieldName;
	}

	public void setFieldName(String fieldName) {
		this.fieldName = fieldName;
	}

	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;
	}

	public String getTo() {
		return to;
	}

	public void setTo(String to) {
		this.to = to;
	}

	public String getLocale() {
		return locale;
	}

	public void setLocale(String locale) {
		this.locale = locale;
	}

	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
	}

	/**
	 * Method to create a range query builder
	 * @return {@code RangeQueryBuilder} or null
	 */
	public RangeQueryBuilder createQueryBuilder() {
		RangeQueryBuilder builder = null;
		if (!IUtils.isNullOrEmpty(getFieldName())) {
			builder = QueryBuilders.rangeQuery(getFieldName());
			// set values based on type
			switch (type) {
			case "Number":
				builder.from(NumberUtils.parseNumber(getFrom(), Double.class));
				builder.to(NumberUtils.parseNumber(getTo(), Double.class));
				break;
			case "Date":
				Date fromDate = IUtils.parseDate(getFrom(), getLocale(), getFormat());
				Date toDate = IUtils.parseDate(getTo(), getLocale(), getFormat());
				if (!IUtils.isNull(toDate) && !IUtils.isNull(fromDate)) {
					builder.from(fromDate.getTime());
					builder.to(toDate.getTime());
				} else if (!IUtils.isNull(toDate)) {
					builder.lte(toDate.getTime());
				} else if (!IUtils.isNull(fromDate)) {
					builder.gte(fromDate.getTime());
				}
				break;
			default:
				builder.from(getFrom());
				break;
			}
		}
		return builder;
	}

}
