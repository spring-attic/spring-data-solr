package org.springframework.data.solr.core.query;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

/**
 * The most trivial implementation of {@link PivotField}.
 * 
 * @author Francisco Spaeth
 * 
 */
public class SimplePivotField implements PivotField {

	private List<Field> fields;
	private String name;

	public SimplePivotField(String fieldName) {
		Assert.notNull(fieldName, "fieldName should not be null");
		Assert.isTrue(fieldName.contains(","),
				"a pivot field needs to be composed by 2 or more solr fields separated by comma");

		this.name = fieldName;

		fields = new ArrayList<Field>();
		for (String field : StringUtils.split(fieldName, ",")) {
			fields.add(new SimpleField(field));
		}
	}

	public SimplePivotField(List<Field> fields) {
		Assert.notNull(fields, "fields should not be null");
		Assert.isTrue(fields.size() > 1, "a pivot field needs to be composed by 2 or more solr fields");

		this.fields = new ArrayList<Field>(fields);

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < fields.size(); i++) {
			if (i > 0)
				sb.append(',');
			sb.append(fields.get(i).getName());
		}
		this.name = sb.toString();
	}

	@Override
	public List<Field> getFields() {
		return fields;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SimplePivotField other = (SimplePivotField) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

}