import java.io.Serializable;

public class DELETE implements Serializable {
  public String id;
  public String key;

  public DELETE(String key, String id) {
    this.id = id;
    this.key = key;
  }
}
