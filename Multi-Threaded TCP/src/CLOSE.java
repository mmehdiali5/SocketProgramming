import java.io.Serializable;

public class CLOSE implements Serializable {
  String id;
  int close = -1;

  public CLOSE(String id) {
    this.id = id;
  }
}
