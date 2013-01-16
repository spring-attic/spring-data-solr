package org.springframework.data.solr.core.geo;

/**
 * @author John Dorman
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
