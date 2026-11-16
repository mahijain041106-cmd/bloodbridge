
package backend;

public class RequestManager {

    private static boolean started = false;

    public static synchronized void start() {

        if (started) {
            return;
        }

        started = true;

        System.out.println(
            "BloodBridge Background Processing Started"
        );

        // Start permanent workers
        Thread worker1 = new Thread(
            new RequestConsumer(1), "Worker-1"
        );

        Thread worker2 = new Thread(
            new RequestConsumer(2), "Worker-2"
        );

        worker1.start();
        worker2.start();

        // Load requests that were pending when the server started
        Thread producer = new Thread(
            new RequestProducer(), "Producer"
        );

        producer.start();
    }

    public static void main(String[] args) {
        start();
    }
}
