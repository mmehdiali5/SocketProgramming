import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

public class Server {

//  static class  Response2 implements Serializable {
//    private String status;
//    private String message;
//
//    private String reqId;
//
//    public Response2(String status, String message, String id) {
//      this.status = status;
//      this.message = message;
//      this.reqId = id;
//    }
//  }

  static HashMap<String, String> store = new HashMap<>();

  // A function to get current time in yyyy-MM-dd HH:mm:ss:SSS in a String to be used in log
  private static String getCurrentTime() {
    LocalDateTime currentDateTime = LocalDateTime.now();

    // Create a DateTimeFormatter to format the output (optional)
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss:SSS");

    // Format and print the current date and time
    return currentDateTime.format(formatter);
  }

  public static void main(String[] args) {
    //int PORT = 32000;
    int PORT=Integer.parseInt(args[0]);
    try {
      //Listen for client at this port
      ServerSocket serverSocket = new ServerSocket(PORT);
      System.out.println("Server is listening on port " + PORT);

      //Server keeps listening for incoming connection requests.
      while (true) {
        Socket clientSocket = null;
        try {
          //Wait for client to make connection
          clientSocket = serverSocket.accept();
          ObjectOutputStream objectOutputStream = new ObjectOutputStream(clientSocket.getOutputStream());
          ObjectInputStream objectInputStream = new ObjectInputStream(clientSocket.getInputStream());

          //After connection is established handle the client requests.
          handleRequest(clientSocket, objectOutputStream, objectInputStream);
        } catch (SocketException | EOFException e) {
          System.err.println(getCurrentTime() + " Client Disconnected");
          clientSocket.close();
        } catch (ClassNotFoundException | ClassCastException | StreamCorruptedException e) {
          System.err.println(getCurrentTime() + " Malformed Data Packet Received. Not a valid" +
                  " PUT, GET, DELETE OR CLOSE Request from. Closing the connection with the client" + clientSocket.getInetAddress() + ":" +
                  clientSocket.getPort());
        }
      }

    } catch (IOException e) {
      e.printStackTrace();
    }

  }


  private static void handleRequest(Socket clientSocket, ObjectOutputStream objectOutputStream,
                                    ObjectInputStream objectInputStream)
          throws IOException, ClassNotFoundException {
    while (true) {
      Object receivedObject = objectInputStream.readObject();

      if (receivedObject instanceof PUT) {
        Response response = handlePutRequest(clientSocket, receivedObject);
        System.out.println(getCurrentTime() + " PUT REQUEST RequestID: " + ((PUT) receivedObject).id + " FROM " + clientSocket.getInetAddress() + ":" +
                clientSocket.getPort() + ", " + response);
        objectOutputStream.writeObject(response);
      } else if (receivedObject instanceof GET) {
        Response response = handleGetRequest(clientSocket, receivedObject);
        System.out.println(getCurrentTime() + " GET REQUEST FROM " + clientSocket.getInetAddress() + ":" +
                clientSocket.getPort() + " " + response);
        objectOutputStream.writeObject(response);
      } else if (receivedObject instanceof DELETE) {
        Response response = handleDeleteRequest(clientSocket, receivedObject);
        System.out.println(getCurrentTime() + " DELETE REQUEST FROM " + clientSocket.getInetAddress() + ":" +
                clientSocket.getPort() + " " + response);
        objectOutputStream.writeObject(response);
      } else if (receivedObject instanceof CLOSE) {
        System.out.println(getCurrentTime() + " CLOSE Request received from " + clientSocket.getInetAddress() + ":" +
                clientSocket.getPort());
        objectOutputStream.writeObject(new Response("200", "CLOSE Request successful", ((CLOSE) receivedObject).id));
        clientSocket.close();
        break;
      } else {
        throw new ClassNotFoundException();
      }
    }
  }


  //Uncomment below code to send a malformed response.
  /*public static byte[] objectToByteArray(Object obj) {
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

  private static void sendMalformedResponse(Response response, Socket socket) throws IOException {
    Response res = new Response(response.getStatus(), response.getStatus(), response.getReqId());
    byte[] byteArray = objectToByteArray(res);
    byte[] byteArray2 = Arrays.copyOfRange(byteArray, 0, byteArray.length / 2);
    OutputStream outputStream = socket.getOutputStream();
    outputStream.write(byteArray2);
    outputStream.flush();
  }*/

  //If the received object is of type DELETE then we have to process a delete request.
  //We get the key from the Request. If the key is present we delete the key from the map
  // and send a success code 200 along with success message. If the key is not present an error
  // message is sent.
  private static Response handleDeleteRequest(Socket clientSocket, Object receivedObject) {
    System.out.println(getCurrentTime() + " DELETE REQUEST FROM " + clientSocket.getInetAddress() + ":" +
            clientSocket.getPort());
    DELETE req = (DELETE) receivedObject;
    if (!store.containsKey(req.key)) {
      return new Response("404", "Key not found", req.id);
    }
    store.remove(req.key);
    return new Response("200", "DELETE Request successful", req.id);
  }


  // If the received object is of type GET then we have to process a get request.
  // We get the key from the Request. If the key is present we return the key value from the map
  // and send a success code 200 along with success message and key value. If the key is not present an error
  // message is sent.
  private static Response handleGetRequest(Socket clientSocket, Object receivedObject) {
    System.out.println(getCurrentTime() + " GET REQUEST FROM " + clientSocket.getInetAddress() + ":" +
            clientSocket.getPort());
    GET req = (GET) receivedObject;
    if (!store.containsKey(req.key)) {
      return new Response("404", "Key not found", req.id);
    }
    store.get(req.key);
    return new Response("200", store.get(req.key), req.id);
  }

  //If the received object is of type PUT then we have to process a PUT request.
  // We get the key and value from the Request. If the key is present we overwrite the key value from the map
  //and send a success code 200 along with success message. If the key is not present a new key is made
  //with specified value.
  private static Response handlePutRequest(Socket clientSocket, Object receivedObject) {
    System.out.println(getCurrentTime() + " PUT REQUEST RequestId:" + ((PUT) receivedObject).id + " FROM " + clientSocket.getInetAddress() + ":" +
            clientSocket.getPort());
    PUT req = (PUT) receivedObject;
    store.put(req.key, req.value);
    return new Response("200", "PUT Request successful", req.id);
  }


}
