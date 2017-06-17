package com.example.pc.silentmusicparty;

import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class DJActivity extends AppCompatActivity {
    private static final int REQUEST_LOGIN = 0;
    private Button send;
    private Socket mSocket;
    private String mUsername="Server";
    private Boolean isConnected = true;

    private List<Message> mMessages = new ArrayList<Message>();
    private MyServer server;
    Button stop, reset;
    TextView ip;
    String message;
    Boolean isPlaying=false;
    ImageButton playlist, play;
    private SongsManager songManager;
    MediaPlayer mp;
    private ArrayList<HashMap<String, String>> songsList = new ArrayList<HashMap<String, String>>();
    private int currentSongIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dj);
        stop=(Button) findViewById(R.id.song1);
        play=(ImageButton) findViewById(R.id.song2);
        reset=(Button) findViewById(R.id.reset);
        playlist=(ImageButton) findViewById(R.id.playlist);
        // Mediaplayer
        mp = new MediaPlayer();
        songManager = new SongsManager();
        // Getting all songs list
        songsList = songManager.getPlayList();
        // By default play first song
        //playSong(0);
        ip=(TextView)findViewById(R.id.ip);
        ip.setText(getLocalIpAddress());
        ip.setEnabled(false);
/** Messages part*/
        ChatApplication app = (ChatApplication) getApplication();
        mSocket = app.getSocket();
        mSocket.on(Socket.EVENT_CONNECT,onConnect);
        mSocket.on(Socket.EVENT_DISCONNECT,onDisconnect);
        mSocket.on(Socket.EVENT_CONNECT_ERROR, onConnectError);
        mSocket.on(Socket.EVENT_CONNECT_TIMEOUT, onConnectError);
        mSocket.on("new message", onNewMessage);
        //  mSocket.on("user joined", onUserJoined);
        //  mSocket.on("user left", onUserLeft);
        //  mSocket.on("typing", onTyping);
        //  mSocket.on("stop typing", onStopTyping);
        mSocket.connect();
        attemptLogin();
        mSocket.on("login", onLogin);
        //playSong(0);
      //  startSignIn();
        currentSongIndex=0;
