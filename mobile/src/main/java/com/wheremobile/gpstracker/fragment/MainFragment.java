package com.wheremobile.gpstracker.fragment;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.wheremobile.gpstracker.R;
import com.wheremobile.gpstracker.activity.SettingsActivity;
import com.wheremobile.gpstracker.app.ApplicationClass;
import com.wheremobile.gpstracker.config.Constants;
import com.wheremobile.gpstracker.model.BatteryModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.READ_PHONE_STATE;
import static android.content.Context.POWER_SERVICE;
import static com.wheremobile.gpstracker.R.id.location;
import static com.wheremobile.gpstracker.config.Constants.PATTERN_DATE;

public class MainFragment extends BaseFragment implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private TextView tvMobileKey,
            tvGps,
            tvInternet,
            tvDate,
            tvLocation,
            tvSpeed,
            tvAltitude,
            tvDirection,
            tvBattery,
            tvAccuracy;

    private TelephonyManager telephonyManager;
    private LocationManager locationManager;
    private GoogleMap mMap;
    GoogleApiClient mGoogleApiClient;

    Location mLastLocation;
    Marker mCurrLocationMarker = null;
    SupportMapFragment mapFragment;
    ImageView img_map_type;
    LinearLayout ll_map;
    int map_type = 1;

    private static final int MILLISECONDS_IN_SECOND = 1000;
    private static final float LOCATION_DISTANCE = 0.100f;


    private BroadcastReceiver serviceBroadcast = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Location location = intent.getParcelableExtra(Constants.EXTRA_LOCATION);
            reactToLocationChange(location);
            if (isNetworkAvailable() == true) {
                if (location != null) {
                    showGooglemap(location);
                }
            }
        }
    };

    BroadcastReceiver gpsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(LocationManager.PROVIDERS_CHANGED_ACTION)) {
                initLocationManager();
//              boolean isNetwork = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
                boolean isGPS = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
                tvGps.setText(String.format(getString(R.string.pattern_gps), isGPS ? "On" : "Off"));
            }
        }
    };

    BroadcastReceiver batteryLevelReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            int rawlevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            int level = -1;
            if (rawlevel >= 0 && scale > 0) {
                level = (rawlevel * 100) / scale;
            }
            reactBattery(new BatteryModel(level, status));
        }
    };
    private BottomSheetBehavior<View> sheetBehavior;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initTelephonyManager();
        initLocationManager();

    }

    private void initTelephonyManager() {
        if (checkPhonePermission()) {
            telephonyManager = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
        }
    }

    private void initLocationManager() {
        if (checkLocationPermission()) {
            locationManager = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        final ImageButton settings = view.findViewById(R.id.settings);
        settings.setOnClickListener(v -> {
                    sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    startActivity(new Intent(getActivity(), SettingsActivity.class));
                }
        );
        mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);

        LinearLayout ll = view.findViewById(R.id.bottomsheet);
        tvMobileKey = view.findViewById(R.id.mobile_key);
        sheetBehavior = BottomSheetBehavior.from(ll);
        sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        sheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                switch (newState) {
                    case BottomSheetBehavior.STATE_HIDDEN:
                        break;
                    case BottomSheetBehavior.STATE_EXPANDED: {
                        //btnBottomSheet.setText("Close Sheet");
                        settings.setVisibility(View.VISIBLE);
                        tvMobileKey.setVisibility(View.VISIBLE);
                    }
                    break;
                    case BottomSheetBehavior.STATE_COLLAPSED: {
                        settings.setVisibility(View.INVISIBLE);
                        tvMobileKey.setVisibility(View.INVISIBLE);
                        //btnBottomSheet.setText("Expand Sheet");
                    }
                    break;
                    case BottomSheetBehavior.STATE_DRAGGING:
                        break;
                    case BottomSheetBehavior.STATE_SETTLING:
                        break;
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {

            }
        });

        tvGps = view.findViewById(R.id.gps_status);
        tvInternet = view.findViewById(R.id.internet_status);
        tvDate = view.findViewById(R.id.date);
        tvLocation = view.findViewById(location);
        tvSpeed = view.findViewById(R.id.speed);
        tvAltitude = view.findViewById(R.id.altitude);
        tvDirection = view.findViewById(R.id.direction);
        tvBattery = view.findViewById(R.id.battery);
        tvAccuracy = view.findViewById(R.id.accuracy);
        //if(isNetworkAvailable()==true) {
        mapFragment.getMapAsync(googleMap -> {
            mMap = googleMap;
            if (mMap == null) {
                return;
            }

            try {
                // Customise the styling of the base map using a JSON object defined
                // in a raw resource file.
                boolean success = googleMap.setMapStyle(
                        MapStyleOptions.loadRawResourceStyle(
                                getActivity(), R.raw.stylish));

                if (!success) {
                    Log.e(">", "Style parsing failed.");
                }
            } catch (Resources.NotFoundException e) {
                Log.e(">", "Can't find style. Error: ", e);
            }

            googleMap.getUiSettings().setMyLocationButtonEnabled(false);

            mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            //Initialize Google Play Services
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ContextCompat.checkSelfPermission(getActivity(),
                        Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
                    buildGoogleApiClient();
                    //  mMap.setMyLocationEnabled(true);
                }
            } else {
                buildGoogleApiClient();
                // mMap.setMyLocationEnabled(true);
            }
//                    Location location = null;
//                    if (locationManager != null) {
//                        locationManager.requestLocationUpdates(
//                                LocationManager.GPS_PROVIDER,
//                                MILLISECONDS_IN_SECOND,
//                                LOCATION_DISTANCE,locationListeners[0]);
//                        Location gotLoc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
//                        if(gotLoc!=null) {
//                            showGooglemap(gotLoc);
//                        }
//                    }
        });
        //}

        if (locationManager != null) {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    MILLISECONDS_IN_SECOND,
                    LOCATION_DISTANCE, this);
            Location gotLoc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (gotLoc != null) {
                showGooglemap(gotLoc);
            }
        }

        img_map_type = view.findViewById(R.id.map_mode);
        img_map_type.setImageResource(R.mipmap.map_normal);
        ll_map = view.findViewById(R.id.ll_map);
        ll_map.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                switch (map_type) {
                    case 1:
                        mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                        img_map_type
                                .setImageResource(R.mipmap.map_terrain);
                        map_type = 2;
                        break;
                    case 2:
                        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                        img_map_type
                                .setImageResource(R.mipmap.map_hybrid);
                        map_type = 3;
                        break;
                    case 3:
                        mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                        img_map_type
                                .setImageResource(R.mipmap.map_satelite);
                        map_type = 4;
                        break;
                    case 4:
                        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                        img_map_type
                                .setImageResource(R.mipmap.map_normal);
                        map_type = 1;
                        break;
                    default:
                        break;
                }
            }
        });


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent();
            String packageName = getActivity().getPackageName();
            PowerManager pm = (PowerManager) getActivity().getSystemService(POWER_SERVICE);
            /*if (!pm.isIgnoringBatteryOptimizations(packageName)) {*/
            intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + packageName));
            startActivity(intent);
            /*}*/
        }

        return view;
    }

    @Override
    public void onLocationChanged(Location location) {

        //Location gotLoc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (location != null) {
            showGooglemap(location);
            reactToLocationChange(location);
        }

    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.d("Latitude", "disable");
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.d("Latitude", "enable");
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.d("Latitude", "status");
    }
