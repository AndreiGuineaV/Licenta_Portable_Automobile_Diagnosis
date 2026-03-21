package com.example.licenta_test.Adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import com.example.licenta_test.Classes.WarningLight;
import com.example.licenta_test.R;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

public class WarningLightAdapter extends RecyclerView.Adapter<WarningLightAdapter.WarningViewHolder> implements Filterable {

    private Context context;
    private List<WarningLight> lightList;
    private List<WarningLight> lightListFull;

    public WarningLightAdapter(Context context, List<WarningLight> lightList) {
        this.context = context;
        this.lightList = lightList;
        this.lightListFull = new ArrayList<>(lightList);
    }

    @NonNull
    @Override
    public WarningViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_warning_light, parent, false);
        return new WarningViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WarningViewHolder holder, int position) {
        WarningLight currentLight = lightList.get(position);
        holder.tvLightName.setText(currentLight.getName());
        holder.tvLightDescription.setText(currentLight.getOtherDetails());

        String imageName = currentLight.getUrlImage();

        holder.imgWarninglight.setTag(imageName);
        holder.imgWarninglight.setImageResource(android.R.drawable.ic_menu_report_image);

        if (imageName != null && !imageName.trim().isEmpty()) {

            StorageReference storageReference = FirebaseStorage.getInstance()
                    .getReference()
                    .child("warning_lights")
                    .child(imageName);

            storageReference.getDownloadUrl().addOnSuccessListener(uri -> {
                Log.d("IMAGINE_SUCCES", "Am găsit link-ul pentru: " + imageName);

            if(holder.imgWarninglight.getTag() != null && holder.imgWarninglight.getTag().equals(imageName)) {
                Glide.with(context)
                        .load(uri) // Folosim 'uri', NU 'storageReference'
                        .placeholder(android.R.drawable.ic_menu_report_image)
                        .error(android.R.drawable.stat_notify_error)
                        .into(holder.imgWarninglight);
            }

            }).addOnFailureListener(e -> {
                Log.e("IMAGINE_EROARE", "Serverul Firebase a refuzat poza: [" + imageName + "]. Motiv: " + e.getMessage());
                holder.imgWarninglight.setImageResource(android.R.drawable.stat_notify_error);
            });

        } else {
            Log.e("IMAGINE_EROARE", "imageName este null sau gol în baza de date pentru: " + currentLight.getName());
            holder.imgWarninglight.setImageResource(android.R.drawable.ic_menu_report_image);
        }
    }

    @Override
    public int getItemCount() {
        return lightList.size();
    }
    public static class WarningViewHolder extends RecyclerView.ViewHolder {

        ImageView imgWarninglight;
        TextView tvLightName;
        TextView tvLightDescription;
        public WarningViewHolder(View itemView) {
            super(itemView);
            imgWarninglight = itemView.findViewById(R.id.imgWarningLight);
            tvLightName = itemView.findViewById(R.id.tvLightName);
            tvLightDescription = itemView.findViewById(R.id.tvLightDescription);
        }
    }

    @Override
    public Filter getFilter() {
        return exampleFilter;
    }

    private Filter exampleFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<WarningLight> filteredList = new ArrayList<>();

            if (constraint == null || constraint.length() == 0) {
                filteredList.addAll(lightListFull);
            } else {
                String filterPattern = constraint.toString().toLowerCase().trim();

                for (WarningLight item : lightListFull) {
                    if (item.getName().toLowerCase().contains(filterPattern)) {
                        filteredList.add(item);
                    }
                }
            }

            FilterResults results = new FilterResults();
            results.values = filteredList;
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            // Curățăm lista actuală și punem doar rezultatele găsite
            lightList.clear();
            lightList.addAll((List) results.values);
            // Spunem RecyclerView-ului să se redeseneze (esențial!)
            notifyDataSetChanged();
        }
    };
}