/** Messages part*/

        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                play.setImageResource(R.drawable.play);
                attemptSend("change");
            }
        });

        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isPlaying==false) {
                    //play.setText("Pause");
                    play.setImageResource(R.drawable.pause);
                    //playSong(0);
                    mp.start();
                    //server.fileloc = Environment.getExternalStorageDirectory() + "/test2.mp3";
                    isPlaying=true;
                    attemptSend("play");
                } else if (isPlaying==true){
                    //play.setText("Play");
                    play.setImageResource(R.drawable.play);
                    mp.pause();
                    attemptSend("pause");
                    isPlaying=false;
                }
            }
        });
        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptSend("reset");
            }
        });
        playlist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getApplicationContext(), PlayListActivity.class);
                startActivityForResult(i, 100);
            }
        });
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        try {
            server = new MyServer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

   /* @Override
    public void onPause() {
        super.onPause();
        if(server != null) {
            server.stop();
        }
    } */
    @Override
    public void onBackPressed() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(
                DJActivity.this);

        alertDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                attemptSend("end");
                finish();

            }
        });

        alertDialog.setNegativeButton("No", null);

        alertDialog.setMessage("Connection will terminate. Do you want to stop the partying!?");
        alertDialog.setTitle("Attention!");
        alertDialog.show();
    }
    private void doExit() {


    }

    /**Get host IP address*/
    public String getLocalIpAddress() {
        try {
            Enumeration<NetworkInterface> enumNetworkInterfaces = NetworkInterface
                    .getNetworkInterfaces();
            while (enumNetworkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = enumNetworkInterfaces
                        .nextElement();
                Enumeration<InetAddress> enumInetAddress = networkInterface
                        .getInetAddresses();
                while (enumInetAddress.hasMoreElements()) {
                    InetAddress inetAddress = enumInetAddress.nextElement();

                    if (inetAddress.isSiteLocalAddress()) {
                        return inetAddress.getHostAddress();
                    }

                }

            }

        }
        catch (SocketException e) { e.printStackTrace(); }
        return null;
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSocket.off("login", onLogin);
        mSocket.disconnect();
        mSocket.off(Socket.EVENT_CONNECT, onConnect);
        mSocket.off(Socket.EVENT_DISCONNECT, onDisconnect);
        mSocket.off(Socket.EVENT_CONNECT_ERROR, onConnectError);
        mSocket.off(Socket.EVENT_CONNECT_TIMEOUT, onConnectError);
        mSocket.off("new message", onNewMessage);
        mp.release();
        mp.stop();
    }

    /**Messages part*/
    private void addMessage(String username, String message) {
        mMessages.add(new Message.Builder(Message.TYPE_MESSAGE)
                .username(username).message(message).build());
    }


    private void attemptSend(String message) {
        // if (null == mUsername) return;
        //if (!mSocket.connected()) return;

        //String message = "change";      /*change*/        //mInputMessageView.getText().toString().trim();
        if(message=="play")
        {
            Toast.makeText(getApplicationContext(),"playing!", Toast.LENGTH_SHORT).show();
        }
        if(message=="pause")
        {
            Toast.makeText(getApplicationContext(),"paused!", Toast.LENGTH_SHORT).show();
        }
        addMessage(mUsername, message);

        // perform the sending message attempt.
        mSocket.emit("new message", message);
    }
    private void attemptSync(String message) {
        // if (null == mUsername) return;
        //if (!mSocket.connected()) return;

        //String message = "change";      /*change*/        //mInputMessageView.getText().toString().trim();

        addMessage(mUsername, message);

        // perform the sending message attempt.
        mSocket.emit("new message", message);
    }

    private void startSignIn() {
        mUsername = null;
        /**Login*/
        //attemptLogin();
        //mSocket.on("login", onLogin);
        /**Login*/
        Intent intent = new Intent(this, LoginActivity.class);
        startActivityForResult(intent, REQUEST_LOGIN);
    }
    private void leave() {
        mUsername = null;
        mSocket.disconnect();
        mSocket.connect();
        startSignIn();
    }


    private Emitter.Listener onConnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(!isConnected) {
                        if(null!=mUsername)
                            mSocket.emit("add user", mUsername);
                        Toast.makeText(getApplicationContext(),
                                "Connected", Toast.LENGTH_LONG).show();
                        isConnected = true;
                    }
                }
            });
        }
    };

    private Emitter.Listener onDisconnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    isConnected = false;
                    Toast.makeText(getApplicationContext(),
                            "Disconnected", Toast.LENGTH_LONG).show();
                }
            });
        }
    };

    private Emitter.Listener onConnectError = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(),
                            "Disconnected, Please check Internet", Toast.LENGTH_LONG).show();
                }
            });
        }
    };

    private Emitter.Listener onNewMessage = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    String username;
                    String message;
                    try {
                        username = data.getString("username");
                        message = data.getString("message");
                    } catch (JSONException e) {
                        return;
                    }

                    //removeTyping(username);
                    Toast.makeText(getApplicationContext(),"Success!", Toast.LENGTH_SHORT).show();
                    addMessage(username, message);
                }
            });
        }
    };
    /** Messages part*/
    /**Login*/
    private void attemptLogin() {

        String username = "Server";//mUsernameView.getText().toString().trim();

        mUsername = username;

        // perform the user login attempt.
        mSocket.emit("add user", username);
    }

    private Emitter.Listener onLogin = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            JSONObject data = (JSONObject) args[0];

            int numUsers;
            try {
                numUsers = data.getInt("numUsers");
            } catch (JSONException e) {
                return;
            }

            Intent intent = new Intent();
            intent.putExtra("username", mUsername);
            intent.putExtra("numUsers", numUsers);
            setResult(RESULT_OK, intent);
            //finish();        /** caused the activity to end, hence been commented out*/
        }
    };
    /**Login*/
    public void  playSong(int songIndex){
        // Play song
        server.fileloc=songsList.get(songIndex).get("songPath");
        try {
            mp.reset();
            mp.setDataSource(songsList.get(songIndex).get("songPath"));
            mp.prepare();
            mp.start();
            // Displaying Song title
            String songTitle = songsList.get(songIndex).get("songTitle");
            //songTitleLabel.setText(songTitle);

            // Changing Button Image to pause image
            //btnPlay.setImageResource(R.drawable.btn_pause);

            // set Progress bar values
            //songProgressBar.setProgress(0);
            //songProgressBar.setMax(100);

            // Updating progress bar
            //updateProgressBar();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**
     * Receiving song index from playlist view
     * and play the song
     * */
    @Override
    protected void onActivityResult(int requestCode,
                                    int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == 100){
            play.setImageResource(R.drawable.pause);
            isPlaying=true;
            currentSongIndex = data.getExtras().getInt("songIndex");
            server.fileloc=songsList.get(currentSongIndex).get("songPath");
            Toast.makeText(getApplicationContext(),server.fileloc, Toast.LENGTH_SHORT).show();
            // play selected song
            playSong(currentSongIndex);
            attemptSend("change");
        }

    }
}
