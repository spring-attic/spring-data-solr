package org.springframework.data.solr.core.query;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.solr.common.params.FacetParams;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * @author juhrlass
 */
public class FacetRangeOptions {

    public static final int DEFAULT_FACET_LIMIT = 10;
    public static final FacetOptions.FacetSort DEFAULT_FACET_SORT = FacetOptions.FacetSort.COUNT;

    private List<Field> facetRangeOnFields = new ArrayList<Field>(1);
    private int facetLimit = DEFAULT_FACET_LIMIT;
    private FacetOptions.FacetSort facetSort = DEFAULT_FACET_SORT;
    private Pageable pageable;

    /**
     * Creates new instance range faceting on fields with given name
     *
     * @param fieldnames
     */
    public FacetRangeOptions(String... fieldnames) {
        Assert.notNull(fieldnames, "Fields must not be null.");
        Assert.noNullElements(fieldnames, "Cannot range facet on null fieldname.");

        for (String fieldname : fieldnames) {
            addFacetRangeOnField(fieldname);
        }
    }

    /**
     * Creates new instance faceting on given fields
     *
     * @param fields
     */
    public FacetRangeOptions(Field... fields) {
        Assert.notNull(fields, "Fields must not be null.");
        Assert.noNullElements(fields, "Cannot range facet on null field.");

        for (Field field : fields) {
            addFacetRangeOnField(field);
        }
    }

    /**
     * Append additional field for faceting
     *
     * @param field
     * @return
     */
    public final FacetRangeOptions addFacetRangeOnField(Field field) {
        Assert.notNull(field, "Cannot facet on null field.");
        Assert.hasText(field.getName(), "Cannot range facet on field with null/empty fieldname.");

        this.facetRangeOnFields.add(field);
        return this;
    }

    /**
     * Append additional field with given name for faceting
     *
     * @param fieldname
     * @return
     */
    public final FacetRangeOptions addFacetRangeOnField(String fieldname) {
        addFacetRangeOnField(new SimpleField(fieldname));
        return this;
    }

    /**
     * Append all fieldnames for faceting
     *
     * @param fieldnames
     * @return
     */
    public final FacetRangeOptions addFacetRangeOnFieldnames(Collection<String> fieldnames) {
        Assert.notNull(fieldnames);

        for (String fieldname : fieldnames) {
            addFacetRangeOnField(fieldname);
        }
        return this;
    }

    /**
     * Get the list of Fields to facet on
     *
     * @return
     */
    public final List<Field> getFacetRangeOnFields() {
        return Collections.unmodifiableList(this.facetRangeOnFields);
    }

    /**
     * @return true if at least one facet field set
     */
    public boolean hasFields() {
        return !this.facetRangeOnFields.isEmpty();
    }

    /**
     * Set {@code facet.limit}
     *
     * @param rowsToReturn Default is 10
     * @return
     */
    public FacetRangeOptions setFacetLimit(int rowsToReturn) {
        this.facetLimit = Math.max(1, rowsToReturn);
        return this;
    }

    /**
     * Set {@code facet.sort} ({@code INDEX} or {@code COUNT})
     *
     * @param facetSort Default is {@code COUNT}
     * @return
     */
    public FacetRangeOptions setFacetSort(FacetOptions.FacetSort facetSort) {
        Assert.notNull(facetSort, "FacetSort must not be null.");

        this.facetSort = facetSort;
        return this;
    }

    /**
     * Get the max number of results per facet field.
     *
     * @return
     */
    public int getFacetLimit() {
        return this.facetLimit;
    }

    /**
     * Get sorting of facet results. Default is COUNT
     *
     * @return
     */
    public FacetOptions.FacetSort getFacetSort() {
        return this.facetSort;
    }


    /**
     * Get the facet page requested.
     *
     * @return
     */
    public Pageable getPageable() {
        return this.pageable != null ? this.pageable : new PageRequest(0, facetLimit);
    }

    /**
     * Set {@code facet.offet} and {@code facet.limit}
     *
     * @param pageable
     * @return
     */
    public FacetRangeOptions setPageable(Pageable pageable) {
        this.pageable = pageable;
        return this;
    }


    @SuppressWarnings("unchecked")
    public Collection<FieldWithFacetRangeParameters> getFieldsWithRangeParameters() {
        return (Collection<FieldWithFacetRangeParameters>) CollectionUtils.select(this.facetRangeOnFields,
                new IsFieldWithFacetRangeParametersInstancePredicate());
    }

    private static class IsFieldWithFacetRangeParametersInstancePredicate implements Predicate {

        @Override
        public boolean evaluate(Object object) {
            return object instanceof FieldWithFacetRangeParameters;
        }
    }

    public static class FacetRangeParameter extends QueryParameterImpl {

        public FacetRangeParameter(String parameter, Object value) {
            super(parameter, value);
        }

    }

    public static class FieldWithFacetRangeParameters extends FieldWithQueryParameters<FacetRangeParameter> {

        private FieldWithFacetRangeParameters(String name) {
            super(name);
        }

        /**
         * @param rangeSort
         * @return
         */
        public FieldWithFacetRangeParameters setSort(FacetOptions.FacetSort rangeSort) {
            addFacetRangeParameter(FacetParams.FACET_SORT, rangeSort, true);
            return this;
        }

