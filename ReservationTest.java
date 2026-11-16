package backend;

public class ReservationTest {

    public static void main(String[] args) {

        ReservationService service = new ReservationService();

        System.out.println("===== CONFIRMED RESERVATION CANCELLATION TEST =====");

        service.cancelReservation(1);

        System.out.println("Confirmed reservation cancellation attempted.");
    }
}