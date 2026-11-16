
package backend;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RecoveryScheduler implements AutoCloseable {

    private final RecoveryService service;
    private final ScheduledExecutorService scheduler;

    public RecoveryScheduler() {
        this.service = new RecoveryService();

        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "BloodBridge-Recovery");
            thread.setDaemon(true);
            return thread;
        });
    }

    public void start() {
        scheduler.scheduleWithFixedDelay(() -> {
            try {
                service.cleanupExpiredReservations();
            } catch (Exception e) {
                System.out.println("Scheduled recovery error: " + e.getMessage());
            }
        }, 0, 60, TimeUnit.SECONDS);

        System.out.println("Recovery scheduler started.");
    }

    public void runNow() {
        service.cleanupExpiredReservations();
    }

    @Override
    public void close() {
        scheduler.shutdownNow();
        System.out.println("Recovery scheduler stopped.");
    }
}
