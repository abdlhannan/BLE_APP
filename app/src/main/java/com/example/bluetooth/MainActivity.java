package com.example.bluetooth;


import androidx.appcompat.app.AppCompatActivity;
import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
public class MainActivity extends AppCompatActivity {

    BluetoothManager btManager;
    BluetoothAdapter btAdapter;
    BluetoothLeScanner btScanner;
    Button startScanningButton;
    Button stopScanningButton;
    //TextView peripheralTextView;
    private final static int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    public List<ScanFilter> filters;
    ScanFilter filter;
    ScanSettings scanSettings;
    private RequestQueue requestQueue;
    public ArrayList<Integer>AP1_rssi;
    public ArrayList<Integer>AP2_rssi;
    public ArrayList<Integer>AP3_rssi;

    private ImageView img;
    private ViewGroup rootLayout;
    private int xDelta;
    private int yDelta;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        rootLayout = (ViewGroup) findViewById(R.id.view_root);
        img = (ImageView) rootLayout.findViewById(R.id.imageView);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(100, 100);

        img.setLayoutParams(layoutParams);
        img.setOnTouchListener(new ChoiceTouchListener());


        filters = new ArrayList<ScanFilter>();
        ScanFilter AP1 = new ScanFilter.Builder().setDeviceAddress("BC:F3:10:BA:88:0F").build();
        ScanFilter AP2 = new ScanFilter.Builder().setDeviceAddress("BC:F3:10:BA:84:4F").build();
        ScanFilter AP3 = new ScanFilter.Builder().setDeviceAddress("BC:F3:10:20:79:8F").build();
        //AP1_rssi = new ArrayList<Integer>();
        //AP2_rssi = new ArrayList<Integer>();
        //AP3_rssi = new ArrayList<Integer>();
        //ScanFilter crap = new ScanFilter.Builder().setDeviceAddress("00:00:00:00:00:00").build();

        filters.add(AP1);
        filters.add(AP2);
        filters.add(AP3);
        //filters.add(crap);
        scanSettings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();



        //peripheralTextView = (TextView) findViewById(R.id.PeripheralTextView);
        //peripheralTextView.setMovementMethod(new ScrollingMovementMethod());

