import java.io.Serializable;

public class PUT implements Serializable {
  public String key;
  public String value;
  public String id;

  public PUT(String key, String value, String id) {
    this.key = key;
    this.value = value;
    this.id = id;
  }
}