//    private LocationListener[] locationListeners = new LocationListener[]{
//            new LocationListener(getActivity(), LocationManager.GPS_PROVIDER),
//            new LocationListener(getActivity(), LocationManager.NETWORK_PROVIDER)
//    };

    public void showGooglemap(Location location) {
        mLastLocation = location;

        if (mCurrLocationMarker != null) {
            mCurrLocationMarker.remove();
        }

        //int accuracy = (int) location.getAccuracy();
        //String accuracyString = String.format(getString(R.string.pattern_accuracy), accuracy);
        //tvAccuracy.setText(accuracyString + " meter(s)");
        //Place current location marker
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title("Current Position");
        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_marker));
        if (mMap != null) {
            mCurrLocationMarker = mMap.addMarker(markerOptions);

            //move map camera
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(15));
        }

    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    @SuppressWarnings("MissingPermission")
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat sdf = new SimpleDateFormat(Constants.PATTERN_DATE, Locale.getDefault());
        tvDate.setText(String.format(getString(R.string.pattern_date), sdf.format(date)));
        tvInternet.setText(String.format(getString(R.string.pattern_internet), isNetworkAvailable() ? "On" : "Off"));
        if (null != telephonyManager) {
            tvMobileKey.setText(String.format(getString(R.string.pattern_mobile_key), telephonyManager.getDeviceId()));
        }
        if (null != locationManager) {
            boolean isGPS = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            tvGps.setText(String.format(getString(R.string.pattern_gps), isGPS ? "On" : "Off"));
            Location location = null;
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    MILLISECONDS_IN_SECOND,
                    LOCATION_DISTANCE, this);
            location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            /*for (String provider : locationManager.getAllProviders()) {
                if (null != (location = locationManager.getLastKnownLocation(provider))) {*/
            reactToLocationChange(location);

            //    }
            //}
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(batteryLevelReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        getActivity().registerReceiver(gpsReceiver, new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION));
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext())
                .registerReceiver(serviceBroadcast, new IntentFilter(Constants.ACTION_SERVICE_DATA));

    }

    @Override
    public void onPause() {
        getActivity().unregisterReceiver(batteryLevelReceiver);
        getActivity().unregisterReceiver(gpsReceiver);
        //LocalBroadcastManager.getInstance(getActivity().getApplicationContext())
        //        .unregisterReceiver(serviceBroadcast);
        super.onPause();
    }

    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext())
                .unregisterReceiver(serviceBroadcast);
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case Constants.REQUEST_PERMISSION_MULTITASK:
                if (verifyPermissions(grantResults)) {
                    initTelephonyManager();
                    if (null != telephonyManager) {
                        tvMobileKey.setText(String.format(getString(R.string.pattern_mobile_key), telephonyManager.getDeviceId()));
                    }
                    if (ContextCompat.checkSelfPermission(getActivity(),
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {
                        buildGoogleApiClient();
                        //  mMap.setMyLocationEnabled(true);
                    }

                    if (locationManager != null) {
                        locationManager.requestLocationUpdates(
                                LocationManager.GPS_PROVIDER,
                                MILLISECONDS_IN_SECOND,
                                LOCATION_DISTANCE, this);
                        Location gotLoc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                        if (gotLoc != null) {
                            showGooglemap(gotLoc);
                        }
                    }

                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private boolean checkLocationPermission() {
        return (ActivityCompat.checkSelfPermission(activity, ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(activity, ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED);
    }

    private boolean checkPhonePermission() {
        return (ActivityCompat.checkSelfPermission(activity, READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED);
    }

    private void reactToLocationChange(@Nullable Location location) {

        if (null == location) return;
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();

        //  Toast.makeText(activity, "Location Changedd..!!", Toast.LENGTH_SHORT).show();

        tvInternet.setText(String.format(ApplicationClass.mContext.getString(R.string.pattern_internet), isNetworkAvailable() ? "On" : "Off"));
        SimpleDateFormat sdf = new SimpleDateFormat(PATTERN_DATE, Locale.getDefault());
        tvDate.setText(String.format(ApplicationClass.mContext.getString(R.string.pattern_date), sdf.format(new Date(System.currentTimeMillis()))));
        tvLocation.setText("" + latitude + " : " + longitude);
        if (location.hasSpeed()) {
            tvSpeed.setText(String.format(ApplicationClass.mContext.getString(R.string.pattern_speed), location.getSpeed() * 3.6f));
        }
        if (location.hasAltitude()) {
            tvAltitude.setText(String.format(ApplicationClass.mContext.getString(R.string.pattern_altitude), (double) location.getAltitude()));
        }
        if (location.hasBearing()) {
            tvDirection.setText(parseDegree(location.getBearing()));
        }


        try {
            if (location.hasAccuracy()) {
                int accuracy = (int) location.getAccuracy();
                String accuracyString = String.format(ApplicationClass.mContext.getString(R.string.pattern_accuracy), accuracy);
                tvAccuracy.setText(accuracyString + " meter(s)");
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void reactBattery(@Nullable BatteryModel battery) {
        if (null == battery) return;
        tvBattery.setText(parseBatteryStatus(battery.getStatus()) + ", " + battery.getLevel() + "%");
    }

    @Nullable
    private String parseDegree(float degree) {
        if (degree == 0 && degree < 45 || degree >= 315 && degree == 360) {
            return "Northbound";/*You are: */
        }
        if (degree >= 45 && degree < 90) {
            return "NorthEastbound";
        }
        if (degree >= 90 && degree < 135) {
            return "Eastbound";
        }
        if (degree >= 135 && degree < 180) {
            return "SouthEastbound";
        }
        if (degree >= 180 && degree < 225) {
            return "SouthWestbound";
        }
        if (degree >= 225 && degree < 270) {
            return "Westbound";
        }
        if (degree >= 270 && degree < 315) {
            return "NorthWestbound";
        }
        return null;
    }

    private String parseBatteryStatus(int status) {
        switch (status) {
            case BatteryManager.BATTERY_STATUS_UNKNOWN:
                return "Unknown";
            case BatteryManager.BATTERY_STATUS_CHARGING:
                return "Charging";
            case BatteryManager.BATTERY_STATUS_DISCHARGING:
                return "Discharging";
            case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
                return "Not charging";
            case BatteryManager.BATTERY_STATUS_FULL:
                return "Full";
            default:
                throw new IllegalArgumentException("Unknown status : " + status);
        }
    }

    private boolean isNetworkAvailable() {
      /*  ConnectivityManager connectivityManager = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return null != activeNetworkInfo && activeNetworkInfo.isConnectedOrConnecting();*/

        // Changed by Dhaval
        if (activity == null)
            return false;
        ConnectivityManager manager = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager == null)
            return false;

        NetworkInfo activeNetworkInfo = manager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        if (mLastLocation != null) {
            showGooglemap(mLastLocation);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}
