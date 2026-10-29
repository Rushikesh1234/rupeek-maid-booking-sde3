package com.rupeek.maidbooking.domain.model;

import java.util.Objects;

/**
 * Value Object representing locality and coordinates for discovery.
 */
public final class Location {
    private final String locality;
    private final String city;
    private final String pincode;
    private final double latitude;
    private final double longitude;

    public Location(String locality, String city, String pincode, double latitude, double longitude) {
        if (locality == null || locality.isBlank()) {
            throw new IllegalArgumentException("Locality is required");
        }
        if (city == null || city.isBlank()) {
            throw new IllegalArgumentException("City is required");
        }
        this.locality = locality.trim();
        this.city = city.trim();
        this.pincode = pincode == null ? "" : pincode.trim();
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getLocality() {
        return locality;
    }

    public String getCity() {
        return city;
    }

    public String getPincode() {
        return pincode;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public boolean matchesLocality(String requestedLocality) {
        if (requestedLocality == null || requestedLocality.isBlank()) {
            return true;
        }
        return this.locality.equalsIgnoreCase(requestedLocality.trim());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Location location)) return false;
        return Objects.equals(locality.toLowerCase(), location.locality.toLowerCase())
                && Objects.equals(city.toLowerCase(), location.city.toLowerCase())
                && Objects.equals(pincode, location.pincode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(locality.toLowerCase(), city.toLowerCase(), pincode);
    }
}
