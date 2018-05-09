package nr23730.lagekarte_trackerlight;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.StrictMode;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class BackgroundTracker extends Service {

    public Context context = this;
    Notification notification = null;
    private NotificationManager mNM;
    private int NOTIFICATION = R.string.app_name;
    private String account = "";

    public BackgroundTracker(Context applicationContext) {
        super();
    }

    public BackgroundTracker() {

    }

    @SuppressLint("WrongConstant")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        super.onStartCommand(intent, flags, startId);

        String started = "GPS-Tracker gestartet";
        CharSequence text = "Das Gerät ist jetzt in der Lagekarte verzeichnet.";

        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, BackgroundTracker.class), 0);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            notification = new Notification.Builder(this)
                    .setSmallIcon(R.drawable.ic_launcher_background)  // the status icon
                    .setTicker(text)  // the status text
                    .setWhen(System.currentTimeMillis())  // the time stamp
                    .setContentTitle(started)  // the label of the entry
                    .setContentText(text)  // the contents of the entry
                    .setContentIntent(contentIntent)  // The intent to send when the entry is clicked
                    .setChannelId("status")
                    .build();
        } else {
            notification = new Notification.Builder(this)
                    .setSmallIcon(R.drawable.ic_launcher_background)  // the status icon
                    .setTicker(text)  // the status text
                    .setWhen(System.currentTimeMillis())  // the time stamp
                    .setContentTitle(started)  // the label of the entry
                    .setContentText(text)  // the contents of the entry
                    .setContentIntent(contentIntent)  // The intent to send when the entry is clicked
                    .build();
        }

        startForeground(NOTIFICATION, notification);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        NotificationChannel mChannel = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            mChannel = new NotificationChannel("status", "Aktiver Status", NotificationManager.IMPORTANCE_MAX);
            mNM.createNotificationChannel(mChannel);
        }

        // Display a notification about us starting.  We put an icon in the status bar.
        //showNotification();

        final LocationListener mLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(final Location location) {
                System.out.println("Lat" + location.getLatitude() + ", Lon" + location.getLongitude());
                Toast.makeText(context, "GPS Tracker aktiv", Toast.LENGTH_LONG).show();

                SharedPreferences sharedPref = getSharedPreferences("TrackerSettings", MODE_PRIVATE);
                String deviceNumber = sharedPref.getString("DeviceNumber", "0");

                System.out.println("Device: " + deviceNumber);

                if (deviceNumber.equals("0"))
                    return;

                int batteryLevel = ((BatteryManager) getSystemService(BATTERY_SERVICE)).getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
                System.out.print("Battery:" + batteryLevel);

                URL url = null;
                try {
                    url = new URL("https://webapp.mobile-lagekarte.de/appservices/app-tracking-api/InsertData.php?id=H" + deviceNumber + "&lat=" + location.getLatitude() + "&long=" + location.getLongitude() + "&orga=" + account + "&plattform=golden-nougat-light&version=0.1&capacity=" + batteryLevel);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
                HttpsURLConnection urlConnection = null;
                try {
                    urlConnection = (HttpsURLConnection) url.openConnection();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                    readStream(in);
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    urlConnection.disconnect();
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {
                Toast.makeText(context, "GPS Tracker gestartet", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onProviderDisabled(String provider) {
                Toast.makeText(context, "GPS Tracker angehalten", Toast.LENGTH_LONG).show();
            }
        };

        LocationManager mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 60000,
                    0, mLocationListener);
        }

        return START_STICKY;

    }

    @Override
    public void onDestroy() {
        // Cancel the persistent notification.
        super.onDestroy();
        mNM.cancel(NOTIFICATION);
    }


    private void showNotification() {
        // In this sample, we'll use the same text for the ticker and the expanded notification


        // Set the info for the views that show in the notification panel

        // Send the notification.
        mNM.notify(NOTIFICATION, notification);
    }

    private String readStream(InputStream is) {
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            int i = is.read();
            while (i != -1) {
                bo.write(i);
                i = is.read();
            }
            return bo.toString();
        } catch (IOException e) {
            return "";
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}