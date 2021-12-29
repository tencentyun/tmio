package com.tencent.xbright.tmio_demo;

import android.os.Bundle;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.tencent.xbright.tmio_demo.config.ConfigSharePref;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

public class TmioDemo extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tmio_demo);
        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_proxy)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);
        ConfigSharePref.initAppConfig(this);
    }

    public static String getTmioVersion() {
        return getSdkVersion();
    }

    static {
        System.loadLibrary("tmiojni");
    }

    private static native String getSdkVersion();
}