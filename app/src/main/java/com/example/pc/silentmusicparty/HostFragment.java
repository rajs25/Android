package com.example.pc.silentmusicparty;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

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

public class HostFragment extends Fragment {
    private OnFragmentInteractionListener mListener;
    private static final int REQUEST_LOGIN = 0;
    private Button send;
    private Socket mSocket;
    private String mUsername="Server";
    private Boolean isConnected = true;

    private List<Message> mMessages = new ArrayList<Message>();
    private MyServer server;
    Button stop, reset, sync;
    ImageView imageView, qr;
    TextView ip;
    String message;
    Boolean isPlaying=false;
    ImageButton playlist, play, next, previous;
    private SongsManager songManager;
    MediaPlayer mp;
    private ArrayList<HashMap<String, String>> songsList = new ArrayList<HashMap<String, String>>();
    private int currentSongIndex = 0;
    private TextView title;
    public final static int QRcodeWidth = 500 ;
    Bitmap bitmap;
    String EditTextValue;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View view= inflater.inflate(R.layout.fragment_host, container, false);
        stop=(Button) view.findViewById(R.id.song1);
        play=(ImageButton) view.findViewById(R.id.song2);
        next= (ImageButton) view.findViewById(R.id.next);
        previous=(ImageButton) view.findViewById(R.id.prev);
        reset=(Button) view.findViewById(R.id.reset);
        //sync=(Button) view.findViewById(R.id.sync);
        playlist=(ImageButton) view.findViewById(R.id.playlist);
        qr=(ImageButton) view.findViewById(R.id.qr);
        imageView=(ImageView) view.findViewById(R.id.imageView3);
        title=(TextView) view.findViewById(R.id.title);
        // Mediaplayer
        mp = new MediaPlayer();
        songManager = new SongsManager();
        // Getting all songs list
        songsList = songManager.getPlayList();
        // By default play first song
        //playSong(0);
        ip=(TextView)view.findViewById(R.id.ip);
        ip.setText(getLocalIpAddress());
        ip.setEnabled(false);
/** Messages part*/
        ChatApplication app = (ChatApplication) getActivity().getApplication();
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
        //attemptLogin();
        //mSocket.on("login", onLogin);
        //playSong(0);
        //  startSignIn();
        currentSongIndex=0;
/** Messages part*/
        EditTextValue=ip.getText().toString();


        qr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

               try {
                    bitmap = TextToImageEncode(EditTextValue);
                    imageView.setImageBitmap(bitmap);
                } catch (WriterException e) {
                    e.printStackTrace();
                }
            }
        });

        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                play.setImageResource(R.drawable.play);
                mp.stop();
                attemptSend("+stop");
            }
        });

        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isPlaying==false&&currentSongIndex!=0) {
                    //play.setText("Pause");
                    play.setImageResource(R.drawable.pause);
                    //playSong(0);
                    mp.start();
                    //server.fileloc = Environment.getExternalStorageDirectory() + "/test2.mp3";
                    isPlaying=true;
                    attemptSend("+play");
                } else if (isPlaying==true){
                    //play.setText("Play");
                    play.setImageResource(R.drawable.play);
                    mp.pause();
                    attemptSend("+pause");
                    isPlaying=false;
                }
            }
        });
        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptSend("+reset");
                mp.stop();
                playSong(currentSongIndex);
            }
        });
        playlist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getContext(), PlayListActivity.class);
                startActivityForResult(i, 100);
            }
        });
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // check if next song is there or not
                if(currentSongIndex < (songsList.size() - 1)){
                    isPlaying=false;
                    play.setImageResource(R.drawable.pause);
                    attemptSend("+change");
                    playSong(currentSongIndex + 1);
                    currentSongIndex = currentSongIndex + 1;
                }else{
                    // play first song
                    isPlaying=false;
                    play.setImageResource(R.drawable.pause);
                    attemptSend("+change");
                    playSong(0);
                    currentSongIndex = 0;
                }
            }
        });
        previous.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(currentSongIndex > 0){
                    isPlaying=false;
                    play.setImageResource(R.drawable.pause);
                    attemptSend("+change");
                    playSong(currentSongIndex - 1);
                    currentSongIndex = currentSongIndex - 1;
                }else{
                    // play last song
                    isPlaying=false;
                    play.setImageResource(R.drawable.pause);
                    attemptSend("+change");
                    playSong(songsList.size() - 1);
                    currentSongIndex = songsList.size() - 1;
                }
            }
        });
        return view;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
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
    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
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
    public void onDestroy() {
        super.onDestroy();
        attemptSend("+end");
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
        if(message=="+play")
        {
            Toast.makeText(getContext(),"playing!", Toast.LENGTH_SHORT).show();
        }
        if(message=="+pause")
        {
            Toast.makeText(getContext(),"paused!", Toast.LENGTH_SHORT).show();
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
        Intent intent = new Intent(getContext(), LoginActivity.class);
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
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(!isConnected) {
                        if(null!=mUsername)
                            mSocket.emit("add user", mUsername);
                        Toast.makeText(getContext(),
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
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    isConnected = false;
                    Toast.makeText(getContext(),
                            "Disconnected", Toast.LENGTH_LONG).show();
                }
            });
        }
    };

    private Emitter.Listener onConnectError = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getContext(),
                            "Disconnected, Please check Internet", Toast.LENGTH_LONG).show();
                }
            });
        }
    };

    private Emitter.Listener onNewMessage = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            getActivity().runOnUiThread(new Runnable() {
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
            //setResult(RESULT_OK, intent);
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
            play.setImageResource(R.drawable.pause);
            mp.start();
            // Displaying Song title
            String songTitle = songsList.get(songIndex).get("songTitle");
            title.setText(songTitle);

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
    public void onActivityResult(int requestCode,
                                    int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == 100){
            play.setImageResource(R.drawable.pause);
            attemptSend("+play");
            isPlaying=true;
            currentSongIndex = data.getExtras().getInt("songIndex");
            server.fileloc=songsList.get(currentSongIndex).get("songPath");
            //Toast.makeText(getContext(),"Track: "+title.getText().toString(), Toast.LENGTH_SHORT).show();
            // play selected song
            playSong(currentSongIndex);
            attemptSend("Track: "+title.getText().toString());
        }

    }
    Bitmap TextToImageEncode(String Value) throws WriterException {
        BitMatrix bitMatrix;
        try {
            bitMatrix = new MultiFormatWriter().encode(
                    Value,
                    BarcodeFormat.DATA_MATRIX.QR_CODE,
                    QRcodeWidth, QRcodeWidth, null
            );

        } catch (IllegalArgumentException Illegalargumentexception) {

            return null;
        }
        int bitMatrixWidth = bitMatrix.getWidth();

        int bitMatrixHeight = bitMatrix.getHeight();

        int[] pixels = new int[bitMatrixWidth * bitMatrixHeight];

        for (int y = 0; y < bitMatrixHeight; y++) {
            int offset = y * bitMatrixWidth;

            for (int x = 0; x < bitMatrixWidth; x++) {

                pixels[offset + x] = bitMatrix.get(x, y) ?
                        getResources().getColor(R.color.QRCodeBlackColor):getResources().getColor(R.color.QRCodeWhiteColor);
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(bitMatrixWidth, bitMatrixHeight, Bitmap.Config.ARGB_4444);

        bitmap.setPixels(pixels, 0, 500, 0, 0, bitMatrixWidth, bitMatrixHeight);
        return bitmap;
    }
}
