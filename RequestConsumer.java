
package backend;

public class RequestConsumer implements Runnable {

    private final int workerId;

    private final WorkerDAO workerDAO = new WorkerDAO();
    private final ReservationDAO reservationDAO = new ReservationDAO();
    private final RequestProcessingDAO processingDAO =
            new RequestProcessingDAO();

    public RequestConsumer(int workerId) {
        this.workerId = workerId;
    }

    @Override
    public void run() {

        long databaseWorkerId = -1;

        try {

            String workerCode = "WORKER-" + workerId;

            String processId = String.valueOf(
                    ProcessHandle.current().pid()
            );

            String threadId = Thread.currentThread().getName();

            databaseWorkerId = workerDAO.registerWorker(
                    workerCode, processId, threadId
            );

            System.out.println(workerCode + " registered in database.");

            while (true) {

                workerDAO.updateWorkerStatus(databaseWorkerId, "IDLE");

                int requestId = RequestQueue.getRequest();

                if (requestId == RequestQueue.STOP_SIGNAL) {
                    System.out.println(
                            workerCode + " received stop signal."
                    );
                    break;
                }

                boolean claimed = processingDAO.claimRequest(requestId);

                if (!claimed) {
                    System.out.println(
                            workerCode + " could not claim Request "
                                    + requestId + ". Skipping."
                    );
                    continue;
                }

                System.out.println(
                        workerCode + " successfully claimed Request "
                                + requestId
                );

                workerDAO.updateWorkerStatus(databaseWorkerId, "RUNNING");

                boolean acquired = false;

                try {

                    ResourceManager.acquireResource();
                    acquired = true;

                    System.out.println(
                            workerCode + " is processing Request " + requestId
                    );

                    boolean reserved = reservationDAO.reserveUnit(
                            requestId, databaseWorkerId
                    );

                    if (reserved) {

                        boolean updated = processingDAO.markFulfilled(requestId);

                        if (updated) {
                            System.out.println(
                                "Request " + requestId + " status updated to FULFILLED."
                            );
                        }

                        System.out.println(
                            workerCode +
                            " successfully reserved inventory for Request " +
                            requestId
                        );
                    } else {

                        boolean updated =
                                processingDAO.markUnfulfilled(requestId);

                        if (updated) {
                            System.out.println(
                                    "Request " + requestId +
                                    " status updated to UNFULFILLED."
                            );
                        } else {
                            System.out.println(
                                    "Could not update Request " +
                                    requestId + " status."
                            );
                        }

                        System.out.println(
                                workerCode +
                                " could not reserve inventory for Request "
                                        + requestId
                        );
                    }

                    Thread.sleep(2000);

                    System.out.println(
                            workerCode + " completed Request " + requestId
                    );

                } finally {

                    if (acquired) {
                        ResourceManager.releaseResource();
                    }
                }
            }

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            System.out.println(
                    "Worker " + workerId + " interrupted."
            );

        } catch (Exception e) {

            System.out.println(
                    "Worker " + workerId + " error: " + e.getMessage()
            );

        } finally {

            if (databaseWorkerId != -1) {

                try {
                    workerDAO.updateWorkerStatus(
                            databaseWorkerId, "STOPPED"
                    );
                } catch (Exception e) {
                    System.out.println(
                            "Could not update worker status."
                    );
                }
            }
        }
    }
}
