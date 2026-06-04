package com.example.licenta_test.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.licenta_test.R;
import com.example.licenta_test.entities.CarInvite;

import java.util.List;

public class InvitesAdapter extends RecyclerView.Adapter<InvitesAdapter.InviteViewHolder> {

    private List<CarInvite> inviteList;
    private OnInviteActionListener listener;

    // Interfața prin care comunicăm click-urile către InvitesActivity
    public interface OnInviteActionListener {
        void onAcceptClick(CarInvite invite, int position);
        void onDeclineClick(CarInvite invite, int position);
    }

    public InvitesAdapter(List<CarInvite> inviteList, OnInviteActionListener listener) {
        this.inviteList = inviteList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public InviteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_invite, parent, false);
        return new InviteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull InviteViewHolder holder, int position) {
        CarInvite invite = inviteList.get(position);

        String actionText = (invite.getInviteType() != null && invite.getInviteType().equals("transfer"))
                ? " wants to transfer ownership of: "
                : " invited you to access: ";
        String finalActionText = invite.getSenderEmail() + actionText + invite.getCarName();
        holder.tvMessage.setText(finalActionText);

        holder.btnAccept.setOnClickListener(v -> {
            int currentPos = holder.getBindingAdapterPosition();
            if (currentPos != RecyclerView.NO_POSITION && listener != null) {
                listener.onAcceptClick(invite, currentPos);
            }
        });

        holder.btnDecline.setOnClickListener(v -> {
            int currentPos = holder.getBindingAdapterPosition();
            if (currentPos != RecyclerView.NO_POSITION && listener != null) {
                listener.onDeclineClick(invite, currentPos);
            }
        });
    }

    @Override
    public int getItemCount() {
        return inviteList.size();
    }

    public static class InviteViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;
        Button btnAccept, btnDecline;

        public InviteViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvInviteMessage);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnDecline = itemView.findViewById(R.id.btnDecline);
        }
    }
}