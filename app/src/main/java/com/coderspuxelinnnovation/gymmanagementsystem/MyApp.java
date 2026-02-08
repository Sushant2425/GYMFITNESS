package com.coderspuxelinnnovation.gymmanagementsystem;

import android.app.Application;
import com.razorpay.Checkout;

public class MyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Checkout.preload(getApplicationContext());
    }
}
