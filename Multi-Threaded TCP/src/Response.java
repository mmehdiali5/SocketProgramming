import java.io.Serializable;

class Response implements Serializable {
  private String status;
  private String message;

  private String reqId;

  public Response(String status, String message, String id) {
    this.status = status;
    this.message = message;
    this.reqId = id;
  }

  public String getStatus() {
    return status;
  }

  public String getMessage() {
    return message;
  }

  public String getReqId() {
    return reqId;
  }

  @Override
  public String toString() {
    return "Response{" + "status='" + status + '\'' + ", message='" + message + '\'' + '}';
  }
}
