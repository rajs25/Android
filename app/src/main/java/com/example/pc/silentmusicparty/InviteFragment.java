package com.example.pc.silentmusicparty;



import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;


/**
 * A simple {@link Fragment} subclass.
 */
public class InviteFragment extends Fragment {

    Button inviteButton;
    EditText inviteText;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View inviteView = inflater.inflate(R.layout.fragment_invite, container, false);

        inviteText = (EditText) inviteView.findViewById(R.id.id_invitedescrip);
        inviteButton = (Button) inviteView.findViewById(R.id.id_invitebutton);

        inviteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");
                String shareBody = inviteText.getText().toString().trim();
                String shareSub = "Invitation";
                sharingIntent.putExtra(Intent.EXTRA_SUBJECT, shareSub);
                sharingIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
                startActivity(Intent.createChooser(sharingIntent, "Share using"));
            }
        });
        return inviteView;
    }

}
