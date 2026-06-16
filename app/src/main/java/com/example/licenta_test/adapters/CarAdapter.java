package com.example.licenta_test.adapters;

import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.licenta_test.R;
import com.example.licenta_test.entities.Car;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CarAdapter extends RecyclerView.Adapter<CarAdapter.CarViewHolder> {
    private List<Car> carList;
    private Context context;
    private String activeCarId = null;

    private OnCarLongClickListener longClickListener;
    private OnReminderEditListener editListener;
    private OnCarEditInfoListener onCarEditInfoListener;
    private OnCarJournalListener onCarJournalListener;
    private OnCarShareListener shareListener;



    public interface OnCarLongClickListener{
        void onCarLongClick(int position);
    }

    public interface OnReminderEditListener {
        void onEditReminders(Car car, int position);
    }

    public interface OnCarEditInfoListener {
        void onEditInfo(Car car, int position);
    }
    public interface OnCarJournalListener{
        void onOpenJournal(Car car);
    }
    public interface OnCarShareListener {
        void onShareCar(Car car, int position);
    }


    public CarAdapter(List<Car> carList, Context context, OnCarLongClickListener longClickListener, OnReminderEditListener editListener, OnCarEditInfoListener onCarEditInfoListener, OnCarJournalListener onCarJournalListener, OnCarShareListener shareListener) {
        this.carList = carList;
        this.context = context;
        this.longClickListener = longClickListener;
        this.editListener = editListener;
        this.onCarEditInfoListener = onCarEditInfoListener;
        this.onCarJournalListener = onCarJournalListener;
        this.shareListener = shareListener;
    }

    public void setActiveCarId(String activeCarId) {
        this.activeCarId = activeCarId;
        notifyDataSetChanged();
    }

    public String getActiveCarId() {
        return activeCarId;
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


        if (currentCar.getFuel() != null && currentCar.getFuel().equalsIgnoreCase("ELECTRIC")) {
            holder.tvEngine.setVisibility(View.INVISIBLE);
        } else {
            holder.tvEngine.setVisibility(View.VISIBLE);
            String formattedEngine = String.format(Locale.getDefault(), "%.1f L", currentCar.getEngine());
            holder.tvEngine.setText(String.format(java.util.Locale.getDefault(), "%.1f L", currentCar.getEngine()));
        }
        String formattedPower = String.format(Locale.getDefault(), "%d Hp", currentCar.getPower());
        holder.tvPower.setText(formattedPower);
        String formattedMileage = String.format(Locale.getDefault(), "%d Km", currentCar.getKm());
        holder.tvMileage.setText(formattedMileage);

        if(currentCar.getImgPath() != null && !currentCar.getImgPath().isEmpty()) {
            Glide.with(context)
                    .load(currentCar.getImgPath())
                    .placeholder(android.R.drawable.ic_menu_camera)
                    .error(android.R.drawable.ic_menu_camera)
                    .into(holder.imgCar);
        } else {
            holder.imgCar.setImageResource(android.R.drawable.ic_menu_camera);
        }

        boolean isSelected = currentCar.getId() != null && currentCar.getId().equals(activeCarId);
        if(isSelected) {
            holder.selectedBadge.setVisibility(View.VISIBLE);
        } else {
            holder.selectedBadge.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v ->{
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            String carId = currentCar.getId();

            if (carId == null) return; //safety

            FirebaseFirestore db = FirebaseFirestore.getInstance();

            if (carId.equals(activeCarId)) {
                // deselect
                db.collection("Users").document(uid).update("activeCarId", null)
                        .addOnSuccessListener(aVoid -> {
                            this.activeCarId = null;
                            sortCars(); // Sort and update the interface instantly
                            Toast.makeText(context, "Car set as inactive!", Toast.LENGTH_SHORT).show();
                        });
            }
            else{
                // select the car for diagnostic
                db.collection("Users").document(uid).update("activeCarId", carId)
                        .addOnSuccessListener(aVoid -> {
                            this.activeCarId = carId;
                            sortCars(); // Sort and move the car to the first position instantly
                            Toast.makeText(context, "Car set as active!", Toast.LENGTH_SHORT).show();
                        });
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if(longClickListener != null)
                longClickListener.onCarLongClick(holder.getBindingAdapterPosition());
            return true;
        });

        // POPUP MENU LOGIC
        holder.btnMoreOptions.setOnClickListener(v -> {
            android.widget.PopupMenu popup = new android.widget.PopupMenu(context, holder.btnMoreOptions);
            popup.inflate(R.menu.menu_car_options);

            popup.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();

                if (id == R.id.menu_journal) {
                    if (onCarJournalListener != null) onCarJournalListener.onOpenJournal(currentCar);
                    return true;
                } else if (id == R.id.menu_alerts) {
                    if (editListener != null) editListener.onEditReminders(currentCar, position);
                    return true;
                } else if (id == R.id.menu_edit) {
                    if (onCarEditInfoListener != null) onCarEditInfoListener.onEditInfo(currentCar, position);
                    return true;
                } else if (id == R.id.menu_share) {
                    if (shareListener != null) shareListener.onShareCar(currentCar, position);
                    return true;
                } else if (id == R.id.menu_delete) {
                    // Reuse the old LongClick interface to trigger deletion
                    if (longClickListener != null) longClickListener.onCarLongClick(position);
                    return true;
                }

                return false;
            });

            popup.show();
        });
    }

    public void sortCars() {
        if (carList == null || carList.isEmpty()) return;

        // Sort the list: active car first, then alphabetically by name
        java.util.Collections.sort(carList, (car1, car2) -> {
            boolean isCar1Active = car1.getId() != null && car1.getId().equals(activeCarId);
            boolean isCar2Active = car2.getId() != null && car2.getId().equals(activeCarId);

            if (isCar1Active && !isCar2Active) {
                return -1; // car1 (active) comes first
            } else if (!isCar1Active && isCar2Active) {
                return 1;  // car2 (active) comes first
            } else {
                // If neither is active (or both are unselected), sort alphabetically ignoring case
                String name1 = car1.getCarName() != null ? car1.getCarName() : "";
                String name2 = car2.getCarName() != null ? car2.getCarName() : "";

                return name1.compareToIgnoreCase(name2);
            }
        });

        notifyDataSetChanged();
    }

    private void showCarRemindersDialog(Car car, Context context) {
        long now = System.currentTimeMillis();
        long thirtyDaysInMs = 30L * 24 * 60 * 60 * 1000;

        StringBuilder details = new StringBuilder();

        details.append(formatReminderStatus("ITP", car.getItpExpiration(), now, thirtyDaysInMs));
        details.append(formatReminderStatus("RCA", car.getRcaExpiration(), now, thirtyDaysInMs));
        details.append(formatReminderStatus("Rovinieta", car.getRovinietaExpiration(), now, thirtyDaysInMs));
        details.append(formatReminderStatus("Oil Change", car.getOilChangeDate(), now, thirtyDaysInMs));

        if (details.length() == 0) {
            details.append("No reminders set for this vehicle.");
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Reminders: " + car.getCarName());
        builder.setMessage(Html.fromHtml(details.toString(), Html.FROM_HTML_MODE_COMPACT));

        builder.setPositiveButton("Close", (dialog, which) -> dialog.dismiss());

        builder.setNeutralButton("Edit", (dialog, which) -> {
            if (editListener != null) {
                //calls the MyGarageActivity's onEditReminders method and sends the car
                int position = carList.indexOf(car);
                editListener.onEditReminders(car, position);
            }
        });

        builder.show();
    }

    private String formatReminderStatus(String name, long expirationDate, long now, long thirtyDaysInMs) {
        if (expirationDate == 0) return ""; // Not set

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        String dateString = sdf.format(new Date(expirationDate));

        if (expirationDate < now) {
            return "<b>" + name + "</b>: <font color='#D32F2F'>" + dateString + " (EXPIRED)</font><br><br>";
        } else if (expirationDate - now <= thirtyDaysInMs) {
            return "<b>" + name + "</b>: <font color='#F57F17'>" + dateString + " (Expiring Soon)</font><br><br>";
        } else {
            return "<b>" + name + "</b>: <font color='#388E3C'>" + dateString + " (Valid)</font><br><br>"; // Green for valid
        }
    }

    @Override
    public int getItemCount() {
        return carList.size();
    }

    public Car getSelectedCar(){
        for (Car car : carList) {
            if (car.getId() != null && car.getId().equals(activeCarId)) {
                return car;
            }
        }
        return null;
    }

    public static class CarViewHolder extends RecyclerView.ViewHolder {
        ImageView imgCar, btnMoreOptions;
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
            btnMoreOptions = itemView.findViewById(R.id.btnMoreOptions);
        }
    }
}
