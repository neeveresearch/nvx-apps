package com.neeve.geofencer;

import java.util.ArrayList;
import java.util.List;

import com.neeve.geofencer.RouteFence.Location;
import com.neeve.geofencer.entities.GPSCoordinate;
import com.neeve.geofencer.entities.Segment;
import com.neeve.geofencer.entities.VehicleRoute;

/**
 * Utility to generate a test geo fence given a start and end location
 */
public class FenceUtil {
    public static GPSCoordinate START_LOCATION = createLocation(32704443, -117159252);
    public static GPSCoordinate END_LOCATION = createLocation(44790496, -68761916);

    public static Segment createSegment(GPSCoordinate start, GPSCoordinate end) {
        Segment segment = Segment.create();
        segment.setStartLocation(start);
        segment.setEndLocation(end);
        return segment;
    }

    public static GPSCoordinate createLocation(int latitude, int longitude) {
        GPSCoordinate location = GPSCoordinate.create();
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        return location;
    }

    public static VehicleRoute createRoute(GPSCoordinate start, GPSCoordinate end, List<Segment> segments) {
        VehicleRoute route = VehicleRoute.create();
        route.setStartLocation(start);
        route.setEndLocation(end);
        route.setSegments(segments.toArray(new Segment[segments.size()]));
        return route;
    }

    // Generates a route fence for a straight route between start and end locations
    // - creates numPoints way point segments for the fence
    public static VehicleRoute generateRoute(GPSCoordinate start, GPSCoordinate end, int numPoints) {
        List<Segment> segments = new ArrayList<Segment>();
        int latStep = (end.getLatitude() - start.getLatitude()) / numPoints;
        int lngStep = (end.getLongitude() - start.getLongitude()) / numPoints;
        int fenceLatLen = latStep > 0 ? 10000 : -10000;
        int fenceLngLen = lngStep > 0 ? 10000 : -10000;
        GPSCoordinate s1 = createLocation(start.getLatitude() + fenceLatLen, start.getLongitude() - fenceLngLen);
        GPSCoordinate s2 = createLocation(start.getLatitude() - fenceLatLen, start.getLongitude() + fenceLngLen);

        segments.add(createSegment(s1, s2));
        for (int i = 1; i < numPoints - 1; i++) {
            segments.add(createSegment(createLocation(s1.getLatitude() + latStep * i, s1.getLongitude() + lngStep * i), createLocation(s2.getLatitude() + latStep * i, s2.getLongitude() + lngStep * i)));
        }
        segments.add(createSegment(createLocation(end.getLatitude() + fenceLatLen, end.getLongitude() - fenceLngLen), createLocation(end.getLatitude() - fenceLatLen, end.getLongitude() + fenceLngLen)));
        return createRoute(start, end, segments);
    }

    // which side of segment is location on?
    // true is one side, false is the other side
    private static boolean getSide(GPSCoordinate location, GPSCoordinate segmentStart, GPSCoordinate segmentEnd) {
        long x = location.getLatitude();
        long y = location.getLongitude();
        long x1 = segmentStart.getLatitude();
        long y1 = segmentStart.getLongitude();
        long x2 = segmentEnd.getLatitude();
        long y2 = segmentEnd.getLongitude();

        return (x - x1) * (y2 - y1) - (y - y1) * (x2 - x1) >= 0;
    }

    // is location within the quadrilateral defined by segments 1 & 2?
    private static boolean isWithin(GPSCoordinate location, Segment segment1, Segment segment2) {
        boolean side = getSide(location, segment1.getStartLocation(), segment1.getEndLocation());
        if (side != getSide(location, segment1.getEndLocation(), segment2.getEndLocation())) {
            return false;
        }
        if (side != getSide(location, segment2.getEndLocation(), segment2.getStartLocation())) {
            return false;
        }
        return side == getSide(location, segment2.getStartLocation(), segment1.getStartLocation());
    }

    // looks for location at current and next fence block
    public static int locate(GPSCoordinate location, VehicleRoute route, int lastIndex) {
        // check current and next fence box
        final Segment[] segments = route.getSegments();
        for (int i = lastIndex; i < Math.min(lastIndex + 2, segments.length - 1); i++) {
            if (isWithin(location, segments[i], segments[i + 1])) {
                return i;
            }
        }
        return -1;
    }

    // Generates a route fence for a straight route between start and end locations
    // - creates numPoints way point segments for the fence
    public static RouteFence generateFence(Location start, Location end, int numPoints) {
        List<RouteFence.Segment> segments = new ArrayList<RouteFence.Segment>();
        int latStep = (end.getLatitude() - start.getLatitude()) / numPoints;
        int lngStep = (end.getLongitude() - start.getLongitude()) / numPoints;
        int fenceLatLen = latStep > 0 ? 10000 : -10000;
        int fenceLngLen = lngStep > 0 ? 10000 : -10000;
        Location s1 = new Location(start.getLatitude() + fenceLatLen, start.getLongitude() - fenceLngLen);
        Location s2 = new Location(start.getLatitude() - fenceLatLen, start.getLongitude() + fenceLngLen);

        segments.add(new RouteFence.Segment(s1, s2));
        for (int i = 1; i < numPoints - 1; i++) {
            segments.add(new RouteFence.Segment(new Location(s1.getLatitude() + latStep * i, s1.getLongitude() + lngStep * i), new Location(s2.getLatitude() + latStep * i, s2.getLongitude() + lngStep * i)));
        }
        segments.add(new RouteFence.Segment(new Location(end.getLatitude() + fenceLatLen, end.getLongitude() - fenceLngLen), new Location(end.getLatitude() - fenceLatLen, end.getLongitude() + fenceLngLen)));
        return new RouteFence(segments);
    }

//    public static RouteFence generateFence(int numPoints) {
//        return generateFence(START_LOCATION, END_LOCATION, numPoints > 0 ? numPoints : 3200);
//    }

    public static void print(RouteFence fence) {
        System.out.println("  -- Fence  --------");
        for (RouteFence.Segment segment : fence.getWaypointSegments()) {
            System.out.println(segment.getStart().latitude + ", " + segment.getStart().longitude + " -- " + segment.getEnd().latitude + ", " + segment.getEnd().longitude);
        }
        System.out.println("  -- Fence  --------");
    }
}
