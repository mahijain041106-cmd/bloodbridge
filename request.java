
package backend;

public class request {

    private int requesterUserId;
    private String requiredGroupCode;
    private int componentId;
    private int quantity;
    private int priorityLevel;
    private String requestNote;

    public request(int requesterUserId,
                   String requiredGroupCode,
                   int componentId,
                   int quantity,
                   int priorityLevel,
                   String requestNote) {

        this.requesterUserId = requesterUserId;
        this.requiredGroupCode = requiredGroupCode;
        this.componentId = componentId;
        this.quantity = quantity;
        this.priorityLevel = priorityLevel;
        this.requestNote = requestNote;
    }

    public int getRequesterUserId() {
        return requesterUserId;
    }

    public String getRequiredGroupCode() {
        return requiredGroupCode;
    }

    public int getComponentId() {
        return componentId;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getPriorityLevel() {
        return priorityLevel;
    }

    public String getRequestNote() {
        return requestNote;
    }
}
