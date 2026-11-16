
package backend;

public class RecoveryBloodUnit {

    private Long id;
    private String status;

    public RecoveryBloodUnit() {
    }

    public RecoveryBloodUnit(Long id, String status) {
        this.id = id;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public String getStatus() {
        return status;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
