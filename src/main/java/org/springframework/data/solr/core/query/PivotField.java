package org.springframework.data.solr.core.query;

import java.util.List;

/**
 * Defines a field that could be used within a pivot facet query.
 * 
 * @author Francisco Spaeth
 *
 */
public interface PivotField extends Field {

	/**
	 * Get the fields for this pivot.
	 * 
	 * @return
	 */
	List<Field> getFields();
	
}
