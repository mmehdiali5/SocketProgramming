import java.io.Serializable;

public class GET implements Serializable {
  String id;
  String key="DEFAULT";

  public GET(String key, String id) {
    this.key=key;
    this.id=id;
  }
}
