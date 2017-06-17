package com.example.pc.silentmusicparty;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

/**
 * Created by andrei on 7/30/15.
 */
public class MyServer extends NanoHTTPD {
    private final static int PORT = 8080;

    public String fileloc;

    public MyServer() throws IOException {
        super(PORT);
        start();
        System.out.println( "\nRunning! Point your browers to http://localhost:8080/ \n" );
    }

  /*  @Override
    public Response serve(IHTTPSession session) {
        String msg = "<html><body><h1>Hello server</h1>\n";
        msg += "<p>We serve " + session.getUri() + " !</p>";
        return newFixedLengthResponse( msg + "</body></html>\n" );
    }
    */

  @Override
  public Response serve(String uri, Method method,
                        Map<String, String> header, Map<String, String> parameters,
                        Map<String, String> files) {
      String answer = "";

      FileInputStream fis = null;
      try {
          fis = new FileInputStream(fileloc);  //Environment.getExternalStorageDirectory()+ "/test.mp3"
      } catch (FileNotFoundException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
      }
      return newChunkedResponse(Response.Status.OK, "audio/mpeg", fis);
      //return newFixedLengthResponse(Response.Status.OK, "text/plain", "Hello!");

  }
}
