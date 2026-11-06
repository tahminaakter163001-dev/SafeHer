
package com.example.safeher;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class EmergencyPlacesAdapter
        extends RecyclerView.Adapter<EmergencyPlacesAdapter.PlaceViewHolder> {

    private final List<EmergencyPlace> places;

    public EmergencyPlacesAdapter(List<EmergencyPlace> places) {
        this.places = places;
    }

    @NonNull
    @Override
    public PlaceViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_emergency_place, parent, false);

        return new PlaceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull PlaceViewHolder holder,
            int position) {

        EmergencyPlace place = places.get(position);

        holder.placeName.setText(place.getName());

        String type = place.getType();

        if ("police".equals(type)) {
            type = "Police Station";
        } else if ("hospital".equals(type)) {
            type = "Hospital";
        } else if ("clinic".equals(type)) {
            type = "Clinic";
        }

        holder.placeType.setText(type);

        String address = place.getAddress();

        if (address == null || address.trim().isEmpty()) {
            address = "Address not available";
        }

        holder.placeAddress.setText(address);
    }
    public void updatePlaceAddress(int position, String address) {

        EmergencyPlace oldPlace = places.get(position);

        EmergencyPlace updatedPlace = new EmergencyPlace(
                oldPlace.getName(),
                oldPlace.getType(),
                oldPlace.getLatitude(),
                oldPlace.getLongitude(),
                address
        );

        places.set(position, updatedPlace);

        notifyItemChanged(position);
    }
    @Override
    public int getItemCount() {
        return places.size();
    }

    static class PlaceViewHolder extends RecyclerView.ViewHolder {

        TextView placeName;
        TextView placeType;
        TextView placeAddress;

        public PlaceViewHolder(@NonNull View itemView) {
            super(itemView);

            placeName = itemView.findViewById(R.id.placeName);
            placeType = itemView.findViewById(R.id.placeType);
            placeAddress = itemView.findViewById(R.id.placeAddress);
        }
    }
}
