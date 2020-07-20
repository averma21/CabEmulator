package com.amrit.cabemulator.util;

import com.amrit.taxiserviceapi.messaging.Position;

public class StraightLine {

    private static final double X_LEAP = 20;

    final Position from;
    final Position to;
    final double m;
    final double c;
    Position current;

    public StraightLine(Position from, Position to) {
        this.from = from;
        this.to = to;
        this.current = from;
        m = (from.getLatitude() - to.getLatitude()) / (from.getLongitude() - to.getLongitude());
        c = to.getLatitude() - m*to.getLongitude();
    }

    private double distance(Position from, Position to) {
        return Math.sqrt(Math.pow(to.getLatitude() - from.getLatitude(), 2) + Math.pow(to.getLongitude() - from.getLongitude(), 2));
    }

    public Position getNextPosition() {
        double nextLongitude = current.getLongitude() + X_LEAP * (to.getLongitude() > current.getLongitude() ?  1 : -1);
        double nextLatitude = m*nextLongitude + c;
        Position newPos = new Position(nextLatitude, nextLongitude);
        if (distance(current, newPos) < distance(current, to)) {
            current = newPos;
            return newPos;
        }
        current = to;
        return to;
    }

    public boolean isEndPoint(Position position) {
        return to.equals(position);
    }
}
