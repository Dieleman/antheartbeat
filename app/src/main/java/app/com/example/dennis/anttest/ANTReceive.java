package app.com.example.dennis.anttest;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.IntentService;
import android.app.Service;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Context;
import android.net.Uri;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.dsi.ant.plugins.antplus.pcc.AntPlusHeartRatePcc;
import com.dsi.ant.plugins.antplus.pcc.defines.DeviceState;
import com.dsi.ant.plugins.antplus.pcc.defines.EventFlag;
import com.dsi.ant.plugins.antplus.pcc.defines.RequestAccessResult;
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc;
import com.dsi.ant.plugins.antplus.pccbase.AsyncScanController;
import com.dsi.ant.plugins.antplus.pccbase.PccReleaseHandle;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumSet;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class ANTReceive extends Service {
    public static final String ACTION = "com.codepath.example.servicesdemo.MyTestService";

    ArrayList<AsyncScanController.AsyncScanResultDeviceInfo> mAlreadyConnectedDeviceInfos;
    ArrayList<AsyncScanController.AsyncScanResultDeviceInfo> mScannedDeviceInfos;
    ArrayAdapter<String> adapter_devNameList;
    ArrayAdapter<String> adapter_connDevNameList;

    AsyncScanController<AntPlusHeartRatePcc> hrScanCtrl;
    AntPlusHeartRatePcc hrPcc = null;
    protected PccReleaseHandle<AntPlusHeartRatePcc> releaseHandle = null;

    /** indicates how to behave if the service is killed */
    int mStartMode;

    /** interface for clients that bind */
    IBinder mBinder;

    /** indicates whether onRebind should be used */
    boolean mAllowRebind;

    public void onCreate() {
        super.onCreate();
        Log.d("ANTReceive", "Creating service");

    }

    /** The service is starting, due to a call to startService() */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("ANTReceive", "Starting service");
        initScanDisplay();
        handleReset();
        return mStartMode;
    }

    /** A client is binding to the service with bindService() */
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /** Called when all clients have unbound with unbindService() */
    @Override
    public boolean onUnbind(Intent intent) {
        return mAllowRebind;
    }

    /** Called when a client is binding to the service with bindService()*/
    @Override
    public void onRebind(Intent intent) {

    }

    private void initScanDisplay()
    {
        //setContentView(R.layout.activity_async_scan);
        mAlreadyConnectedDeviceInfos = new ArrayList<AsyncScanController.AsyncScanResultDeviceInfo>();
        mScannedDeviceInfos = new ArrayList<AsyncScanController.AsyncScanResultDeviceInfo>();

    }

    /**
     * Requests access to the given search result.
     * @param asyncScanResultDeviceInfo The search result to attempt to connect to.
     */
    protected void requestConnectToResult(final AsyncScanController.AsyncScanResultDeviceInfo asyncScanResultDeviceInfo)
    {

                 Toast.makeText(ANTReceive.this, "Found " + asyncScanResultDeviceInfo.getDeviceDisplayName() + " connecting..", Toast.LENGTH_LONG).show();
                //mTextView_Status.setText("Scanning for heart rate devices asynchronouly...");

                // mTextView_Status.setText("Connecting to " + asyncScanResultDeviceInfo.getDeviceDisplayName());
                releaseHandle = hrScanCtrl.requestDeviceAccess(asyncScanResultDeviceInfo,
                        new AntPluginPcc.IPluginAccessResultReceiver<AntPlusHeartRatePcc>()
                        {
                            @Override
                            public void onResultReceived(AntPlusHeartRatePcc result,
                                                         RequestAccessResult resultCode, DeviceState initialDeviceState)
                            {
                                if(resultCode == RequestAccessResult.SEARCH_TIMEOUT)
                                {
                                    //On a connection timeout the scan automatically resumes, so we inform the user, and go back to scanning
                                    Log.d("ANTReceive","Timed out attempting to connect, try again");
                                }
                                else
                                {
                                    //Otherwise the results, including SUCCESS, behave the same as
                                    base_IPluginAccessResultReceiver.onResultReceived(result, resultCode, initialDeviceState);
                                    hrScanCtrl = null;
                                }
                            }
                        }, base_IDeviceStateChangeReceiver);
    }



    //Receives state changes and shows it on the status display line
    protected AntPluginPcc.IDeviceStateChangeReceiver base_IDeviceStateChangeReceiver =
        new AntPluginPcc.IDeviceStateChangeReceiver()
        {
            @Override
            public void onDeviceStateChange(final DeviceState newDeviceState)
             {
                 //tv_status.setText(hrPcc.getDeviceName() + ": " + newDeviceState);
             }
        };

    protected AntPluginPcc.IPluginAccessResultReceiver<AntPlusHeartRatePcc> base_IPluginAccessResultReceiver =
            new AntPluginPcc.IPluginAccessResultReceiver<AntPlusHeartRatePcc>()
            {
                //Handle the result, connecting to events on success or reporting failure to user.
                @Override
                public void onResultReceived(AntPlusHeartRatePcc result, RequestAccessResult resultCode,
                                             DeviceState initialDeviceState)
                {
                   // showDataDisplay("Connecting...");
                    Toast.makeText(ANTReceive.this, "Connecting to device", Toast.LENGTH_SHORT).show();
                    Log.d("ANTReceive","Connecting..");
                    switch(resultCode)
                    {

                        case SUCCESS:
                            hrPcc = result;
                           // tv_status.setText(result.getDeviceName() + ": " + initialDeviceState);

                            //if(!result.supportsRssi()) tv_rssi.setText("N/A");
                            Log.d("ANTReceive","Found HR Device, succes!");
                            Toast.makeText(ANTReceive.this, "Connected to device " + result.getDeviceName(), Toast.LENGTH_SHORT).show();
                            subscribeToHrEvents();
                            break;
                        case CHANNEL_NOT_AVAILABLE:
                            Toast.makeText(ANTReceive.this, "Channel Not Available", Toast.LENGTH_SHORT).show();
                            //tv_status.setText("Error. Do Menu->Reset.");
                            break;
                        case ADAPTER_NOT_DETECTED:
                            Toast.makeText(ANTReceive.this, "ANT Adapter Not Available. Built-in ANT hardware or external adapter required.", Toast.LENGTH_SHORT).show();
                           // tv_status.setText("Error. Do Menu->Reset.");
                            break;
                        case BAD_PARAMS:
                            //Note: Since we compose all the params ourself, we should never see this result
                            Toast.makeText(ANTReceive.this, "Bad request parameters.", Toast.LENGTH_SHORT).show();
                            //tv_status.setText("Error. Do Menu->Reset.");
                            break;
                        case OTHER_FAILURE:
                            Toast.makeText(ANTReceive.this, "RequestAccess failed. See logcat for details.", Toast.LENGTH_SHORT).show();
                            //tv_status.setText("Error. Do Menu->Reset.");
                            break;
                        case DEPENDENCY_NOT_INSTALLED:
                           // tv_status.setText("Error. Do Menu->Reset.");
                            AlertDialog.Builder adlgBldr = new AlertDialog.Builder(ANTReceive.this);
                            adlgBldr.setTitle("Missing Dependency");
                            adlgBldr.setMessage("The required service\n\"" + AntPlusHeartRatePcc.getMissingDependencyName() + "\"\n was not found. You need to install the ANT+ Plugins service or you may need to update your existing version if you already have it. Do you want to launch the Play Store to get it?");
                            adlgBldr.setCancelable(true);
                            adlgBldr.setPositiveButton("Go to Store", new DialogInterface.OnClickListener()
                            {
                                @Override
                                public void onClick(DialogInterface dialog, int which)
                                {
                                    Intent startStore = null;
                                    startStore = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + AntPlusHeartRatePcc.getMissingDependencyPackageName()));
                                    startStore.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                                    ANTReceive.this.startActivity(startStore);
                                }
                            });
                            adlgBldr.setNegativeButton("Cancel", new DialogInterface.OnClickListener()
                            {
                                @Override
                                public void onClick(DialogInterface dialog, int which)
                                {
                                    dialog.dismiss();
                                }
                            });

                            final AlertDialog waitDialog = adlgBldr.create();
                            waitDialog.show();
                            break;
                        case USER_CANCELLED:
                            //tv_status.setText("Cancelled. Do Menu->Reset.");
                            break;
                        case UNRECOGNIZED:
                            Toast.makeText(ANTReceive.this,
                                    "Failed: UNRECOGNIZED. PluginLib Upgrade Required?",
                                    Toast.LENGTH_SHORT).show();
                            //tv_status.setText("Error. Do Menu->Reset.");
                            break;
                        default:
                            Toast.makeText(ANTReceive.this, "Unrecognized result: " + resultCode, Toast.LENGTH_SHORT).show();
                            //tv_status.setText("Error. Do Menu->Reset.");
                            break;
                    }
                }
            };
    /**
     * Requests the asynchronous scan controller
     */
    protected void requestAccessToPcc()
    {
        initScanDisplay();
        Log.d("RequestAccessToPcc", "Searching..");
        Toast.makeText(ANTReceive.this, "Searching for devices", Toast.LENGTH_SHORT).show();
        hrScanCtrl = AntPlusHeartRatePcc.requestAsyncScanController(this, 0,
                new AsyncScanController.IAsyncScanResultReceiver()
                {
                    @Override
                    public void onSearchStopped(RequestAccessResult reasonStopped)
                    {
                        Log.d("RequestAccessToPcc", "Search stopped.");
                        //The triggers calling this function use the same codes and require the same actions as those received by the standard access result receiver
                        base_IPluginAccessResultReceiver.onResultReceived(null, reasonStopped, DeviceState.DEAD);
                    }

                    @Override
                    public void onSearchResult(final AsyncScanController.AsyncScanResultDeviceInfo deviceFound)
                    {
                        Log.d("RequestAccessToPcc", "Has Search result.");
                        for(AsyncScanController.AsyncScanResultDeviceInfo i: mScannedDeviceInfos)
                        {
                            //The current implementation of the async scan will reset it's ignore list every 30s,
                            //So we have to handle checking for duplicates in our list if we run longer than that
                            if(i.getAntDeviceNumber() == deviceFound.getAntDeviceNumber())
                            {
                                //Found already connected device, ignore
                                return;
                            }
                        }

                        //We split up devices already connected to the plugin from un-connected devices to make this information more visible to the user,
                        //since the user most likely wants to be aware of which device they are already using in another app
                        if(deviceFound.isAlreadyConnected())
                        {
                            mAlreadyConnectedDeviceInfos.add(deviceFound);

                                    if(adapter_connDevNameList.isEmpty())   //connected device category is invisible unless there are some present
                                    {
                                        // findViewById(R.id.listView_AlreadyConnectedDevices).setVisibility(View.VISIBLE);
                                        // findViewById(R.id.textView_AlreadyConnectedTitle).setVisibility(View.VISIBLE);
                                    }
                                    // adapter_connDevNameList.add(deviceFound.getDeviceDisplayName());
                                    //adapter_connDevNameList.notifyDataSetChanged();
                                    Log.d("From ANT+ thread", "Found already connected device: " + deviceFound.getDeviceDisplayName());
                                    requestConnectToResult(mScannedDeviceInfos.get(0));


                        }
                        else
                        {
                            mScannedDeviceInfos.add(deviceFound);

                                    // adapter_devNameList.add(deviceFound.getDeviceDisplayName());
                                    //  adapter_devNameList.notifyDataSetChanged();
                                    Log.d("From ANT+ thread", "Found device: " + deviceFound.getDeviceDisplayName());
                                    requestConnectToResult(mScannedDeviceInfos.get(0));




                        }
                    }
                });
    }

    public void subscribeToHrEvents() {
        hrPcc.subscribeHeartRateDataEvent(new AntPlusHeartRatePcc.IHeartRateDataReceiver() {
            @Override
            public void onNewHeartRateData(final long estTimestamp, EnumSet<EventFlag> eventFlags,
                                           final int computedHeartRate, final long heartBeatCount,
                                           final BigDecimal heartBeatEventTime, final AntPlusHeartRatePcc.DataState dataState) {
                // Mark heart rate with asterisk if zero detected
                final String textHeartRate = String.valueOf(computedHeartRate)
                        + ((AntPlusHeartRatePcc.DataState.ZERO_DETECTED.equals(dataState)) ? "" : "");

                // Mark heart beat count and heart beat event time with asterisk if initial value
                final String textHeartBeatCount = String.valueOf(heartBeatCount)
                        + ((AntPlusHeartRatePcc.DataState.INITIAL_VALUE.equals(dataState)) ? "" : "");
                final String textHeartBeatEventTime = String.valueOf(heartBeatEventTime)
                        + ((AntPlusHeartRatePcc.DataState.INITIAL_VALUE.equals(dataState)) ? "" : "");
                Intent intent = new Intent("heartBeatChanged");
                intent.putExtra("heartBeatBPM", textHeartRate);
                //sendBroadcast(intent);
                LocalBroadcastManager.getInstance(ANTReceive.this).sendBroadcast(intent);
                Log.d("ANTReceive", "heartrate change detected: " + textHeartRate);
            }
        });
    }

    /**
     * Ensures our controller is closed whenever we exit
     */
    @Override
    public void onDestroy()
    {
        Log.d("ANTReceive", "Destroying service");
        if(hrScanCtrl != null)
        {
            hrScanCtrl.closeScanController();
            hrScanCtrl = null;
        }
        super.onDestroy();
    }

    protected void handleReset()
    {
        //Release the old access if it exists
        if(releaseHandle != null)
        {
            releaseHandle.close();
        }
        Log.d("ANTReceive", "Handle reset");
        requestAccessToPcc();
    }

    protected void onHandleIntent(Intent intent) {
        // Fetch data passed into the intent on start
        String val = intent.getStringExtra("foo");
        // Construct an Intent tying it to the ACTION (arbitrary event namespace)
        Intent in = new Intent(ACTION);
        // Put extras into the intent as usual
        in.putExtra("resultCode", Activity.RESULT_OK);
        in.putExtra("resultValue", "My Result Value. Passed in: " + val);
        // Fire the broadcast with intent packaged
        LocalBroadcastManager.getInstance(this).sendBroadcast(in);
        // or sendBroadcast(in) for a normal broadcast;
    }
}

