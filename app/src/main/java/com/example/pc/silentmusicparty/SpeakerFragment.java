package com.example.pc.silentmusicparty;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Point;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.pc.silentmusicparty.Service.StepCountService;
import com.example.pc.silentmusicparty.barcode.BarcodeCaptureActivity;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.barcode.Barcode;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;


public class SpeakerFragment extends Fragment implements RatingBar.OnRatingBarChangeListener {
    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    private static final int BARCODE_READER_REQUEST_CODE = 1;
    static  String AUDIO_PATH;
    private MediaPlayer mediaPlayer;
    private EditText ip;
    private  String IP;
    private int playbackPosition=0;
    Button  scan;
    ImageButton stopPlayerBtn, mute;
    private static final int REQUEST_LOGIN = 0;
    private Button send;
    private io.socket.client.Socket mSocket;
    private String mUsername="Client";
    private Boolean isConnected = true;
    private static final String INTENT_ACTION = "com.example.pc.silentmusicparty.STEP_COUNT" ;
    private static final int STEP_THRESHOLD = 20;

    AudioManager aManager;
    Boolean isMute=false;

    private List<Message> mMessages = new ArrayList<Message>();

    private OnFragmentInteractionListener mListener;
    private RatingBar mRatingBar;
    private TextView mStepCountView;
    private Intent intent;

