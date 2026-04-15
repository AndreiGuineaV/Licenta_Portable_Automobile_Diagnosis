package com.example.licenta_test.additional;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.licenta_test.entities.Car;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

public class GarageStorage {
    public static final String PREFS_NAME = "MyGaragePrefs";
    public static final String CAR_LIST_KEY = "carList";
    public static final String SELECTED_CAR_KEY = "selectedCar";


    public static void saveCarList(Context context, List<Car> carList) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        Gson gson = new Gson();
        String json = gson.toJson(carList);

        editor.putString(CAR_LIST_KEY, json);
        editor.apply();
    }

    public static List<Car> loadCarList(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(CAR_LIST_KEY, null);
        if(json == null) return new ArrayList<>();

        Gson gson = new Gson();
        return gson.fromJson(json, new com.google.gson.reflect.TypeToken<List<Car>>() {}.getType());
    }

    public static void saveSelectedCar(Context context, Car car) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        Gson gson = new Gson();
        String json = gson.toJson(car);

        editor.putString(SELECTED_CAR_KEY, json);
        editor.apply();
    }

    public static Car getSelectedCar(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(SELECTED_CAR_KEY, null);

        if(json == null) return null;

        Gson gson = new Gson();
        return gson.fromJson(json, Car.class);
    }
}
