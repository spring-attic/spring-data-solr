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

import java.util.Arrays;

import com.google.common.base.CaseFormat;

/**
 * @author Joe Linn
 */
public class StatFacetFunction extends AbstractFunction {
	private final Func func;

	public StatFacetFunction(Func func, Object... arguments) {
		super(Arrays.asList(arguments));
		this.func = func;
	}

	@Override
	public String getOperation() {
		return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, func.name());
	}

	@Override
	public String toSolrFunction(Context context) {
		StringBuilder str = new StringBuilder(getOperation()).append("(");
		String comma = "";
		for (Object parameter : getArguments()) {
			str.append(comma);
			if (parameter instanceof StatFacetFunction) {
				str.append(((StatFacetFunction) parameter).toSolrFunction(context));
			} else {
				str.append(context.convert(parameter));
			}
			comma = ",";
		}
		str.append(")");
		return str.toString();
	}

	public static StatFacetFunction sum(String field) {
		return new StatFacetFunction(Func.SUM, field);
	}

	public static StatFacetFunction sum(StatFacetFunction function) {
		return new StatFacetFunction(Func.SUM, function);
	}

	public static StatFacetFunction avg(String field) {
		return new StatFacetFunction(Func.AVG, field);
	}

	public static StatFacetFunction avg(StatFacetFunction function) {
		return new StatFacetFunction(Func.AVG, function);
	}

	public static StatFacetFunction min(String field) {
		return new StatFacetFunction(Func.MIN, field);
	}

	public static StatFacetFunction min(StatFacetFunction function) {
		return new StatFacetFunction(Func.MIN, function);
	}

	public static StatFacetFunction max(String field) {
		return new StatFacetFunction(Func.MAX, field);
	}

	public static StatFacetFunction max(StatFacetFunction function) {
		return new StatFacetFunction(Func.MAX, function);
	}

	public static StatFacetFunction missing(String field) {
		return new StatFacetFunction(Func.MISSING, field);
	}

	public static StatFacetFunction missing(StatFacetFunction function) {
		return new StatFacetFunction(Func.MISSING, function);
	}

	public static StatFacetFunction countvals(String field) {
		return new StatFacetFunction(Func.COUNTVALS, field);
	}

	public static StatFacetFunction countvals(StatFacetFunction function) {
		return new StatFacetFunction(Func.COUNTVALS, function);
	}

	public static StatFacetFunction unique(String field) {
		return new StatFacetFunction(Func.UNIQUE, field);
	}

	public static StatFacetFunction uniqueBlock(String field) {
		return new StatFacetFunction(Func.UNIQUE_BLOCK, field);
	}

	public static StatFacetFunction uniqueBlock(Criteria criteria) {
		return new StatFacetFunction(Func.UNIQUE_BLOCK, criteria);
	}

	public static StatFacetFunction hll(String field) {
		return new StatFacetFunction(Func.HLL, field);
	}

	// bug in solrj causes results of percentile ƒacet to not be parsed properly:
	// https://issues.apache.org/jira/browse/SOLR-14006
	/*public static StatFacetFunction percentile(String field, float... quantiles) {
	    return percentile((Object) field, quantiles);
	}
	
	public static StatFacetFunction percentile(StatFacetFunction function, float... quantiles) {
	    return percentile((Object) function, quantiles);
	}*/

	private static StatFacetFunction percentile(Object fieldOrFunction, float... quantiles) {
		Object[] params = new Object[quantiles.length + 1];
		params[0] = fieldOrFunction;
		for (int i = 0; i < quantiles.length; i++) {
			params[i + 1] = quantiles[i];
		}
		return new StatFacetFunction(Func.PERCENTILE, params);
	}

	public static StatFacetFunction sumsq(String field) {
		return new StatFacetFunction(Func.SUMSQ, field);
	}

	public static StatFacetFunction sumsq(StatFacetFunction function) {
		return new StatFacetFunction(Func.SUMSQ, function);
	}

	public static StatFacetFunction variance(String field) {
		return new StatFacetFunction(Func.VARIANCE, field);
	}

	public static StatFacetFunction variance(StatFacetFunction function) {
		return new StatFacetFunction(Func.VARIANCE, function);
	}

	public static StatFacetFunction stddev(String field) {
		return new StatFacetFunction(Func.STDDEV, field);
	}

	public static StatFacetFunction stddev(StatFacetFunction function) {
		return new StatFacetFunction(Func.STDDEV, function);
	}

	public static StatFacetFunction relatedness(String foreground, String background) {
		return new StatFacetFunction(Func.RELATEDNESS, foreground, background);
	}

	public enum Func {
		SUM, AVG, MIN, MAX, MISSING, COUNTVALS, UNIQUE, UNIQUE_BLOCK, HLL, PERCENTILE, SUMSQ, VARIANCE, STDDEV, RELATEDNESS
	}
}
