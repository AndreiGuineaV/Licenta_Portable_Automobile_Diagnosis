package com.example.licenta_test.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.licenta_test.R;
import com.example.licenta_test.entities.Car;

import java.util.List;

public class CarAdapter extends RecyclerView.Adapter<CarAdapter.CarViewHolder> {
    private List<Car> carList;
    private Context context;

    private int selectedPosition = -1;

    private OnCarLongClickListener longClickListener;
    public interface OnCarLongClickListener{
        void onCarLongClick(int position);
    }
    public CarAdapter(List<Car> carList, Context context, OnCarLongClickListener longClickListener) {
        this.carList = carList;
        this.context = context;
        this.longClickListener = longClickListener;
    }

    @NonNull
    @Override
    public CarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_my_car, parent, false);
        return new CarViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CarAdapter.CarViewHolder holder, int position) {
        Car currentCar = carList.get(position);
        holder.tvCarName.setText(currentCar.getCarName());
        holder.tvYear.setText(String.valueOf(currentCar.getYear()));
        holder.tvFuelType.setText(currentCar.getFuel());
        holder.tvEngine.setText(String.valueOf(currentCar.getEngine()));
        holder.tvPower.setText(String.valueOf(currentCar.getPower()));
        holder.tvMileage.setText(String.valueOf(currentCar.getKm()));

        if(currentCar.getImgPath() != null && !currentCar.getImgPath().isEmpty())
                Glide.with(context)
                        .load(new java.io.File(currentCar.getImgPath()))
                        .placeholder(android.R.drawable.ic_menu_camera)
                        .error(android.R.drawable.ic_menu_camera)
                        .centerCrop()
                        .into(holder.imgCar);
        else holder.imgCar.setImageResource(android.R.drawable.ic_menu_camera);

        if(position == selectedPosition)
            holder.selectedBadge.setVisibility(View.VISIBLE);
        else holder.selectedBadge.setVisibility(View.GONE);

        holder.itemView.setOnClickListener(v ->{
            int previousPosition = selectedPosition;
            selectedPosition = holder.getBindingAdapterPosition();
            notifyItemChanged(previousPosition);
            notifyItemChanged(selectedPosition);
        });

        holder.itemView.setOnLongClickListener(v -> {
            if(longClickListener != null)
                longClickListener.onCarLongClick(holder.getBindingAdapterPosition());
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return carList.size();
    }

    public Car getSelectedCar(){
        if(selectedPosition != -1)
            return carList.get(selectedPosition);
        return null;
    }

    public static class CarViewHolder extends RecyclerView.ViewHolder {
        ImageView imgCar;
        TextView tvCarName, tvYear, tvFuelType, tvEngine, tvPower, tvMileage, selectedBadge;
        public CarViewHolder(@NonNull View itemView) {
            super(itemView);
            imgCar = itemView.findViewById(R.id.imgCar);
            tvCarName = itemView.findViewById(R.id.tvCarName);
            tvYear = itemView.findViewById(R.id.tvYear);
            tvFuelType = itemView.findViewById(R.id.tvFuelType);
            tvEngine = itemView.findViewById(R.id.tvEngine);
            tvPower = itemView.findViewById(R.id.tvPower);
            tvMileage = itemView.findViewById(R.id.tvMileage);
            selectedBadge = itemView.findViewById(R.id.tvSelectedBadge);
        }
    }
}
