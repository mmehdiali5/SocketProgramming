import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Scanner;
import java.util.UUID;


public class Client {

//  private static class PUT2 implements Serializable {
//    public String key;
//    public String value;

//    public PUT2(String key, String value) {
//      this.key=key;
//      this.value=value;
//    }
//  }

  private static String getCurrentTime() {
    LocalDateTime currentDateTime = LocalDateTime.now();

    // Create a DateTimeFormatter to format the output (optional)
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss:SSS");

    // Format and print the current date and time
    return currentDateTime.format(formatter);
  }

  private static String generateUniqueId() {
    // Using a combination of timestamp and random component
    long timestamp = System.currentTimeMillis();
    UUID uuid = UUID.randomUUID();

    // Concatenate timestamp and random UUID

    return timestamp + "-" + uuid.toString();
  }

  private static void sendPUTRequest(String key, String value, ObjectInputStream objectInputStream,
                                     ObjectOutputStream objectOutputStream) throws IOException {
    PUT req = new PUT(key, value, generateUniqueId());
    objectOutputStream.writeObject(req);
    System.out.println(getCurrentTime() + " PUT Request ID: " + req.id + " sent to Server with Key = " + key + ", Value = " + value);
    getResponse(objectInputStream);
  }


  public static byte[] objectToByteArray(Object obj) {
    ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
    try (ObjectOutputStream objOut = new ObjectOutputStream(byteOut)) {
      objOut.writeObject(obj);
      objOut.flush();
      return byteOut.toByteArray();
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  private static void sendMalformedPUTRequest(String key, String value, Socket socket) throws IOException {
    PUT req = new PUT(key, value, generateUniqueId());
    byte[] byteArray = objectToByteArray(req);
    byte[] byteArray2 = Arrays.copyOfRange(byteArray, 0, byteArray.length / 2);
    OutputStream outputStream = socket.getOutputStream();
    outputStream.write(byteArray2);
    outputStream.flush();
  }

  private static void sendGETRequest(String key, ObjectInputStream objectInputStream,
                                     ObjectOutputStream objectOutputStream) throws IOException {
    GET req = new GET(key, generateUniqueId());
    objectOutputStream.writeObject(req);
    System.out.println(getCurrentTime() + " GET Request ID: " + req.id + " sent to Server with Key = " + key);
    getResponse(objectInputStream);
  }

  private static void sendDeleteRequest(String key, ObjectInputStream objectInputStream, ObjectOutputStream objectOutputStream)
          throws IOException {
    DELETE req = new DELETE(key, generateUniqueId());
    objectOutputStream.writeObject(req);
    System.out.println(getCurrentTime() + " DELETE Request ID: " + req.id + " sent to Server with Key = " + key);
    getResponse(objectInputStream);
  }

  private static void sendCloseRequest(ObjectInputStream objectInputStream, ObjectOutputStream objectOutputStream)
          throws IOException {
    CLOSE req = new CLOSE(generateUniqueId());
    objectOutputStream.writeObject(req);
    System.out.println(getCurrentTime() + " CLOSE Request ID: " + req.id + " sent to Server");
    getResponse(objectInputStream);
  }

  private static void getResponse(ObjectInputStream objectInputStream) throws IOException {
    try {
      Object receivedObject = objectInputStream.readObject();
      Response res = (Response) receivedObject;
      System.out.println(getCurrentTime() + " Request ID: " + res.getReqId() + " Status: " + res.getStatus() + ", Message: " + res.getMessage());
    } catch (SocketTimeoutException e) {
      // Handle timeout: log the issue and continue with other requests
      System.err.println(getCurrentTime() + " Timeout while waiting for server response. Continuing with other requests.");
    } catch (ClassNotFoundException | ClassCastException e) {
      System.err.println(getCurrentTime() + " Received unsolicited response acknowledging" +
              " unknown PUT/GET/DELETE");
    }
  }


  private static void runUI(ObjectInputStream objectInputStream,
                            ObjectOutputStream objectOutputStream) throws IOException {
    boolean stop = false;
    while (!stop) {
      System.out.println();
      System.out.println("Choose From Following Options:\n1) PUT\n2) GET\n3) DELETE\n4) CLOSE CONNECTION\n");
      Scanner scanner = new Scanner(System.in);

      // Read the user's input as a String
      String option = scanner.nextLine();
      String key = "";
      String value = "";
      switch (option) {
        case "1":
          System.out.print("Enter Key: ");
          key = scanner.nextLine();
          System.out.print("Enter Value: ");
          value = scanner.nextLine();
          sendPUTRequest(key, value, objectInputStream, objectOutputStream);
          break;
        case "2":
          System.out.print("Enter Key: ");
          key = scanner.nextLine();
          sendGETRequest(key, objectInputStream, objectOutputStream);
          break;
        case "3":
          System.out.print("Enter Key: ");
          key = scanner.nextLine();
          sendDeleteRequest(key, objectInputStream, objectOutputStream);
          break;
        case "4":
          sendCloseRequest(objectInputStream, objectOutputStream);
          System.out.println("Connection Closed");
          stop = true;
          break;
        default:
          System.out.println("Please enter valid Input");
          break;
      }
    }
  }

  public static void main(String[] args) {
//    String serverAddress = "localhost";
//    int PORT = 32000;
    String serverAddress = args[0];
    int PORT = Integer.parseInt(args[1]);

    int TIMEOUT = 5000;
    try (Socket socket = new Socket(serverAddress, PORT)) {
      socket.setSoTimeout(TIMEOUT);
      ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
      ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
//      sendMalformedPUTRequest("key1","value1",socket);
//      while (true){}
      //Pre-Populate
      sendPUTRequest("key1", "value1", objectInputStream, objectOutputStream);
      sendPUTRequest("key2", "val2", objectInputStream, objectOutputStream);
      sendPUTRequest("key3", "value3", objectInputStream, objectOutputStream);


      sendPUTRequest("key4", "val4", objectInputStream, objectOutputStream);
      sendGETRequest("key4", objectInputStream, objectOutputStream);
      sendDeleteRequest("key4", objectInputStream, objectOutputStream);

      //Valid Delete
      sendDeleteRequest("key1", objectInputStream, objectOutputStream);
      //Invalid GET
      sendGETRequest("key1", objectInputStream, objectOutputStream);


      sendPUTRequest("key5", "val5", objectInputStream, objectOutputStream);
      sendGETRequest("key5", objectInputStream, objectOutputStream);
      //Update value and get new value
      sendPUTRequest("key5", "val6", objectInputStream, objectOutputStream);
      sendGETRequest("key5", objectInputStream, objectOutputStream);

      sendPUTRequest("key7", "val7", objectInputStream, objectOutputStream);
      sendPUTRequest("key8", "val8", objectInputStream, objectOutputStream);
      sendDeleteRequest("key8", objectInputStream, objectOutputStream);
      //Invalid GET
      sendGETRequest("key8", objectInputStream, objectOutputStream);


      sendDeleteRequest("key7", objectInputStream, objectOutputStream);
      //Invalid GET
      sendGETRequest("key7", objectInputStream, objectOutputStream);


      //Invalid Delete
      sendDeleteRequest("INVALID", objectInputStream, objectOutputStream);


      runUI(objectInputStream, objectOutputStream);


    } catch (IOException e) {
      e.printStackTrace();
    }
  }


}
