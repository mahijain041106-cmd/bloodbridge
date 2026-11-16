package backend;

import java.sql.Timestamp;

/**
 * One row of request_queue.
 * compareTo is the scheduling rule in plain Java:
 *   higher priority first; if priorities are equal, smaller queueId (arrived earlier) first.
 */
public class QueueEntry implements Comparable<QueueEntry> {
    private final int queueId;
    private final int requestId;
    private final int priority;
    private final String status;
    private final Timestamp enqueuedAt;

    public QueueEntry(int queueId, int requestId, int priority, String status, Timestamp enqueuedAt) {
        this.queueId = queueId;
        this.requestId = requestId;
        this.priority = priority;
        this.status = status;
        this.enqueuedAt = enqueuedAt;
    }

    public int getQueueId()         { return queueId; }
    public int getRequestId()       { return requestId; }
    public int getPriority()        { return priority; }
    public String getStatus()       { return status; }
    public Timestamp getEnqueuedAt() { return enqueuedAt; }

    @Override
    public int compareTo(QueueEntry other) {
        if (this.priority != other.priority) return Integer.compare(other.priority, this.priority); // priority scheduling
        return Integer.compare(this.queueId, other.queueId);                                       // FCFS tie-break
    }

    @Override
    public String toString() {
        return "queue#" + queueId + "  request#" + requestId + "  priority=" + priority + "  " + status;
    }
}
