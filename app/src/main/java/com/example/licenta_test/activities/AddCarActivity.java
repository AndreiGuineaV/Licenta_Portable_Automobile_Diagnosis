package com.example.licenta_test.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.licenta_test.R;
import com.example.licenta_test.entities.Car;
import com.example.licenta_test.entities.FuelType;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Calendar;

public class AddCarActivity extends AppCompatActivity {

    private ImageView iconBack;
    private ImageView imgCarPreview;
    private EditText etCarName;
    private EditText etYear;
    private EditText etMileage;
    private Spinner spinnerFuelType;
    private EditText etEngine;
    private EditText etPower;
    private Button btnSaveCar;
    private String imagePath = "";

    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    imagePath = saveUriToInternalStorage(uri);
                    imgCarPreview.setImageURI(uri); // Afișăm pe ecran
                }
            }
    );

    private final ActivityResultLauncher<Void> takePictureLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicturePreview(),
            bitmap -> {
                if (bitmap != null) {
                    imagePath = saveBitmapToInternalStorage(bitmap);
                    imgCarPreview.setImageBitmap(bitmap); // Afișăm pe ecran
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_car);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        iconBack = findViewById(R.id.iconBack);
        imgCarPreview = findViewById(R.id.imgCarPreview);
        etCarName = findViewById(R.id.etCarName);
        etYear = findViewById(R.id.etYear);
        etMileage = findViewById(R.id.etMileage);

        spinnerFuelType = findViewById(R.id.spinnerFuelType);
        ArrayAdapter<FuelType> fuelTypeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, FuelType.values());
        fuelTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFuelType.setAdapter(fuelTypeAdapter);

        etEngine = findViewById(R.id.etEngine);
        etPower = findViewById(R.id.etPower);
        btnSaveCar = findViewById(R.id.btnSaveCar);

        iconBack.setOnClickListener(v -> finish());

        imgCarPreview.setOnClickListener(v-> showImagePickerDialog());

        btnSaveCar.setOnClickListener(v -> {
            String carName = etCarName.getText().toString().trim();
            String mileageStr = etMileage.getText().toString().trim();
            String engineStr = etEngine.getText().toString().trim();
            String powerStr = etPower.getText().toString().trim();
            String yearStr = etYear.getText().toString().trim();

            if (carName.isEmpty() || mileageStr.isEmpty() || engineStr.isEmpty() || powerStr.isEmpty() || yearStr.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields!", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                int km = Integer.parseInt(mileageStr);
                float engine = Float.parseFloat(engineStr);
                int power = Integer.parseInt(powerStr);
                int year = Integer.parseInt(yearStr);
                String fuel = spinnerFuelType.getSelectedItem().toString();

                int currentYear = Calendar.getInstance().get(Calendar.YEAR);
                if (year < 1900 || year > currentYear) {
                    etYear.setError("The year must be between 1900 and " + currentYear);
                    etYear.requestFocus();
                    return;
                }

                if (km < 0 || km > 3000000) {
                    etMileage.setError("Mileage must be between 0 and 3000000 km");
                    etMileage.requestFocus();
                    return;
                }

                if (engine < 0.049f || engine > 8.0f) {
                    etEngine.setError("Engine capacity must be between 0.049L and 8.0L");
                    etEngine.requestFocus();
                    return;
                }

                if (power <= 0 || power > 2200) {
                    etPower.setError("Horsespower must be between 1 and 2200 HP");
                    etPower.requestFocus();
                    return;
                }

                Car car = new Car(carName, km, fuel, engine, power, year, imagePath);
                Intent intent = new Intent();
                intent.putExtra("car", car);
                setResult(RESULT_OK, intent);
                finish();

            } catch (NumberFormatException e) {
                Toast.makeText(this, "Error: Invalid input for mileage, engine, or power.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showImagePickerDialog() {
        String[] options = {"Take Picture", "Choose from Gallery", "Cancel"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add an image");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                takePictureLauncher.launch(null); // Opens up camera
            } else if (which == 1) {
                pickImageLauncher.launch("image/*"); // Opens up gallery
            } else {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    private String saveUriToInternalStorage(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            File file = new File(getFilesDir(), "car_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream outputStream = new FileOutputStream(file);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            outputStream.close();
            inputStream.close();
            return file.getAbsolutePath(); // Returns the path
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    private String saveBitmapToInternalStorage(Bitmap bitmap) {
        try {
            File file = new File(getFilesDir(), "car_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream outputStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
            outputStream.close();
            return file.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
}