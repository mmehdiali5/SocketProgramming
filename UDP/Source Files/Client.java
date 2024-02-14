import java.io.IOException;
import java.io.StreamCorruptedException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Scanner;
import java.util.UUID;


public class Client {

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

  // Receive the Response from the server and log the response.
  private static void getResponse(DatagramSocket socket) throws IOException {
    try {
      byte[] b1 = new byte[1024];
      DatagramPacket dp = new DatagramPacket(b1, b1.length);
      socket.receive(dp);
      //Deserialize the object from byte arrays to object
      Object obj = SerializerDeserializer.convertByteArrayToObject(dp.getData());
      Response res = (Response) obj;
      System.out.println(getCurrentTime() + " Request ID: " + res.getReqId() + " Status: " + res.getStatus() + ", Message: " + res.getMessage());
    } catch (SocketTimeoutException e) {
      // Handle timeout: log the issue and continue with other requests
      System.err.println(getCurrentTime() + " Timeout while waiting for server response. Continuing with other requests.");
    }//If it is not possible to convert to Response object or response is corrupted, then Display and error message
    //indicating an unsolicited response is received
    catch (ClassCastException | StreamCorruptedException e) {
      System.err.println(getCurrentTime() + " Received unsolicited response acknowledging" +
              " unknown PUT/GET/DELETE");
    }
  }


  //A function to send a put request
  private static void sendPUTRequest(String key, String value, DatagramSocket socket,
                                     InetAddress address, int PORT) throws IOException {
    PUT req = new PUT(key, value, generateUniqueId());
    //Serialize the PUT object to byte arrays
    byte[] b = SerializerDeserializer.convertObjectToByteArray(req);
    DatagramPacket dp = new DatagramPacket(b, b.length, address, PORT);
    socket.send(dp);
    System.out.println(getCurrentTime() + " PUT Request ID: " + req.id + " sent to Server with Key = " + key + ", Value = " + value);
    getResponse(socket);
  }

  //A function to send a GET request
  private static void sendGETRequest(String key, DatagramSocket socket,
                                     InetAddress address, int PORT) throws IOException {
    GET req = new GET(key, generateUniqueId());
    //Serialize the GET object to byte arrays
    byte[] b = SerializerDeserializer.convertObjectToByteArray(req);
    DatagramPacket dp = new DatagramPacket(b, b.length, address, PORT);
    socket.send(dp);
    System.out.println(getCurrentTime() + " GET Request ID: " + req.id + " sent to Server with Key = " + key);
    getResponse(socket);
  }

  //A function to send a DELETE request
  private static void sendDeleteRequest(String key, DatagramSocket socket,
                                        InetAddress address, int PORT) throws IOException {
    DELETE req = new DELETE(key, generateUniqueId());
    byte[] b = SerializerDeserializer.convertObjectToByteArray(req);
    DatagramPacket dp = new DatagramPacket(b, b.length, address, PORT);
    socket.send(dp);
    System.out.println(getCurrentTime() + " DELETE Request ID: " + req.id + " sent to Server with Key = " + key);
    getResponse(socket);
  }


  //Function to mimic a malformed request sent to sever
  private static void sendMalformedPUTRequest(String key, String value, InetAddress address, int PORT,
                                              DatagramSocket socket) throws IOException {
    PUT req = new PUT(key, value, generateUniqueId());
    byte[] byteArray = SerializerDeserializer.convertObjectToByteArray(req);
    byte[] byteArray2 = Arrays.copyOfRange(byteArray, 0, byteArray.length / 2);
    DatagramPacket dp = new DatagramPacket(byteArray2, byteArray2.length, address, PORT);
    socket.send(dp);
    System.out.println(getCurrentTime() + " DELETE Request ID: " + req.id + " sent to Server with Key = " + key);
    getResponse(socket);
  }

  //User Interface code
  private static void runUI(DatagramSocket socket, InetAddress address, int PORT) throws IOException {
    boolean stop = false;
    while (!stop) {
      System.out.println();
      System.out.println("Choose From Following Options:\n1) PUT\n2) GET\n3) DELETE\n4) Stop Client\n");
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
          sendPUTRequest(key, value, socket, address, PORT);
          break;
        case "2":
          System.out.print("Enter Key: ");
          key = scanner.nextLine();
          sendGETRequest(key, socket, address, PORT);
          break;
        case "3":
          System.out.print("Enter Key: ");
          key = scanner.nextLine();
          sendDeleteRequest(key, socket, address, PORT);
          break;
        case "4":
          System.out.println("Client Stopped");
          stop = true;
          break;
        default:
          System.out.println("Please enter valid Input");
          break;
      }
    }
  }


  public static void main(String args[]) throws IOException {
    int TIMEOUT = 5000;

    //InetAddress address = InetAddress.getLocalHost();
    InetAddress address= InetAddress.getByName(args[0]);

    //int PORT = 32000;
    int PORT = Integer.parseInt(args[1]);

    int CLIENT_SOCKET = -1;
    if (args.length < 3) {
      CLIENT_SOCKET = 52000;
    } else {
      CLIENT_SOCKET = Integer.parseInt(args[2]);
    }

    try (DatagramSocket socket = new DatagramSocket(CLIENT_SOCKET)) {
      //Timeout limit. If no response is sent by server in this time than a timeout occurs and client
      //continues with other requests
      socket.setSoTimeout(TIMEOUT);

      //To send Malformed PUT request uncomment the following code to mimic malformed packet transfer

      /*sendMalformedPUTRequest("key1", "value1", address, PORT, socket);
      while (true){}*/

      //Pre-Populate the data 5 PUTs, GETs and DELETEs
      sendPUTRequest("key1", "value1", socket, address, PORT);
      sendPUTRequest("key2", "val2", socket, address, PORT);
      sendPUTRequest("key3", "value3", socket, address, PORT);

      sendPUTRequest("key4", "val4", socket, address, PORT);
      sendGETRequest("key4", socket, address, PORT);
      sendDeleteRequest("key4", socket, address, PORT);

      //Valid Delete
      sendDeleteRequest("key1", socket, address, PORT);
      //Invalid GET
      sendGETRequest("key1", socket, address, PORT);

      sendPUTRequest("key5", "val5", socket, address, PORT);
      sendGETRequest("key5", socket, address, PORT);
      //Update value and get new value
      sendPUTRequest("key5", "val6", socket, address, PORT);
      sendGETRequest("key5", socket, address, PORT);

      sendPUTRequest("key7", "val7", socket, address, PORT);
      sendPUTRequest("key8", "val8", socket, address, PORT);
      sendDeleteRequest("key8", socket, address, PORT);
      //Invalid GET
      sendGETRequest("key8", socket, address, PORT);

      sendDeleteRequest("key7", socket, address, PORT);
      //Invalid GET
      sendGETRequest("key7", socket, address, PORT);

      //Invalid Delete
      sendDeleteRequest("INVALID", socket, address, PORT);


      //Run the Interactive client code
      runUI(socket, address, PORT);


    } catch (IOException e) {
      e.printStackTrace();
    }

  }


}
