/*
 * Copyright 2012 - 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.solr.core.query;

import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a JSON <a href="https://lucene.apache.org/solr/guide/8_5/json-facet-api.html#range-facet">range facet</a>.
 * 
 * @author Joe Linn
 */
public class JsonRangeFacet extends JsonFieldFacet {
	private Number start;
	private Number end;
	private Number gap;
	@JsonProperty("hardend") private Boolean hardEnd;
	private Other other;
	private Include include = Include.LOWER;
	private List<Range> ranges = new LinkedList<>();

	@Override
	public String getType() {
		return "range";
	}

	public Number getStart() {
		return start;
	}

	public JsonRangeFacet setStart(Number start) {
		this.start = start;
		return this;
	}

	public Number getEnd() {
		return end;
	}

	public JsonRangeFacet setEnd(Number end) {
		this.end = end;
		return this;
	}

	public Number getGap() {
		return gap;
	}

	public JsonRangeFacet setGap(Number gap) {
		this.gap = gap;
		return this;
	}

	public Boolean isHardEnd() {
		return hardEnd;
	}

	public JsonRangeFacet setHardEnd(Boolean hardEnd) {
		this.hardEnd = hardEnd;
		return this;
	}

	public Other getOther() {
		return other;
	}

	public JsonRangeFacet setOther(Other other) {
		this.other = other;
		return this;
	}

	public Include getInclude() {
		return include;
	}

	public JsonRangeFacet setInclude(Include include) {
		this.include = include;
		return this;
	}

	public List<Range> getRanges() {
		return ranges;
	}

	public JsonRangeFacet setRanges(List<Range> ranges) {
		this.ranges = ranges;
		return this;
	}

	public JsonRangeFacet addRange(Range range) {
		ranges.add(range);
		return this;
	}

	public static enum Other {
		BEFORE, AFTER, BETWEEN, NONE, ALL;
	}

	public static enum Include {
		LOWER, UPPER, EDGE, OUTER, ALL;
	}

	public static class Range {
		private Number from;
		private Number to;
		@JsonProperty("inclusive_from") private boolean inclusiveFrom = true;
		@JsonProperty("inclusive_to") private boolean inclusiveTo = false;

		public Number getFrom() {
			return from;
		}

		public Range setFrom(Number from) {
			this.from = from;
			return this;
		}

		public Number getTo() {
			return to;
		}

		public Range setTo(Number to) {
			this.to = to;
			return this;
		}

		public boolean isInclusiveFrom() {
			return inclusiveFrom;
		}

		public Range setInclusiveFrom(boolean inclusiveFrom) {
			this.inclusiveFrom = inclusiveFrom;
			return this;
		}

		public boolean isInclusiveTo() {
			return inclusiveTo;
		}

		public Range setInclusiveTo(boolean inclusiveTo) {
			this.inclusiveTo = inclusiveTo;
			return this;
		}
	}
}
