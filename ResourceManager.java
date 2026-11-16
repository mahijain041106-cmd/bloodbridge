
package backend;

import java.util.concurrent.Semaphore;

public class ResourceManager {

    private static final Semaphore allocationSemaphore =
            new Semaphore(1);

    public static void acquireResource() throws InterruptedException {
        allocationSemaphore.acquire();
    }

    public static void releaseResource() {
        allocationSemaphore.release();
    }
}