    public SpeakerFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_speaker, container, false);
        aManager=(AudioManager)getActivity().getSystemService(Context.AUDIO_SERVICE);
        ip = (EditText) view.findViewById(R.id.serverip);
        mRatingBar = (RatingBar) view.findViewById(R.id.ratingBar);
        mStepCountView = (TextView) view.findViewById(R.id.tv_step_count);
        mStepCountView.setText("0");

        InputFilter[] filters = new InputFilter[1];
        filters[0] = new InputFilter() {
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                if (end > start) {
                    String destTxt = dest.toString();
                    String resultingTxt = destTxt.substring(0, dstart) + source.subSequence(start, end) + destTxt.substring(dend);
                    if (!resultingTxt.matches ("^\\d{1,3}(\\.(\\d{1,3}(\\.(\\d{1,3}(\\.(\\d{1,3})?)?)?)?)?)?")) {
                        return "";
                    } else {
                        String[] splits = resultingTxt.split("\\.");
                        for (int i=0; i<splits.length; i++) {
                            if (Integer.valueOf(splits[i]) > 255) {
                                return "";
                            }
                        }
                    }
                }
                return null;
            }
        };
        ip.setFilters(filters);
        intent = new Intent(getActivity(), StepCountService.class);

        /** Messages part*/
        ChatApplication app = (ChatApplication) getActivity().getApplication();
        mSocket = app.getSocket();

        mSocket.on(Socket.EVENT_CONNECT, onConnect);
        mSocket.on(Socket.EVENT_DISCONNECT, onDisconnect);
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
        stopPlayerBtn = (ImageButton) view.findViewById(R.id.stopPlayerBtn);
        mute=(ImageButton) view.findViewById(R.id.mute);
        scan=(Button) view.findViewById(R.id.scan);
        scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getContext(), BarcodeCaptureActivity.class);
                startActivityForResult(intent, BARCODE_READER_REQUEST_CODE);
            }
        });

        stopPlayerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(
                        getContext());

                alertDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        stopPlayerBtn.setImageResource(R.drawable.stopdisabled);
                        if (mediaPlayer != null) {
                            ip.setText("");
                            stopService();
                            mediaPlayer.stop();
                            playbackPosition = 0;
                            killMediaPlayer();
                            stopPlayerBtn.setEnabled(false);
                            Toast.makeText(getContext(), "Stop", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

                alertDialog.setNegativeButton("No", null);

                alertDialog.setMessage("Do you want to stop the music!? ");
                alertDialog.setTitle("Attention!");
                alertDialog.show();


            }
        });
        mute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!isMute){
                    aManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0);
                    mute.setImageResource(R.drawable.mute);
                    isMute=true;
                }else{
                    aManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE,0);
                    mute.setImageResource(R.drawable.speaker);
                    isMute=false;
                }

            }
        });

        mRatingBar.setOnRatingBarChangeListener(this);
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
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
        Toast.makeText(getActivity(),"You gave "+rating+" stars to this song.",Toast.LENGTH_LONG).show();
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        killMediaPlayer();
        stopService();
        mediaPlayer.stop();
        mSocket.off("login", onLogin);
        mSocket.off(Socket.EVENT_CONNECT, onConnect);
        mSocket.off(Socket.EVENT_DISCONNECT, onDisconnect);
        mSocket.off(Socket.EVENT_CONNECT_ERROR, onConnectError);
        mSocket.off(Socket.EVENT_CONNECT_TIMEOUT, onConnectError);
        mSocket.off("new message", onNewMessage);
        mSocket.disconnect();
    }
    private void playAudio(String url) throws Exception
    {
        killMediaPlayer();
        startService();
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setDataSource(url);
        mediaPlayer.prepare();
        mediaPlayer.start();
    }
    private void killMediaPlayer() {
        if(mediaPlayer!=null) {
            try {
                mediaPlayer.release();
                mediaPlayer = null;
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
        //Intent intent = new Intent(MemberActivity.this, ChatLoginActivity.class);
        //startActivityForResult(intent, REQUEST_LOGIN);
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
                    if(mediaPlayer != null && message.equalsIgnoreCase("+change")) {
                        Toast.makeText(getContext(),"Change!",Toast.LENGTH_SHORT).show();
                        stopService();
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
                    if(mediaPlayer != null && message.equalsIgnoreCase("+end")) {
                        Toast.makeText(getContext(),"Connection terminated by the host!",Toast.LENGTH_SHORT).show();
                        ip.setText("");
                        stopService();
                        mediaPlayer.stop();
                        playbackPosition = 0;
                        killMediaPlayer();
                        stopPlayerBtn.setEnabled(false);
                    }
                    if(mediaPlayer != null && message.equalsIgnoreCase("+stop")) {
                        Toast.makeText(getContext(),"Playback stopped!",Toast.LENGTH_SHORT).show();
                        stopService();
                        mediaPlayer.stop();
                    }
                    if(mediaPlayer != null && message.equalsIgnoreCase("+pause")) {
                        Toast.makeText(getContext(),"Paused!",Toast.LENGTH_SHORT).show();
                        mediaPlayer.pause();
                    }
                    if( message.equalsIgnoreCase("+play")) {
                        Toast.makeText(getContext(),"Playing!",Toast.LENGTH_SHORT).show();
                        if(mediaPlayer==null &&ip!=null){
                            try {
                                stopPlayerBtn.setEnabled(true);
                                stopPlayerBtn.setImageResource(R.drawable.stop);
                                playAudio(AUDIO_PATH);
                                //    playLocalAudio();
                                //   playLocalAudio_UsingDescriptor();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else if (mediaPlayer != null){
                            mediaPlayer.start();
                        }

                    }
                    if(mediaPlayer != null && message.equalsIgnoreCase("+reset")) {
                        Toast.makeText(getContext(),"Playback reset",Toast.LENGTH_SHORT).show();
                        stopService();
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
                    if(mediaPlayer != null && message.contains("Track:")) {
                        Toast.makeText(getContext(),message,Toast.LENGTH_SHORT).show();
                    }
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
            //finish();
        }
    };
    /**Login*/
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == BARCODE_READER_REQUEST_CODE) {
            if (resultCode == CommonStatusCodes.SUCCESS) {
                if (data != null) {
                    Barcode barcode = data.getParcelableExtra(BarcodeCaptureActivity.BarcodeObject);
                    Point[] p = barcode.cornerPoints;
                    ip.setText(barcode.displayValue);
                    IP = ip.getText().toString();
                    Toast.makeText(getContext(), "Server IP: "+IP, Toast.LENGTH_SHORT).show();
                    AUDIO_PATH = "http://" + IP + ":8080/";
                } else ip.setText(null);
            } else Log.e(LOG_TAG, String.format("error reading code",
                    CommonStatusCodes.getStatusCodeString(resultCode)));
        } else super.onActivityResult(requestCode, resultCode, data);
    }

   public void startService(){
       getActivity().startService(intent);
   }
    public void stopService(){
        getActivity().stopService(intent);
    }

    private BroadcastReceiver stepCountReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String step_count = intent.getStringExtra("STEP_COUNT");
            mStepCountView.setText(step_count);
            if(Integer.valueOf(step_count)> STEP_THRESHOLD){
                mRatingBar.setVisibility(View.VISIBLE);
            }
        }
    };

    @Override
    public void onResume() {
        getActivity().registerReceiver(stepCountReceiver, new IntentFilter(INTENT_ACTION));
        super.onResume();
    }

    @Override
    public void onPause() {
        getActivity().unregisterReceiver(stepCountReceiver);
        super.onPause();
    }


}
