
package backend;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class RequestQueue {

    private static final int QUEUE_CAPACITY = 5;

    private static final BlockingQueue<Integer> queue =
            new ArrayBlockingQueue<>(QUEUE_CAPACITY);

    public static final int STOP_SIGNAL = -1;

    // Producer adds requests to the queue
    public static void addRequest(int requestId)
            throws InterruptedException {

        queue.put(requestId);

        System.out.println(
                "Request " + requestId + " added to queue."
        );
    }

    // Worker retrieves a request from the queue
    public static int getRequest() throws InterruptedException {
        return queue.take();
    }

    // Send shutdown signal to a worker
    public static void stopWorker() throws InterruptedException {
        queue.put(STOP_SIGNAL);
    }

    public static int getQueueSize() {
        return queue.size();
    }
}
