import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;

public class SerializerDeserializer {
  public static byte[] convertObjectToByteArray(Object obj) {
    try {
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      ObjectOutputStream oos = new ObjectOutputStream(bos);
      oos.writeObject(obj);
      oos.flush();
      byte[] bytes = bos.toByteArray();
      oos.close();
      return bytes;
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  public static Object convertByteArrayToObject(byte[] bytes) throws StreamCorruptedException {
    try {
      ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
      ObjectInputStream ois = new ObjectInputStream(bis);
      Object obj = ois.readObject();
      ois.close();
      return obj;
    } catch (Exception e) {
      throw new StreamCorruptedException();
    }
  }
}
