import com.amrit.taxiserviceapi.messaging.Position;
import util.StraightLine;

import java.util.ArrayList;
import java.util.List;

public class Cab implements Runnable {

    private final String regNo;
    private Trip trip;
    private Position position;

    static class Trip {
        private final List<Position> positions;
        private final List<StraightLine> paths;
        private StraightLine currentPath;

        public Trip(List<Position> positions) {
            this.positions = positions;
            this.paths = new ArrayList<>();
            constructPaths();
            currentPath = paths.get(0);
        }

        private void constructPaths() {
            for (int i = 0; i < positions.size() - 1; i++) {
                paths.add(new StraightLine(positions.get(i), positions.get(i+1)));
            }
        }
    }

    public Cab(String regNo) {
        this.regNo = regNo;
    }

    public synchronized void setTrip(List<Position> positions) {
        if (trip == null) {
            trip = new Trip(positions);
        } else {
            throw new IllegalStateException("Already on a trip");
        }
    }


    public void run() {
        while (trip.currentPath != null) {
            Position next = trip.currentPath.getNextPosition();

//            if (next.equals(trip.currentPath.to)) {
//
//            }
        }
    }

    public String getRegNo() {
        return regNo;
    }
}
