/**
 * 
 */
package com.synectiks.search.receiver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.stereotype.Component;

import com.synectiks.commons.entities.search.ESEvent;
import com.synectiks.commons.exceptions.SynectiksException;
import com.synectiks.commons.interfaces.IESEntity;
import com.synectiks.commons.receiver.ESEventReceiver;
import com.synectiks.commons.utils.IUtils;
import com.synectiks.search.utils.IESUtils;

/**
 * @author Rajesh
 */
@Component
public class SearchESEventReceiver extends ESEventReceiver {

	private static final Logger logger = LoggerFactory.getLogger(SearchESEventReceiver.class);

	@Autowired
	private ElasticsearchTemplate searchTemplate;

	public SearchESEventReceiver() {
		super();
		// Let handle events here instead of super class bean.
		handleEvents = false;
	}

	/**
	 * Method to handle {@code LmsEvent} from api call
	 * @param event {@code LmsEvent} instance
	 * @return
	 * @throws SynectiksException 
	 */
	@Override
	public String handleEvent(ESEvent event) throws SynectiksException {
		String res = null;
		if (!IUtils.isNull(event)) {
			switch(event.getEventType()) {
			case DELETE:
				res = deleteIndex(event);
				break;
			case CREATE:
			case UPDATE:
				res = createIndex(event);
				break;
			default:
				logger.error("Unknown Event type: " + event.getEventType());
			}
		}
		return res;
	}

	/**
	 * Method to index an entity sent by fired event.
	 * @param event
	 * @return 
	 * @throws SynectiksException
	 */
	public String createIndex(ESEvent event) throws SynectiksException {
		String res = null;
		logger.info("Handling create index");
		if (!IUtils.isNull(event) && !IUtils.isNull(event.getEntity())) {
			IndexQueryBuilder indxQryBuilder = new IndexQueryBuilder();
			IESEntity target = (IESEntity) event.getEntity();
			if (!IUtils.isNull(IESUtils.getESID(target))) {
				indxQryBuilder.withId(IESUtils.getESID(target));
			}
			if (!IUtils.isNull(IESUtils.getIndexName(target))) {
				indxQryBuilder.withIndexName(IESUtils.getIndexName(target));
			}
			if (!IUtils.isNull(IESUtils.getIndexType(target))) {
				indxQryBuilder.withId(IESUtils.getIndexType(target));
			}
			// finally set object to index
			indxQryBuilder.withObject(target);
			logger.info("index: " + IESUtils.getIndexName(target));
			res = searchTemplate.index(indxQryBuilder.build());
		}
		return res;
	}

	/**
	 * Method to delete an index from elastic search indexes
	 * @param event
	 * @throws SynectiksException 
	 */
	public String deleteIndex(ESEvent event) throws SynectiksException {
		String res = null;
		if (!IUtils.isNull(event) && !IUtils.isNull(event.getEntity())) {
			logger.info("Handling delete index: " + event.getEntity());
			String id = IESUtils.getESID(event.getEntity());
			String indxName = IESUtils.getIndexName(event.getEntity());
			String indxType = IESUtils.getIndexType(event.getEntity());
			logger.info("Delete: " + indxName + ", " + indxType + ", " + id);
			res = searchTemplate.delete(indxName, indxType, id);
		}
		return res;
	}
}
