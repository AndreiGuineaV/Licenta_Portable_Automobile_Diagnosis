package com.example.licenta_test.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.licenta_test.R;
import com.example.licenta_test.adapters.InvitesAdapter;
import com.example.licenta_test.entities.CarInvite;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class InvitesActivity extends AppCompatActivity {

    private RecyclerView recyclerInvites;
    private TextView tvNoInvites;
    private InvitesAdapter adapter;
    private List<CarInvite> inviteList;
    private FirebaseFirestore db;
    private String myUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_invites);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ImageView iconBack = findViewById(R.id.iconBack);
        iconBack.setOnClickListener(v -> finish());

        recyclerInvites = findViewById(R.id.recyclerInvites);
        tvNoInvites = findViewById(R.id.tvNoInvites);

        recyclerInvites.setLayoutManager(new LinearLayoutManager(this));
        inviteList = new ArrayList<>();

        // Inițializăm adaptorul separat și implementăm interfața
        adapter = new InvitesAdapter(inviteList, new InvitesAdapter.OnInviteActionListener() {
            @Override
            public void onAcceptClick(CarInvite invite, int position) {
                acceptInvite(invite, position);
            }

            @Override
            public void onDeclineClick(CarInvite invite, int position) {
                deleteInvite(invite, position, "Invite declined.");
            }
        });

        recyclerInvites.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        loadInvites();
    }

    private void loadInvites() {
        db.collection("Invites")
                .whereEqualTo("targetUid", myUid)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    inviteList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        CarInvite invite = doc.toObject(CarInvite.class);
                        invite.setId(doc.getId());
                        inviteList.add(invite);
                    }

                    if (inviteList.isEmpty()) {
                        tvNoInvites.setVisibility(View.VISIBLE);
                        recyclerInvites.setVisibility(View.GONE);
                    } else {
                        tvNoInvites.setVisibility(View.GONE);
                        recyclerInvites.setVisibility(View.VISIBLE);
                        adapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error loading invites", Toast.LENGTH_SHORT).show());
    }

    private void acceptInvite(CarInvite invite, int position) {
        // Verifying the invite type
        if (invite.getInviteType() != null && invite.getInviteType().equals("transfer")) {

            // Transfer ownership
            db.collection("Vehicles").document(invite.getCarId())
                    .update(
                            "ownerId", myUid,
                            "sharedWith", new ArrayList<String>()
                    )
                    .addOnSuccessListener(aVoid -> {
                        deleteInvite(invite, position, "Transfer accepted! You are now the owner.");
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to accept transfer.", Toast.LENGTH_LONG).show());

        } else {
            // Add the user in the sharedWith list
            db.collection("Vehicles").document(invite.getCarId())
                    .update("sharedWith", FieldValue.arrayUnion(myUid))
                    .addOnSuccessListener(aVoid -> {
                        deleteInvite(invite, position, "Invite accepted! Car added to your garage.");
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to accept invite.", Toast.LENGTH_LONG).show());
        }
    }
    private void deleteInvite(CarInvite invite, int position, String successMessage) {
        db.collection("Invites").document(invite.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    inviteList.remove(position);
                    adapter.notifyItemRemoved(position);
                    adapter.notifyItemRangeChanged(position, inviteList.size());

                    if (inviteList.isEmpty()) {
                        tvNoInvites.setVisibility(View.VISIBLE);
                        recyclerInvites.setVisibility(View.GONE);
                    }
                    Toast.makeText(this, successMessage, Toast.LENGTH_SHORT).show();
                });
    }
}