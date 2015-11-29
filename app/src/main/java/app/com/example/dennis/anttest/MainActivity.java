package app.com.example.dennis.anttest;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;


public class MainActivity extends AppCompatActivity {
    TextToSpeech t1;
    Intent i;
    TextView tvHeartBeatBPM;
    private SpeedometerGauge heartRateMeter, speedMeter;
    int imageResourceOff, imageResourceOn;
    long timeAnimationEnded;
    Animation mAnimation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        heartRateMeter = (SpeedometerGauge) findViewById(R.id.heartRateMeter);
        timeAnimationEnded = 0;
        // configure value range and ticks
        heartRateMeter.setMaxSpeed(200);
        heartRateMeter.setMajorTickStep(25);
        heartRateMeter.setMinorTicks(5);
        // Add label converter
        heartRateMeter.setLabelConverter(new SpeedometerGauge.LabelConverter() {
            @Override
            public String getLabelFor(double progress, double maxProgress) {
                return String.valueOf((int) Math.round(progress));
            }
        });
        // Configure value range colors
        heartRateMeter.addColoredRange(50, 120, Color.GREEN);
        heartRateMeter.addColoredRange(120, 150, Color.YELLOW);
        heartRateMeter.addColoredRange(150, 200, Color.RED);
        heartRateMeter.setUnitsText("BPM");
        heartRateMeter.setSpeed(0, true);

        speedMeter = (SpeedometerGauge) findViewById(R.id.speedMeter);

        // configure value range and ticks
        speedMeter.setMaxSpeed(60);
        speedMeter.setMajorTickStep(10);
        speedMeter.setMinorTicks(1);
        // Add label converter
        speedMeter.setLabelConverter(new SpeedometerGauge.LabelConverter() {
            @Override
            public String getLabelFor(double progress, double maxProgress) {
                return String.valueOf((int) Math.round(progress));
            }
        });
        // Configure value range colors
        speedMeter.addColoredRange(0, 20, Color.GREEN);
        speedMeter.addColoredRange(20, 30, Color.YELLOW);
        speedMeter.addColoredRange(30, 60, Color.RED);
        speedMeter.setUnitsText("km/h");
        speedMeter.setSpeed(0, true);


        t1=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    t1.setLanguage(Locale.UK);
                }
            }
        });

        Log.v("from main thread", "going to start thread");

        String uri = "@drawable/abc_btn_radio_to_on_mtrl_015";
        imageResourceOn = getResources().getIdentifier(uri, null, getPackageName());
        uri = "@drawable/abc_btn_radio_to_on_mtrl_000";
        imageResourceOff = getResources().getIdentifier(uri, null, getPackageName());
        mAnimation = new AlphaAnimation(0, 1);
        initLocationStuff();
        onStartService();

    }

    // Launching the service
    public void onStartService() {
        i = new Intent(this, ANTReceive.class);
        i.putExtra("foo", "bar");
        startService(i);
    }

    @TargetApi(Build.VERSION_CODES.M)
    public void initLocationStuff() {
        // Don't initialize location manager, retrieve it from system services.
        LocationManager locationManager = (LocationManager) this
                .getSystemService(Context.LOCATION_SERVICE);

        LocationListener locationListener = new LocationListener() {

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                Log.d("MainActivity", "Location status changed " + provider + " " + status);
            }

            @Override
            public void onProviderEnabled(String provider) {
                Toast.makeText(MainActivity.this,
                        "Provider enabled: " + provider, Toast.LENGTH_SHORT)
                        .show();
                Log.d("MainActivity", "Location provider enabled " + provider);
            }

            @Override
            public void onProviderDisabled(String provider) {
                Toast.makeText(MainActivity.this,
                        "Provider disabled: " + provider, Toast.LENGTH_SHORT)
                        .show();
                Log.d("MainActivity", "Location provider disabled " + provider);
            }

            @Override
            public void onLocationChanged(Location location) {
                Toast.makeText(MainActivity.this,
                        "Location changed acc: " + location.getAccuracy(), Toast.LENGTH_SHORT)
                        .show();
                Log.d("MainActivity", "Location changed: " + location);
                double speed = location.getSpeed() * 3.6;
                speedMeter.setUnitsText(String.valueOf(speed));
                speedMeter.setSpeed(speed);
                // Do work with new location. Implementation of this method will be covered later.
                //doWorkWithNewLocation(location);
            }
        };

        long minTime = 1000; // Minimum time interval for update in seconds, i.e. 5 seconds.
        long minDistance = 1; // Minimum distance change for update in meters, i.e. 10 meters.

// Assign LocationListener to LocationManager in order to receive location updates.
// Acquiring provider that is used for location updates will also be covered later.
// Instead of LocationListener, PendingIntent can be assigned, also instead of
// provider name, criteria can be used, but we won't use those approaches now.
        if (this.checkCallingOrSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && this.checkCallingOrSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    public void requestPermissions(@NonNull String[] permissions, int requestCode)
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for Activity#requestPermissions for more details.
            Log.d("MainActivity", "No location permission");
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime,
                minDistance, locationListener);
        Log.d("MainActivity", "Location permission fine");
    }


    @Override
    protected void onDestroy(){
        super.onDestroy();
        stopService(i);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Register for the particular broadcast based on ACTION string
        IntentFilter filter = new IntentFilter("heartBeatChanged");
        LocalBroadcastManager.getInstance(this).registerReceiver(antReceiver, filter);
        // or `registerReceiver(testReceiver, filter)` for a normal broadcast
    }

   // Define the callback for what to do when data is received
    private BroadcastReceiver antReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String heartBeatBPM = intent.getStringExtra("heartBeatBPM");
            boolean heartBeatIsReliable = intent.getBooleanExtra("heartBeatIsReliable", false);
            //Log.d("MainActivity", "Received intent: " + heartBeatBPM + " is valid: " + heartBeatIsReliable);

            heartRateMeter.setSpeed(Float.valueOf(heartBeatBPM));
            ImageView blinkingButton = (ImageView) findViewById(R.id.blinkingButton);
            if(heartBeatIsReliable) {
                blinkingButton.setImageResource(imageResourceOn);
                double duration = 1 / (Float.valueOf(heartBeatBPM) / 60.0) * 1000 / 2;
                mAnimation.setDuration((long) duration);
                heartRateMeter.setUnitsText(heartBeatBPM);
                if (!mAnimation.hasStarted() || mAnimation.hasEnded()) {
                    mAnimation.setInterpolator(new LinearInterpolator());
                    mAnimation.setRepeatCount(Animation.INFINITE);
                    mAnimation.setRepeatMode(Animation.REVERSE);
                    blinkingButton.startAnimation(mAnimation);
                    Log.d("MainActivity", "Start animation duration: " + duration);
                }
                //if(!t1.isSpeaking())
                //    t1.speak(heartBeatBPM + "beats per minute", TextToSpeech.QUEUE_FLUSH, null);
            }
                else {
                heartRateMeter.setUnitsText("BPM");
                blinkingButton.setImageResource(imageResourceOff);
                blinkingButton.clearAnimation();
            }



            //blinkingButton.setImageResource(imageResourceOn);
            //blinkingButton.setImageResource(imageResourceOff);
            //RotateAnimation
        }
};


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
