
package backend;

import java.util.List;
import java.util.Map;

public class RecoveryController {

    private final RecoveryService service;

    public RecoveryController() {
        this.service = new RecoveryService();
    }

    public Map<String, Integer> cleanup() {
        return service.cleanupExpiredReservations();
    }

    public List<RecoveryLog> history() {
        return service.getHistory();
    }

    public boolean recoverReservation(Long reservationId) {
        return service.recoverOne(reservationId);
    }
}
