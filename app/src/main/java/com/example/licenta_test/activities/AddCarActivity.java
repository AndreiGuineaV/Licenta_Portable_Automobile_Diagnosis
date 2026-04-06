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
            try {
                String carName = etCarName.getText().toString();
                int km = Integer.parseInt(etMileage.getText().toString());
                String fuel = spinnerFuelType.getSelectedItem().toString();
                float engine = Float.parseFloat(etEngine.getText().toString());
                int power = Integer.parseInt(etPower.getText().toString());
                int year = Integer.parseInt(etYear.getText().toString());

                Car car = new Car(carName, km, fuel, engine, power, year, imagePath);
                Intent intent = new Intent();
                intent.putExtra("car", car);
                setResult(RESULT_OK, intent);
                finish();
            } catch (NumberFormatException e) {
                Log.e("NumberFormatException", "Invalid input format");
                throw new RuntimeException(e);
            }
        });
    }

    private void showImagePickerDialog() {
        String[] options = {"Fă o poză", "Alege din Galerie", "Anulează"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Adaugă o imagine");
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