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

  private static String getCurrentTime() {
    LocalDateTime currentDateTime = LocalDateTime.now();

    // Create a DateTimeFormatter to format the output (optional)
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss:SSS");

    // Format and print the current date and time
    String formattedDateTime = currentDateTime.format(formatter);
    return formattedDateTime;
  }

  public static void main(String[] args) {
    //int PORT = 32000;
    int PORT=Integer.parseInt(args[0]);
    try {
      //Listen for client at this port
      ServerSocket serverSocket = new ServerSocket(PORT);
      System.out.println("Server is listening on port " + PORT);

      while (true) {
        Socket clientSocket = null;
        try {
          //Wait for client to make connection
          clientSocket = serverSocket.accept();
          ObjectOutputStream objectOutputStream = new ObjectOutputStream(clientSocket.getOutputStream());
          ObjectInputStream objectInputStream = new ObjectInputStream(clientSocket.getInputStream());
          handleRequest(clientSocket, objectOutputStream, objectInputStream);
        } catch (SocketException e) {
          System.err.println(getCurrentTime() + " Socket Exception Occurred");
          clientSocket.close();
        } catch (ClassNotFoundException | ClassCastException | StreamCorruptedException e) {
          System.err.println(getCurrentTime() + " Malformed Data Packet Received. Not a valid" +
                  " PUT, GET, DELETE OR CLOSE Request from " + clientSocket.getInetAddress() + ":" +
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

  private static Response handlePutRequest(Socket clientSocket, Object receivedObject) {
    System.out.println(getCurrentTime() + " PUT REQUEST RequestId:" + ((PUT) receivedObject).id + " FROM " + clientSocket.getInetAddress() + ":" +
            clientSocket.getPort());
    PUT req = (PUT) receivedObject;
    store.put(req.key, req.value);
    return new Response("200", "PUT Request successful", req.id);
  }


}
