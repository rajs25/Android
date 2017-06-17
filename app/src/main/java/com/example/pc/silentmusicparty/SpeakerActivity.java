package com.example.pc.silentmusicparty;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class SpeakerActivity extends AppCompatActivity {
    static  String AUDIO_PATH;
    private MediaPlayer mediaPlayer;
    private EditText ip;
    private  String IP;
    private int playbackPosition=0;
    Button startPlayerBtn, pausePlayerBtn,stopPlayerBtn, restartPlayerBtn, setip;
    private static final int REQUEST_LOGIN = 0;
    private Button send;
    private io.socket.client.Socket mSocket;
    private String mUsername="Client";
    private Boolean isConnected = true;

    private List<Message> mMessages = new ArrayList<Message>();
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speaker);
        setip=(Button) findViewById(R.id.setip);
        ip=(EditText) findViewById(R.id.serverip);
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

        //  startSignIn();
/** Messages part*/
        startPlayerBtn=(Button)findViewById(R.id.startPlayerBtn);
        pausePlayerBtn=(Button)findViewById(R.id.pausePlayerBtn);
        restartPlayerBtn=(Button) findViewById(R.id.restartPlayerBtn);
        stopPlayerBtn=(Button) findViewById(R.id.stopPlayerBtn);
        setip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IP=ip.getText().toString();
                Toast.makeText(getApplicationContext(),"http://"+IP+":8080/",Toast.LENGTH_SHORT).show();
                AUDIO_PATH="http://"+IP+":8080/";
            }
        });
        startPlayerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(),"Play",Toast.LENGTH_SHORT).show();
                mediaPlayer=null;
                try {
                    playAudio(AUDIO_PATH);
                    //    playLocalAudio();
                    //   playLocalAudio_UsingDescriptor();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        pausePlayerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(),"Pause",Toast.LENGTH_SHORT).show();
                if(mediaPlayer != null && mediaPlayer.isPlaying()) {
                    playbackPosition = mediaPlayer.getCurrentPosition();
                    mediaPlayer.pause();
                }
            }
        });
        restartPlayerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(),"Restart",Toast.LENGTH_SHORT).show();
                if(mediaPlayer != null && !mediaPlayer.isPlaying()) {
                    mediaPlayer.seekTo(playbackPosition);
                    mediaPlayer.start();
                }
            }
        });
        stopPlayerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(),"Stop",Toast.LENGTH_SHORT).show();
                if(mediaPlayer != null) {
                    mediaPlayer.stop();
                    playbackPosition = 0;
                }
            }
        });
    }


    private void playAudio(String url) throws Exception
    {
        killMediaPlayer();
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setDataSource(url);
        mediaPlayer.prepare();
        mediaPlayer.start();
    }


  /*  private void playLocalAudio() throws Exception
    {
        mediaPlayer = MediaPlayer.create(this, R.raw.music_file);
        mediaPlayer.start();
    }
    */

  /*  private void playLocalAudio_UsingDescriptor() throws Exception {

        AssetFileDescriptor fileDesc = getResources().openRawResourceFd(
                R.raw.music_file);
        if (fileDesc != null) {

            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(fileDesc.getFileDescriptor(), fileDesc
                    .getStartOffset(), fileDesc.getLength());

            fileDesc.close();

            mediaPlayer.prepare();
            mediaPlayer.start();
        }
    } */

    @Override
    protected void onDestroy() {
        super.onDestroy();
        killMediaPlayer();
        mSocket.off("login", onLogin);
        mSocket.off(Socket.EVENT_CONNECT, onConnect);
        mSocket.off(Socket.EVENT_DISCONNECT, onDisconnect);
        mSocket.off(Socket.EVENT_CONNECT_ERROR, onConnectError);
        mSocket.off(Socket.EVENT_CONNECT_TIMEOUT, onConnectError);
        mSocket.off("new message", onNewMessage);
        mSocket.disconnect();
    }

    private void killMediaPlayer() {
        if(mediaPlayer!=null) {
            try {
                mediaPlayer.release();
            }
            catch(Exception e) {
                e.printStackTrace();
            }
        }
    }
    /**Messages part*/
    private void addMessage(String username, String message) {
        mMessages.add(new Message.Builder(Message.TYPE_MESSAGE)
                .username(username).message(message).build());
    }


    private void attemptSend() {
        // if (null == mUsername) return;
        //if (!mSocket.connected()) return;

        String message = "change";      /*change*/        //mInputMessageView.getText().toString().trim();

        addMessage(mUsername, message);

        // perform the sending message attempt.
        mSocket.emit("new message", message);
    }

    private void startSignIn() {
        mUsername = null;
        /**Login*/
        attemptLogin();
        mSocket.on("login", onLogin);
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
                    if(mediaPlayer != null && message.equalsIgnoreCase("change")) {
                        Toast.makeText(getApplicationContext(),"Stopped!",Toast.LENGTH_SHORT).show();
                        mediaPlayer.stop();
                        playbackPosition = 0;
                        try {
                            playAudio(AUDIO_PATH);
                            //    playLocalAudio();
                            //   playLocalAudio_UsingDescriptor();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    if(mediaPlayer != null && message.equalsIgnoreCase("end")) {
                        Toast.makeText(getApplicationContext(),"Connection terminated!",Toast.LENGTH_SHORT).show();
                        mediaPlayer.stop();
                    }
                    if(mediaPlayer != null && message.equalsIgnoreCase("pause")) {
                        Toast.makeText(getApplicationContext(),"Paused!",Toast.LENGTH_SHORT).show();
                        mediaPlayer.pause();
                    }
                    if(mediaPlayer != null && message.equalsIgnoreCase("play")) {
                        Toast.makeText(getApplicationContext(),"Playing!",Toast.LENGTH_SHORT).show();
                        mediaPlayer.start();
                    }
                    if(mediaPlayer != null && message.equalsIgnoreCase("reset")) {
                        Toast.makeText(getApplicationContext(),"Playback reset",Toast.LENGTH_SHORT).show();
                        mediaPlayer.stop();
                        playbackPosition = 0;
                        try {
                            playAudio(AUDIO_PATH);
                            //    playLocalAudio();
                            //   playLocalAudio_UsingDescriptor();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    //removeTyping(username);
                    //addMessage(username, message);
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
            finish();
        }
    };
    /**Login*/

}
