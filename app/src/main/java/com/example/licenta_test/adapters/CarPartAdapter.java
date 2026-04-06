package com.example.licenta_test.adapters;

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
import com.example.licenta_test.entities.CarPart;
import com.example.licenta_test.R;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

public class CarPartAdapter extends RecyclerView.Adapter<CarPartAdapter.PartViewHolder> implements Filterable {

    private Context context;
    private List<CarPart> carPartList;
    private List<CarPart> carPartListFull;
    private OnPartClickListener listener;

    public interface OnPartClickListener {
        void onPartClick(CarPart carPart);
    }

    public CarPartAdapter(Context context, List<CarPart> carPartList, OnPartClickListener listener) {
        this.context = context;
        this.carPartList = carPartList;
        this.carPartListFull = new ArrayList<>(carPartList);
        this.listener = listener;
    }

    @NonNull
    @Override
    public PartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_car_part, parent, false);
        return new PartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PartViewHolder holder, int position) {
        CarPart currentPart = carPartList.get(position);
        holder.partName.setText(currentPart.getName());

        String imageName = currentPart.getUrlImage();

        holder.partImg.setTag(imageName);
        holder.partImg.setImageResource(android.R.drawable.ic_menu_report_image);

        if (imageName != null && !imageName.trim().isEmpty()) {

            StorageReference storageReference = FirebaseStorage.getInstance()
                    .getReference()
                    .child("car_parts")
                    .child(imageName);

            storageReference.getDownloadUrl().addOnSuccessListener(uri -> {
                Log.d("IMAGE_SUCCESS", "Found the link for: " + imageName);

                if(holder.partImg.getTag() != null && holder.partImg.getTag().equals(imageName)) {
                    Glide.with(context)
                            .load(uri) // Folosim 'uri', NU 'storageReference'
                            .placeholder(android.R.drawable.ic_menu_report_image)
                            .error(android.R.drawable.stat_notify_error)
                            .into(holder.partImg);
                }

            }).addOnFailureListener(e -> {
                Log.e("IMAGINE_EROARE", "Serverul Firebase a refuzat poza: [" + imageName + "]. Motiv: " + e.getMessage());
                holder.partImg.setImageResource(android.R.drawable.stat_notify_error);
            });

        } else {
            Log.e("IMAGINE_EROARE", "imageName este null sau gol în baza de date pentru: " + currentPart.getName());
            holder.partImg.setImageResource(android.R.drawable.ic_menu_report_image);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPartClick(currentPart);
            }
        });
    }

    @Override
    public int getItemCount() {
        return carPartList.size();
    }

    public static class PartViewHolder extends RecyclerView.ViewHolder {
        ImageView partImg;
        TextView partName;

        public PartViewHolder(@NonNull View itemView) {
            super(itemView);
            partImg = itemView.findViewById(R.id.imgCarCategory);
            partName = itemView.findViewById(R.id.categoryName);
        }
    }

    @Override
    public Filter getFilter() {
        return exampleFilter;
    }
    private Filter exampleFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<CarPart> filteredList = new ArrayList<>();

            if (constraint == null || constraint.length() == 0) {
                filteredList.addAll(carPartListFull);
            } else {
                String filterPattern = constraint.toString().toLowerCase().trim();

                for (CarPart item : carPartListFull) {
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
            carPartList.clear();
            carPartList.addAll((List) results.values);
            notifyDataSetChanged();
        }
    };
}