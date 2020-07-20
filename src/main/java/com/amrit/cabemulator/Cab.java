package com.amrit.cabemulator;

import com.amrit.cabemulator.util.StraightLine;
import com.amrit.taxiserviceapi.messaging.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Cab implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(Cab.class.getName());

    private final String regNo;
    private Trip trip;
    private Position position;
    private final List<UpdateSubscriber> subscribers;

    static class Trip {
        private final List<Position> positions;
        private final List<StraightLine> paths;
        private StraightLine currentPath;
        private int index;
        private boolean ended;

        public Trip(List<Position> positions) {
            this.positions = positions;
            this.paths = new ArrayList<>();
            ended = false;
            constructPaths();
            index = 0;
            currentPath = paths.get(index++);
        }

        private void constructPaths() {
            for (int i = 0; i < positions.size() - 1; i++) {
                paths.add(new StraightLine(positions.get(i), positions.get(i+1)));
            }
        }

        private void proceedToNextPath() {
            if (index < paths.size()) {
                currentPath = paths.get(index++);
            } else  {
                ended = true;
            }
        }
    }

    interface UpdateSubscriber {
        void notifyUpdate(Cab cab);
    }

    public Cab(String regNo) {
        this.regNo = regNo;
        this.subscribers = new CopyOnWriteArrayList<>();
    }

    public void addPositionUpdateSubscriber(UpdateSubscriber subscriber) {
        this.subscribers.add(subscriber);
    }

    public void removePositionSubscriber(UpdateSubscriber subscriber) {
        this.subscribers.remove(subscriber);
    }

    public synchronized void setTrip(List<Position> positions) {
        if (trip == null) {
            trip = new Trip(positions);
            new Thread(this).start();
        } else {
            throw new IllegalStateException("Already on a trip");
        }
    }


    public void run() {
        while (!trip.ended) {
            Position next = trip.currentPath.getNextPosition();
            position = next;
            LOGGER.debug("Updated trip to position " + position);
            subscribers.forEach(s -> s.notifyUpdate(this));
            if (trip.currentPath.isEndPoint(next)) {
                trip.proceedToNextPath();
            }
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public String getRegNo() {
        return regNo;
    }

    public Position getPosition() {
        return position;
    }

    public boolean isOnTrip() {
        return !trip.ended;
    }
}