        /**
         * @return null if not set
         */
        public FacetOptions.FacetSort getSort() {
            return getQueryParameterValue(FacetParams.FACET_SORT);
        }

        /**
         * @param rangeHardEnd
         * @return
         */
        public FieldWithFacetRangeParameters setHardEnd(boolean rangeHardEnd) {
            addFacetRangeParameter(FacetParams.FACET_RANGE_HARD_END, rangeHardEnd, true);
            return this;
        }

        /**
         * @return null if not set
         */
        public boolean getHardEnd() {
            return getQueryParameterValue(FacetParams.FACET_RANGE_HARD_END);
        }

        /**
         * @param rangeOther
         * @return
         */
        public FieldWithFacetRangeParameters setOther(FacetParams.FacetRangeOther rangeOther) {
            addFacetRangeParameter(FacetParams.FACET_RANGE_OTHER, rangeOther, true);
            return this;
        }

        /**
         * @return null if not set
         */
        public FacetParams.FacetRangeOther getOther() {
            return getQueryParameterValue(FacetParams.FACET_RANGE_OTHER);
        }

        /**
         * @param rangeInclude
         * @return
         */
        public FieldWithFacetRangeParameters setInclude(FacetParams.FacetRangeInclude rangeInclude) {
            addFacetRangeParameter(FacetParams.FACET_RANGE_INCLUDE, rangeInclude, true);
            return this;
        }

        /**
         * @return null if not set
         */
        public FacetParams.FacetRangeInclude getInclude() {
            return getQueryParameterValue(FacetParams.FACET_RANGE_INCLUDE);
        }

        /**
         * Add field specific parameter by name
         *
         * @param parameterName
         * @param value
         */
        public FieldWithFacetRangeParameters addFacetRangeParameter(String parameterName, Object value) {
            return addFacetRangeParameter(parameterName, value, false);
        }

        protected FieldWithFacetRangeParameters addFacetRangeParameter(String parameterName, Object value, boolean removeIfValueIsNull) {
            if (removeIfValueIsNull && value == null) {
                removeQueryParameter(parameterName);
                return this;
            }
            return this.addFacetRangeParameter(new FacetRangeParameter(parameterName, value));
        }

        /**
         * Add field specific facet parameter
         *
         * @param parameter
         * @return
         */
        public FieldWithFacetRangeParameters addFacetRangeParameter(FacetRangeParameter parameter) {
            this.addQueryParameter(parameter);
            return this;
        }

    }

    public static class FieldWithDateFacetRangeParameters extends FieldWithFacetRangeParameters {

        public FieldWithDateFacetRangeParameters(String name) {
            super(name);
        }

        /**
         * @param rangeGap
         * @return
         */
        public FieldWithFacetRangeParameters setGap(String rangeGap) {
            addFacetRangeParameter(FacetParams.FACET_RANGE_GAP, rangeGap, true);
            return this;
        }

        /**
         * @return null if not set
         */
        public String getGap() {
            return getQueryParameterValue(FacetParams.FACET_RANGE_GAP);
        }

        /**
         * @param rangeStart
         * @return
         */
        public FieldWithFacetRangeParameters setStart(Date rangeStart) {
            addFacetRangeParameter(FacetParams.FACET_RANGE_START, rangeStart, true);
            return this;
        }

        /**
         * @return null if not set
         */
        public Date getStart() {
            return getQueryParameterValue(FacetParams.FACET_RANGE_START);
        }

        /**
         * @param rangeEnd
         * @return
         */
        public FieldWithFacetRangeParameters setEnd(Date rangeEnd) {
            addFacetRangeParameter(FacetParams.FACET_RANGE_END, rangeEnd, true);
            return this;
        }

        /**
         * @return null if not set
         */
        public Date getEnd() {
            return getQueryParameterValue(FacetParams.FACET_RANGE_END);
        }
    }

    public static class FieldWithNumericFacetRangeParameters extends FieldWithFacetRangeParameters {


        public FieldWithNumericFacetRangeParameters(String name) {
            super(name);
        }

        /**
         * @param rangeGap
         * @return
         */
        public FieldWithFacetRangeParameters setGap(Number rangeGap) {
            addFacetRangeParameter(FacetParams.FACET_RANGE_GAP, rangeGap, true);
            return this;
        }

        /**
         * @return null if not set
         */
        public Number getGap() {
            return getQueryParameterValue(FacetParams.FACET_RANGE_GAP);
        }

        /**
         * @param rangeStart
         * @return
         */
        public FieldWithFacetRangeParameters setStart(Number rangeStart) {
            addFacetRangeParameter(FacetParams.FACET_RANGE_START, rangeStart, true);
            return this;
        }

        /**
         * @return null if not set
         */
        public Number getStart() {
            return getQueryParameterValue(FacetParams.FACET_RANGE_START);
        }

        /**
         * @param rangeEnd
         * @return
         */
        public FieldWithFacetRangeParameters setEnd(Number rangeEnd) {
            addFacetRangeParameter(FacetParams.FACET_RANGE_END, rangeEnd, true);
            return this;
        }

        /**
         * @return null if not set
         */
        public Number getEnd() {
            return getQueryParameterValue(FacetParams.FACET_RANGE_END);
        }
    }


}
