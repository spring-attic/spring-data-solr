package org.springframework.data.solr.core.query.result;

import org.springframework.data.domain.Page;

/**
 * Specific type of {@link Page} holding max score information.
 * 
 * @author Francisco Spaeth
 *
 * @param <T>
 */
public interface ScoredPage<T> extends Page<T> {

	/**
	 * Returns the scoring of the topmost document (max score).
	 * 
	 * @return
	 */
	Float getMaxScore();
	
}
