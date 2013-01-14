package org.springframework.data.solr.core.geo;

/**
 * Created with IntelliJ IDEA.
 * User: jdorman
 * Date: 1/14/13
 * Time: 1:42 PM
 * To change this template use File | Settings | File Templates.
 */
public class BoundingBox {

    public BoundingBox(GeoLocation geoLocationStart, GeoLocation geoLocationEnd) {
        this.geoLocationStart = geoLocationStart;
        this.geoLocationEnd = geoLocationEnd;
    }

    private GeoLocation geoLocationStart;
    private GeoLocation geoLocationEnd;

    public GeoLocation getGeoLocationStart() {
        return geoLocationStart;
    }

    public void setGeoLocationStart(GeoLocation geoLocationStart) {
        this.geoLocationStart = geoLocationStart;
    }

    public GeoLocation getGeoLocationEnd() {
        return geoLocationEnd;
    }

    public void setGeoLocationEnd(GeoLocation geoLocationEnd) {
        this.geoLocationEnd = geoLocationEnd;
    }
}