        startScanningButton = (Button) findViewById(R.id.StartScanButton);
        startScanningButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startScanning();
            }
        });

        stopScanningButton = (Button) findViewById(R.id.StopScanButton);
        stopScanningButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                stopScanning();
            }
        });
        stopScanningButton.setVisibility(View.INVISIBLE);

        btManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = btManager.getAdapter();
        btScanner = btAdapter.getBluetoothLeScanner();


        if (btAdapter != null && !btAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent,REQUEST_ENABLE_BT);
        }

        // Make sure we have access coarse location enabled, if not, prompt the user to enable it
        if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("This app needs location access");
            builder.setMessage("Please grant location access so this app can detect peripherals.");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                }
            });
            builder.show();
        }
    }

    private final class ChoiceTouchListener implements View.OnTouchListener {
        public boolean onTouch(View view, MotionEvent event) {
            final int X = (int) event.getRawX();
            final int Y = (int) event.getRawY();
            System.out.println("x position: " + X + "\n yposition: " + Y);
            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    RelativeLayout.LayoutParams lParams = (RelativeLayout.LayoutParams) view.getLayoutParams();
                    xDelta = X - lParams.leftMargin;
                    yDelta = Y - lParams.topMargin;
                    break;
                case MotionEvent.ACTION_UP:
                    break;
                case MotionEvent.ACTION_POINTER_DOWN:
                    break;
                case MotionEvent.ACTION_POINTER_UP:
                    break;
                case MotionEvent.ACTION_MOVE:
                    RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) view
                            .getLayoutParams();
                    layoutParams.leftMargin = X - xDelta;
                    layoutParams.topMargin = Y - yDelta;
                    //layoutParams.rightMargin = -250;
                    //layoutParams.bottomMargin = -250;
                    view.setLayoutParams(layoutParams);
                    break;
            }
            rootLayout.invalidate();
            return true;
        }


    }

    public void calculatePosition(float xAPI, float yAPI){
        final int DELTA = 140;
        int originX = 950;
        int originY = 270;

        float newX = (originX - (DELTA*xAPI));
        float newY = originY + (DELTA*yAPI);

        setPosition(newX,newY);
    }


    public void setPosition(float xPos, float yPos){
        //rescale for dp points
        xPos = xPos - 50;
        yPos = yPos - 230;
        System.out.println("ASDASD" + xPos + " " + yPos);
        img.setX(xPos);
        img.setY(yPos);
    }

    // Device scan callback.
    private ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            String ap1_str;
            String ap2_str;
            String ap3_str;

            //peripheralTextView.append("Device Name: " + result.getDevice() + " rssi: " + result.getRssi() + "\n");
            System.out.println((String.valueOf(result.getDevice())).getClass());
            //System.out.println("Device Name: " + result.getDevice() + " rssi: " + result.getRssi() + "\n");
            if ((String.valueOf(result.getDevice())).equals( "BC:F3:10:BA:88:0F")) {
                ap1_str = String.valueOf(result.getRssi());
                String data ="{" +
                       "\"AP1\":" + "\"" + ap1_str  + "\"" +  "}";
                //AP1_rssi.add(result.getRssi());
                System.out.println("ap1: " + ap1_str);
                Submit(data);
            }
            else if ((String.valueOf(result.getDevice())).equals("BC:F3:10:BA:84:4F")){
                ap2_str = String.valueOf(result.getRssi());
                String data ="{" +
                        "\"AP2\":" + "\"" + ap2_str  + "\"" +  "}";
                //AP2_rssi.add(result.getRssi());
                System.out.println("ap2: " + ap2_str);
                Submit(data);
            }
            else if((String.valueOf(result.getDevice())).equals("BC:F3:10:20:79:8F")){
                ap3_str = String.valueOf(result.getRssi());
                String data ="{" +
                        "\"AP3\":" + "\"" + ap3_str  + "\"" +  "}";
                //AP3_rssi.add(result.getRssi());
                System.out.println("ap3: " + ap3_str);
                Submit(data);
            }

        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    System.out.println("coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }

                    });
                    builder.show();
                }
                return;
            }
        }
    }

    public void startScanning() {
        System.out.println("start scanning");
        startScanningButton.setVisibility(View.INVISIBLE);
        stopScanningButton.setVisibility(View.VISIBLE);
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                btScanner.startScan(filters, scanSettings, leScanCallback);
            }
        });
    }

    public void stopScanning() {
        System.out.println("stopping scanning");
        startScanningButton.setVisibility(View.VISIBLE);
        stopScanningButton.setVisibility(View.INVISIBLE);
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                btScanner.stopScan(leScanCallback);
            }
        });
    }
    private void Submit(String data)
    {
        final String save_data= data;
        String URL="http://135.181.31.195:5000/test/";
        requestQueue = Volley.newRequestQueue(getApplicationContext());
        StringRequest stringRequest = new StringRequest(Request.Method.POST, URL, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject objres = new JSONObject(response);
                    if(objres.getString("X").equals("null") || objres.getString("Y").equals("null") ){
                        System.out.println("ifs");
                    }else{
                        System.out.println("dsfdsfsdfds");
                        calculatePosition(Float.valueOf(objres.getString("X")).floatValue(), Float.valueOf(objres.getString("Y")).floatValue());
                    }


                } catch (JSONException e) {
                    //Toast.makeText(getApplicationContext(),"Server Error",Toast.LENGTH_LONG).show();
                    //response_str = "Server Error";
                    ///Result.setText("Server Error");
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                //Toast.makeText(getApplicationContext(), error.getMessage(), Toast.LENGTH_SHORT).show();
                //response_str = error.getMessage();
                //Result.setText(error.getMessage());
                //Log.v("VOLLEY", error.toString());
            }
        }) {
            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }

            @Override
            public byte[] getBody() throws AuthFailureError {
                try {
                    return save_data == null ? null : save_data.getBytes("utf-8");
                } catch (UnsupportedEncodingException uee) {
                    //Log.v("Unsupported Encoding while trying to get the bytes", data);
                    return null;
                }
            }

        };
        requestQueue.add(stringRequest);
    }
}