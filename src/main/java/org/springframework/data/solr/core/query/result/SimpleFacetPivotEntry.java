package org.springframework.data.solr.core.query.result;

import java.util.List;

import org.springframework.data.solr.core.query.Field;

/**
 * The most trivial implementation of {@link FacetPivotFieldEntry}.
 * 
 * @author Francisco Spaeth
 * 
 */
public class SimpleFacetPivotEntry extends AbstractFacetEntry implements FacetPivotFieldEntry {

	private Field field;
	private List<FacetPivotFieldEntry> pivot;

	public SimpleFacetPivotEntry(Field field, String value, long count) {
		super(value, count);
		this.field = field;
	}

	@Override
	public Field getKey() {
		return field;
	}

	@Override
	public Field getField() {
		return field;
	}

	@Override
	public List<FacetPivotFieldEntry> getPivot() {
		return pivot;
	}

	public void setPivot(List<FacetPivotFieldEntry> pivot) {
		this.pivot = pivot;
	}

	@Override
	public String toString() {
		return "SimpleFacetPivotEntry [getField()=" + getField() + ", getValueCount()=" + getValueCount()
				+ ", getValue()=" + getValue() + ", getPivot()=" + getPivot() + "]";
	}

}
