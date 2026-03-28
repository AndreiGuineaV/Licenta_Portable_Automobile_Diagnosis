package com.example.licenta_test.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.licenta_test.Classes.CarCategory;
import com.example.licenta_test.R;

import java.util.List;

public class CarCategoryAdapter extends RecyclerView.Adapter<CarCategoryAdapter.CategoryViewHolder> {

    private Context context;
    private List<CarCategory> carCategoryList;
    private OnCategoryClickListener listener;

    public interface OnCategoryClickListener{
        void onCategoryClick(CarCategory carCategory);
    }

    public CarCategoryAdapter(Context context, List<CarCategory> carCategoryList, OnCategoryClickListener listener) {
        this.context = context;
        this.carCategoryList = carCategoryList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        View view = LayoutInflater.from(context).inflate(R.layout.item_car_part, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        CarCategory currentCategory = carCategoryList.get(position);

        holder.categoryName.setText(currentCategory.getName());
        holder.categoryImg.setImageResource(currentCategory.getImageResId());

        holder.itemView.setOnClickListener(v -> {
            if(listener != null){
                listener.onCategoryClick(currentCategory);
            }
        });
    }

    @Override
    public int getItemCount() {
        return carCategoryList.size();
    }

    public static class CategoryViewHolder extends RecyclerView.ViewHolder{
        ImageView categoryImg;
        TextView categoryName;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryImg = itemView.findViewById(R.id.imgCarCategory);
            categoryName = itemView.findViewById(R.id.categoryName);
        }
    }
}
