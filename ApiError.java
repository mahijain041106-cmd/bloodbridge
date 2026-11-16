package backend;

import java.time.LocalDateTime;

public record ApiError(LocalDateTime time, int status, String message) {
}
