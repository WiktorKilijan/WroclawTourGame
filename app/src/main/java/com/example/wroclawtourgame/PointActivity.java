package com.example.wroclawtourgame;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.wroclawtourgame.logic.TourWriter;
import com.example.wroclawtourgame.model.Cords;
import com.example.wroclawtourgame.model.Tour;
import com.example.wroclawtourgame.model.TourPoint;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.io.File;
import java.util.Optional;

public class PointActivity extends AppCompatActivity {

    private final int MAX_DISTANCE_ACCEPTED = 50;
    private int currentDistance = Integer.MAX_VALUE;

    Tour mTour;
    TourPoint mNextPoint;

    private TextView tvNextPoint, tvDistanceLeft;
    private Button bIAmHere;

    private FragmentManager mFragmentManager;
    private FragmentTransaction mFragmentTransaction;

    private MapFragment mMapFragment;
    private PointDescriptionFragment mPointDescriptionFragment;
    private QuestionFragment mQuestionFragment;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;


    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_point);

        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                mTour = (Tour) extras.getSerializable("tourObject");
            }
        }

        mFragmentManager = getSupportFragmentManager();

        tvNextPoint = findViewById(R.id.nextPointNameTextView);
        bIAmHere = findViewById(R.id.iAmHereButton);

        dealWithNewPoint();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void dealWithNewPoint() {
        Optional<TourPoint> optPoint = mTour.firstNotVisitedPoint();
        if (!optPoint.isPresent()) {
            startTourListActivityWithToast(getString(R.string.all_points_visited));
        } else {
            mNextPoint = optPoint.get();

            openMapFragment();

            tvNextPoint.setText(mNextPoint.getName());

            bIAmHere.setOnClickListener(v -> {
                bIAmHere.setClickable(false);
                if (isCloseEnough()) {
                    openPointDescriptionFragment();
                } else {
                    Toast.makeText(this, getString(R.string.you_have_to_be_closer) + MAX_DISTANCE_ACCEPTED + getString(R.string.m_from_the_point), Toast.LENGTH_SHORT).show();
                }

            });
            setDistanceLeftValue();
        }
    }

    private void setDistanceLeftValue() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }
        locationRequest = createLocationRequest();

        tvDistanceLeft = findViewById(R.id.distanceLeftValueTextView);

        locationCallback = new LocationCallback() {
            @SuppressLint({"DefaultLocale", "SetTextI18n"})
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    Cords currCords = new Cords(location.getLongitude(), location.getLatitude());
                    currentDistance = (int) currCords.getDistanceFrom(mNextPoint.getCords());

                    tvDistanceLeft.setText(currentDistance + " m");
                }
            }
        };

        fusedLocationClient.requestLocationUpdates(locationRequest,
                locationCallback,
                Looper.getMainLooper());
    }

    private LocationRequest createLocationRequest() {
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        return locationRequest;
    }

    private boolean isCloseEnough() {
//        return currentDistance <= MAX_DISTANCE_ACCEPTED;
        return true;
    }

    private void startTourListActivityWithToast(String toastText) {
        Toast.makeText(this, toastText, Toast.LENGTH_LONG).show();
        Intent intent = new Intent(this, ToursListActivity.class);
        startActivity(intent);
    }

    public void startGoToQuestionFragment() {
        mFragmentTransaction = mFragmentManager.beginTransaction();
        mQuestionFragment = QuestionFragment.newInstance(mNextPoint.getQuestion(), mNextPoint.getAnswer());
        mFragmentTransaction.replace(R.id.fragmentContainerView, mQuestionFragment);
        mFragmentTransaction.commit();
    }

    private void openMapFragment() {
        mFragmentTransaction = mFragmentManager.beginTransaction();
        mMapFragment = new MapFragment(mNextPoint);
        mFragmentTransaction.add(R.id.fragmentContainerView, mMapFragment);
        mFragmentTransaction.attach(mMapFragment);
        mFragmentTransaction.commit();
    }

    private void openPointDescriptionFragment() {
        mFragmentTransaction = mFragmentManager.beginTransaction();
        mPointDescriptionFragment = PointDescriptionFragment.newInstance(mNextPoint.getDescription());
        mFragmentTransaction.replace(R.id.fragmentContainerView, mPointDescriptionFragment);
        mFragmentTransaction.commit();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void pointFinished() {
        mNextPoint.markAsAnswered();
        setPointFinishedInFile();
        dealWithNewPoint();
    }

    private void setPointFinishedInFile() {
        TourWriter writer = new TourWriter();
        File fileToChange = new File(getExternalFilesDir(null), mTour.getFileName());
        writer.setFirstNotVisitedPointAsVisited(fileToChange);
    }

}