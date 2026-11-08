
package com.example.safeher;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

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

        if ("police".equalsIgnoreCase(type)) {
            type = "Police Station";
        } else if ("hospital".equalsIgnoreCase(type)) {
            type = "Hospital";
        } else if ("clinic".equalsIgnoreCase(type)) {
            type = "Clinic";
        }

        holder.placeType.setText(type);

        String address = place.getAddress();

        if (address == null || address.trim().isEmpty()) {
            address = "Address not available";
        }

        holder.placeAddress.setText(address);

        holder.directionsButton.setOnClickListener(v -> {
            openGoogleMaps(
                    holder.itemView,
                    place.getLatitude(),
                    place.getLongitude()
            );
        });
    }

    private void openGoogleMaps(
            View view,
            double latitude,
            double longitude) {

        String destination = String.format(
                Locale.US,
                "%.6f,%.6f",
                latitude,
                longitude
        );

        String mapsUrl =
                "https://www.google.com/maps/dir/?api=1"
                        + "&destination=" + Uri.encode(destination)
                        + "&travelmode=driving"
                        + "&dir_action=navigate";

        Intent intent = new Intent(
                Intent.ACTION_VIEW,
                Uri.parse(mapsUrl)
        );

        try {

            view.getContext().startActivity(intent);

        } catch (ActivityNotFoundException e) {

            Toast.makeText(
                    view.getContext(),
                    "Unable to open Google Maps. Please install a browser or Google Maps.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    public void updatePlaceAddress(int position, String address) {

        if (position < 0 || position >= places.size()) {
            return;
        }

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
        Button directionsButton;

        public PlaceViewHolder(@NonNull View itemView) {
            super(itemView);

            placeName = itemView.findViewById(R.id.placeName);
            placeType = itemView.findViewById(R.id.placeType);
            placeAddress = itemView.findViewById(R.id.placeAddress);
            directionsButton =
                    itemView.findViewById(R.id.directionsButton);
        }
    }
}
