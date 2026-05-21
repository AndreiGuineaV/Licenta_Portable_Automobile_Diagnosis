package com.example.licenta_test.additional;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.licenta_test.R;
import com.example.licenta_test.activities.MyGarageActivity;
import com.example.licenta_test.entities.Car;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.concurrent.TimeUnit;

public class ReminderWorker extends Worker {

    private static final String CHANNEL_ID = "AutoAssist_Reminders";

    public ReminderWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        // Dacă nu e nimeni logat, nu facem nimic
        if (user == null) {
            return Result.success();
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        try {
            //Tasks.await blocks the main thread until the task is completed
            QuerySnapshot carsSnapshot = Tasks.await(db.collection("Users").document(user.getUid()).collection("Cars").get());

            long now = System.currentTimeMillis();

            for (QueryDocumentSnapshot doc : carsSnapshot) {
                Car car = doc.toObject(Car.class);
                if (car != null) {
                    checkAndNotify(car.getCarName(), "ITP", car.getItpExpiration(), now);
                    checkAndNotify(car.getCarName(), "RCA", car.getRcaExpiration(), now);
                    checkAndNotify(car.getCarName(), "Rovinieta", car.getRovinietaExpiration(), now);
                    checkAndNotify(car.getCarName(), "Oil Change", car.getOilChangeDate(), now);
                }
            }

            return Result.success();
        } catch (Exception e) {
            Log.e("ReminderWorker", "Error fetching cars for reminders", e);
            return Result.retry();
        }
    }

    private void checkAndNotify(String carName, String documentType, long expirationDate, long now) {
        if (expirationDate == 0) return;

        // Calculate the number of days left
        long diffInMillis = expirationDate - now;
        int daysLeft = (int) TimeUnit.MILLISECONDS.toDays(diffInMillis);

        // Dacă e în aceeași zi, dar diferența de ore a trecut-o pe negativ, o considerăm 0
        if (diffInMillis > 0 && daysLeft == 0) daysLeft = 0;

        String message = null;

        // Logica ta: 3, 2, 1, 0 sau expirat
        if (daysLeft == 3) {
            message = documentType + " expires in exactly 3 days!";
        } else if (daysLeft == 2) {
            message = documentType + " expires in 2 days!";
        } else if (daysLeft == 1) {
            message = documentType + " expires TOMORROW!";
        } else if (daysLeft == 0) {
            message = documentType + " EXPIRES TODAY! Please renew it.";
        } else if (daysLeft < 0 && daysLeft >= -3) {
            // Îi spunem că a expirat timp de 3 zile după expirare (ca să nu îl spamăm la infinit)
            message = "ALERT: " + documentType + " is EXPIRED!";
        }

        if (message != null) {
            sendNotification(carName, message);
        }
    }

    private void sendNotification(String carName, String messageText) {
        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

        //Creates a notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Vehicle Reminders", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        // When click on notification, go to MyGarageActivity
        Intent intent = new Intent(getApplicationContext(), MyGarageActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_IMMUTABLE);

        // Notification builder
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert) // Notification app logo
                .setContentTitle(carName + " Reminder")
                .setContentText(messageText)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        // Using a notificationID to avoid collisions (the hashcode made by carName and messageText)
        int notificationId = (carName + messageText).hashCode();
        notificationManager.notify(notificationId, builder.build());
    }
}