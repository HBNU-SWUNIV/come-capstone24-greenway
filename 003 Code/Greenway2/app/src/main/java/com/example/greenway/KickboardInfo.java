package com.example.greenway;

import java.util.Objects;

public class KickboardInfo {
    private String location;
    private boolean hasStand;
    private String standSize;

    public KickboardInfo(String location, boolean hasStand, String standSize) {
        this.location = location;
        this.hasStand = hasStand;
        this.standSize = standSize;
    }

    public String getLocation() {
        return location;
    }

    public boolean isHasStand() {
        return hasStand;
    }

    public String getStandSize() {
        return standSize;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KickboardInfo that = (KickboardInfo) o;
        return hasStand == that.hasStand &&
                Objects.equals(location, that.location) ;
    }

    @Override
    public int hashCode() {
        return Objects.hash(location, hasStand);
    }
}