package org.springframework.data.solr.core.query.result;

import java.util.List;

import org.springframework.data.solr.core.query.Field;

/**
 * The most trivial implementation of {@link FacetPivotFieldEntry}.
 * 
 * @author Francisco Spaeth
 * @author Christoph Strobl
 */
public class SimpleFacetPivotEntry extends FieldValueCountEntry implements FacetPivotFieldEntry {

	private List<FacetPivotFieldEntry> pivot;

	public SimpleFacetPivotEntry(Field field, String value, long count) {
		super(value, count);
		this.setField(field);
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
		return "SimpleFacetPivotEntry [getField()=" + getField() + ", getValueCount()=" + getValueCount() + ", getValue()="
				+ getValue() + ", getPivot()=" + getPivot() + "]";
	}

}
