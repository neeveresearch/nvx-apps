package com.neeve.geofencer;

import java.util.ArrayList;
import java.util.List;

/**
 * defines a geo fence around a trip route marked by cross bars or segments perpendicular to the route
 * the two ends of each segment mark the boundary of the fence at that location
 * two consecutive segments define one quadrilateral block of the fence
 */
public class RouteFence {

    public static class Location {
        int latitude;
        int longitude;

        public Location() {}

        public Location(int latitude, int longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public int getLatitude() {
            return latitude;
        }

        public int getLongitude() {
            return longitude;
        }
    }

    public static class Segment {
        private Location start;
        private Location end;

        public Segment() {}

        public Segment(Location start, Location end) {
            this.start = start;
            this.end = end;
        }

        public Location getStart() {
            return start;
        }

        public Location getEnd() {
            return end;
        }
    }

    private List<Segment> waypointSegments;

    public RouteFence() {
        waypointSegments = new ArrayList<Segment>();
    }

    public RouteFence(List<Segment> segments) {
        waypointSegments = segments;
    }

    // which side of segment is location on?
    // true is one side, false is the other side
    private boolean getSide(Location location, Location segmentStart, Location segmentEnd) {
        long x = location.getLatitude();
        long y = location.getLongitude();
        long x1 = segmentStart.getLatitude();
        long y1 = segmentStart.getLongitude();
        long x2 = segmentEnd.getLatitude();
        long y2 = segmentEnd.getLongitude();

        return (x - x1) * (y2 - y1) - (y - y1) * (x2 - x1) >= 0;
    }

    // is location within the quadrilateral defined by segments 1 & 2?
    private boolean isWithin(Location location, Segment segment1, Segment segment2) {
        boolean side = getSide(location, segment1.start, segment1.end);
        if (side != getSide(location, segment1.end, segment2.end)) {
            return false;
        }
        if (side != getSide(location, segment2.end, segment2.start)) {
            return false;
        }
        return side == getSide(location, segment2.start, segment1.start);
    }

    // looks for location at current and next fence block
    public int locate(Location location, int lastIndex) {
        // check current and next fence box
        for (int i = lastIndex; i < Math.min(lastIndex + 2, waypointSegments.size() - 1); i++) {
            if (isWithin(location, waypointSegments.get(i), waypointSegments.get(i + 1))) {
                return i;
            }
        }
        return -1;
    }

    public int locate(int latitude, int longitude, int lastIndex) {
        return locate(new Location(latitude, longitude), lastIndex);
    }

    public List<Segment> getWaypointSegments() {
        return waypointSegments;
    }
}
