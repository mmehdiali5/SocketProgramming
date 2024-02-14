import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Scanner;
import java.util.UUID;


public class Client {

  private static Socket socket;
  private static ObjectOutputStream objectOutputStream;
  private static ObjectInputStream objectInputStream;
//  private static class PUT2 implements Serializable {
//    public String key;
//    public String value;

//    public PUT2(String key, String value) {
//      this.key=key;
//      this.value=value;
//    }
//  }

  // A function to get current time in yyyy-MM-dd HH:mm:ss:SSS in a String to be used in log
  private static String getCurrentTime() {
    LocalDateTime currentDateTime = LocalDateTime.now();

    // Create a DateTimeFormatter to format the output (optional)
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss:SSS");

    // Format and print the current date and time
    return currentDateTime.format(formatter);
  }


  // A function to generate a unique Request ID using current timestamp and random UUID
  private static String generateUniqueId() {
    // Using a combination of timestamp and random component
    long timestamp = System.currentTimeMillis();
    UUID uuid = UUID.randomUUID();

    // Concatenate timestamp and random UUID

    return timestamp + "-" + uuid.toString();
  }

  //A function to send a put request
  private static void sendPUTRequest(String key, String value) throws IOException {
    PUT req = new PUT(key, value, generateUniqueId());
    objectOutputStream.writeObject(req);
    //sendMalformedPUTRequest(key,value);
    System.out.println(getCurrentTime() + " PUT Request ID: " + req.id + " sent to Server with Key = " + key + ", Value = " + value);
    getResponse();
  }


  //Serialize the object from object to byte array
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

  //Function to mimic a malformed request sent to sever
  private static void sendMalformedPUTRequest(String key, String value) throws IOException {
    PUT req = new PUT(key, value, generateUniqueId());
    byte[] byteArray = objectToByteArray(req);
    byte[] byteArray2 = Arrays.copyOfRange(byteArray, 0, byteArray.length / 2);
    OutputStream outputStream = socket.getOutputStream();
    outputStream.write(byteArray2);
    outputStream.flush();
  }

  //A function to send a GET request
  private static void sendGETRequest(String key) throws IOException {
    GET req = new GET(key, generateUniqueId());
    objectOutputStream.writeObject(req);
    System.out.println(getCurrentTime() + " GET Request ID: " + req.id + " sent to Server with Key = " + key);
    getResponse();
  }

  //A function to send a DELETE request
  private static void sendDeleteRequest(String key)
          throws IOException {
    DELETE req = new DELETE(key, generateUniqueId());
    objectOutputStream.writeObject(req);
    System.out.println(getCurrentTime() + " DELETE Request ID: " + req.id + " sent to Server with Key = " + key);
    getResponse();
  }

  //A function to send a connection close request to the server
  private static void sendCloseRequest()
          throws IOException {
    CLOSE req = new CLOSE(generateUniqueId());
    objectOutputStream.writeObject(req);
    System.out.println(getCurrentTime() + " CLOSE Request ID: " + req.id + " sent to Server");
    getResponse();
  }


  // Receive the Response from the server and log the response.
  private static void getResponse() throws IOException {
    try {
      Object receivedObject = objectInputStream.readObject();
      Response res = (Response) receivedObject;
      System.out.println(getCurrentTime() + " Request ID: " + res.getReqId() + " Status: " + res.getStatus() + ", Message: " + res.getMessage());
    } catch (SocketTimeoutException e) {
      // Handle timeout: log the issue and continue with other requests
      System.err.println(getCurrentTime() + " Timeout while waiting for server response. Continuing with other requests.");
    } catch (ClassNotFoundException | ClassCastException |
             IOException e) {
      System.err.println(getCurrentTime() + " Received unsolicited response acknowledging" +
              " unknown PUT/GET/DELETE");
      InetAddress address = socket.getInetAddress();

      //In case of corrupt stream connection has to be reset.
      int port = socket.getPort();
      socket.close();
      socket = new Socket(address, port);
      objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
      objectInputStream = new ObjectInputStream(socket.getInputStream());
    }
  }


  private static void runUI() throws IOException {
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
          sendPUTRequest(key, value);
          break;
        case "2":
          System.out.print("Enter Key: ");
          key = scanner.nextLine();
          sendGETRequest(key);
          break;
        case "3":
          System.out.print("Enter Key: ");
          key = scanner.nextLine();
          sendDeleteRequest(key);
          break;
        case "4":
          sendCloseRequest();
          System.out.println("Connection Closed");
          stop = true;
          break;
        default:
          System.out.println("Please enter valid Input");
          break;
      }

    }
  }

  public static void main(String[] args) throws IOException {
    //String serverAddress = "localhost";
    //int PORT = 32000;
    String serverAddress = args[0];
    int PORT = Integer.parseInt(args[1]);

    int TIMEOUT = 5000;
    socket = new Socket(serverAddress, PORT);
    try {
      //Timeout limit. If no response is sent by server in this time than a timeout occurs and client
      //continues with other requests
      socket.setSoTimeout(TIMEOUT);
      objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
      objectInputStream = new ObjectInputStream(socket.getInputStream());

      //Uncomment following to send a malformed request. Note that sending a malformed request
      //results in connection lost from the server as stream gets corrupted.
      /*sendMalformedPUTRequest("key1","value1",socket);
      while (true){}*/

      //Pre-Populate
      sendPUTRequest("key1", "value1");
      sendPUTRequest("key2", "val2");
      sendPUTRequest("key3", "value3");


      sendPUTRequest("key4", "val4");
      sendGETRequest("key4");
      sendDeleteRequest("key4");

      //Valid Delete
      sendDeleteRequest("key1");
      //Invalid GET
      sendGETRequest("key1");


      sendPUTRequest("key5", "val5");
      sendGETRequest("key5");
      //Update value and get new value
      sendPUTRequest("key5", "val6");
      sendGETRequest("key5");

      sendPUTRequest("key7", "val7");
      sendPUTRequest("key8", "val8");
      sendDeleteRequest("key8");
      //Invalid GET
      sendGETRequest("key8");


      sendDeleteRequest("key7");
      //Invalid GET
      sendGETRequest("key7");


      //Invalid Delete
      sendDeleteRequest("INVALID");


      //Run the Interactive client code
      runUI();


    } catch (IOException e) {
      System.err.println("Server Disconnected");
    }
  }


}
