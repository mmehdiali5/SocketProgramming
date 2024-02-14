import java.io.IOException;
import java.io.StreamCorruptedException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

public class Server {
  static HashMap<String, String> store = new HashMap<>();

  // A function to get current time in yyyy-MM-dd HH:mm:ss:SSS in a String to be used in log
  private static String getCurrentTime() {
    LocalDateTime currentDateTime = LocalDateTime.now();

    // Create a DateTimeFormatter to format the output (optional)
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss:SSS");

    // Format and print the current date and time
    return currentDateTime.format(formatter);
  }


  public static void main(String[] args) throws IOException {
    try {
      //Listen for client at this port
      //int PORT = 32000;
      int PORT = Integer.parseInt(args[0]);
      DatagramSocket ds = new DatagramSocket(PORT);
      System.out.println("Server is listening on port " + PORT);
      byte[] b1 = new byte[1024];
      DatagramPacket dp = new DatagramPacket(b1, b1.length);

      //Server keep receiving datagrams
      while (true) {
        try {
          //Wait for client to make connection
          ds.receive(dp);

          //Get data from the datagram and convert from byte array to Object
          Object obj = SerializerDeserializer.convertByteArrayToObject(dp.getData());

          // Check which kind of request is received by checking the instance of object i.e. PUT, GET, DELETE

          if (obj instanceof PUT) {
            Response res = handlePutRequest(obj, dp);
            System.out.println(getCurrentTime() + " PUT REQUEST RequestID: " + ((PUT) obj).id + " FROM " + dp.getAddress() + ":" +
                    dp.getPort() + ", " + res);
            sendResponse(res, dp, ds);
          } else if (obj instanceof GET) {
            Response res = handleGetRequest(obj, dp);
            System.out.println(getCurrentTime() + " GET REQUEST RequestID: " + ((GET) obj).id + " FROM " + dp.getAddress() + ":" +
                    dp.getPort() + ", " + res);
            sendResponse(res, dp, ds);
          } else if (obj instanceof DELETE) {
            Response res = handleDeleteRequest(obj, dp);
            System.out.println(getCurrentTime() + " DELETE REQUEST RequestID: " + ((DELETE) obj).id + " FROM " + dp.getAddress() + ":" +
                    dp.getPort() + ", " + res);
            sendResponse(res, dp, ds);
          } else {
            throw new ClassNotFoundException();
          }
          // If casting to GET, DELETE, or PUT is not possible or stream got corrupted then
          // A message is shown that indicates the reception of a malformed request.
        } catch (ClassNotFoundException | ClassCastException | StreamCorruptedException e) {
          System.err.println(getCurrentTime() + " Malformed Data Packet Received. Not a valid" +
                  " PUT, GET, DELETE OR CLOSE Request from " + dp.getAddress() + ":" +
                  dp.getPort());
        } catch (SocketException e) {
          System.err.println(getCurrentTime() + " Socket Exception Occurred");
        }
      }

    } catch (IOException e) {
      e.printStackTrace();
    }

  }


  // Send response after converting the Response object(containing the status code and message) to the
  // address received in the datagram packet.
  private static void sendResponse(Response res, DatagramPacket dp, DatagramSocket ds) throws IOException {
    byte[] b2 = SerializerDeserializer.convertObjectToByteArray(res);
    DatagramPacket dp1 = new DatagramPacket(b2, b2.length, dp.getAddress(), dp.getPort());
    ds.send(dp1);
  }

  //If the received object is of type DELETE then we have to process a delete request.
  //We get the key from the Request. If the key is present we delete the key from the map
  // and send a success code 200 along with success message. If the key is not present an error
  // message is sent.
  private static Response handleDeleteRequest(Object receivedObject, DatagramPacket dp) {
    System.out.println(getCurrentTime() + " DELETE REQUEST FROM " + dp.getAddress() + ":" +
            dp.getPort());
    DELETE req = (DELETE) receivedObject;
    if (!store.containsKey(req.key)) {
      return new Response("404", "Key not found", req.id);
    }
    store.remove(req.key);
    return new Response("200", "DELETE Request successful", req.id);
  }

  //If the received object is of type PUT then we have to process a PUT request.
  // We get the key and value from the Request. If the key is present we overwrite the key value from the map
  //and send a success code 200 along with success message. If the key is not present a new key is made
  //with specified value.
  private static Response handlePutRequest(Object receivedObject, DatagramPacket dp) {
    System.out.println(getCurrentTime() + " PUT REQUEST RequestId:" + ((PUT) receivedObject).id + " FROM " + dp.getAddress() + ":" +
            dp.getPort());
    PUT req = (PUT) receivedObject;
    store.put(req.key, req.value);
    return new Response("200", "PUT Request successful", req.id);
  }

  // If the received object is of type GET then we have to process a get request.
  // We get the key from the Request. If the key is present we return the key value from the map
  // and send a success code 200 along with success message and key value. If the key is not present an error
  // message is sent.
  private static Response handleGetRequest(Object receivedObject, DatagramPacket dp) {
    System.out.println(getCurrentTime() + " GET REQUEST FROM " + dp.getAddress() + ":" +
            dp.getPort());
    GET req = (GET) receivedObject;
    if (!store.containsKey(req.key)) {
      return new Response("404", "Key not found", req.id);
    }
    store.get(req.key);
    return new Response("200", store.get(req.key), req.id);
  }
}