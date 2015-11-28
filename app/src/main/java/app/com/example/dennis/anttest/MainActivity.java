package app.com.example.dennis.anttest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.RotateAnimation;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity {

    Intent i;
    TextView tvHeartBeatBPM;
    private SpeedometerGauge speedometer;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        speedometer = (SpeedometerGauge) findViewById(R.id.speedometer);

        // configure value range and ticks
        speedometer.setMaxSpeed(200);
        speedometer.setMajorTickStep(25);
        speedometer.setMinorTicks(5);
        // Add label converter
        speedometer.setLabelConverter(new SpeedometerGauge.LabelConverter() {
            @Override
            public String getLabelFor(double progress, double maxProgress) {
                return String.valueOf((int) Math.round(progress));
            }
        });
        // Configure value range colors
        speedometer.addColoredRange(50, 120, Color.GREEN);
        speedometer.addColoredRange(120, 150, Color.YELLOW);
        speedometer.addColoredRange(150, 200, Color.RED);
        speedometer.setUnitsText("BPM");
        speedometer.setSpeed(0,true);
        Log.v("from main thread", "going to start thread");



        onStartService();

    }
    // Launching the service
    public void onStartService() {
        i = new Intent(this, ANTReceive.class);
        i.putExtra("foo", "bar");
        startService(i);
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
            tvHeartBeatBPM = (TextView)findViewById(R.id.tvHeartBeatBPM);
            tvHeartBeatBPM.setText(heartBeatBPM);
            speedometer.setSpeed(Float.valueOf(heartBeatBPM));
            Log.d("MainActivity", "Received intent: " + heartBeatBPM);
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
