package model;

import java.util.PriorityQueue;

public class PriorityQueueManager {

    private PriorityQueue<Donor> queue =
            new PriorityQueue<>((a, b) -> a.getPriority() - b.getPriority());

    public void addDonor(Donor d) {
        queue.add(d);
    }

    public Donor getNextDonor() {
        return queue.poll();
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }
}
