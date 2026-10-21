package com.example.safeher;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class EmergencyContactAdapter
        extends RecyclerView.Adapter<EmergencyContactAdapter.ContactViewHolder> {

    public interface ContactActionListener {
        void onEdit(EmergencyContact contact);
        void onDelete(EmergencyContact contact);
    }

    private final ArrayList<EmergencyContact> contacts;
    private final ContactActionListener listener;

    public EmergencyContactAdapter(
            ArrayList<EmergencyContact> contacts,
            ContactActionListener listener) {
        this.contacts = contacts;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_emergency_contact, parent, false);

        return new ContactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ContactViewHolder holder, int position) {

        EmergencyContact contact = contacts.get(position);

        holder.nameText.setText(contact.getName());
        holder.phoneText.setText(contact.getPhoneNumber());

        holder.editButton.setOnClickListener(
                v -> listener.onEdit(contact));

        holder.deleteButton.setOnClickListener(
                v -> listener.onDelete(contact));
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    static class ContactViewHolder extends RecyclerView.ViewHolder {
        TextView nameText;
        TextView phoneText;
        Button editButton;
        Button deleteButton;

        ContactViewHolder(@NonNull View itemView) {
            super(itemView);

            nameText = itemView.findViewById(R.id.contactNameText);
            phoneText = itemView.findViewById(R.id.contactPhoneText);
            editButton = itemView.findViewById(R.id.editContactButton);
            deleteButton = itemView.findViewById(R.id.deleteContactButton);
        }
    }
}
