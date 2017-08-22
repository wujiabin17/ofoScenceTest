package com.sprocomm.ofoscenetest;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Interpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.MapView;
import com.amap.api.maps.UiSettings;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.maps.model.animation.TranslateAnimation;
import com.sprocomm.ofoscenetest.utils.ConstValue;
import com.sprocomm.ofoscenetest.utils.ContastValue;
import com.sprocomm.ofoscenetest.utils.CoordinateUtil;
import com.sprocomm.ofoscenetest.utils.PrefUtils;
import com.sprocomm.ofoscenetest.utils.Utils;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;

import butterknife.ButterKnife;
import butterknife.InjectView;


public class MainActivity extends Activity implements View.OnClickListener, LocationSource, AMapLocationListener, TextWatcher {
    @InjectView(R.id.et_imei)
    EditText etImei;
    @InjectView(R.id.open_lock)
    Button openLock;
    @InjectView(R.id.btn_stop)
    Button btnStop;
    @InjectView(R.id.btn_voice)
    Button btnVoice;
    @InjectView(R.id.clear_map)
    Button clearMap;
    @InjectView(R.id.last_new)
    TextView lastNew;
    @InjectView(R.id.ll_open_lock_failed)
    LinearLayout llOpenLockFailed;
    @InjectView(R.id.map_view)
    MapView mapView;
    @InjectView(R.id.btn_xing_open_lock)
    Button btnXingOpenLock;
    @InjectView(R.id.tv_open_hint)
    TextView tvOpenHint;
    @InjectView(R.id.tv_wait_time)
    TextView tvWaitTime;
    @InjectView(R.id.ll_open_wait_server)
    LinearLayout llOpenWaitServer;

    private static final int RECEIVE_FROM_SERVER = 0x01;
    private static final int CHANGE_TEXT = 0x02;
    private static final int WAIT_SERVER_RETURN = 0x03;
    private static final int WAIT_CHANGE_TEXT = 0x04;
    private static final  int ERROR_CONNECT_TOAST = 0x05;
    private static final  int SEND_OK = 0x06;
    private int time_up = 0;
    private AMap aMap;
    private UiSettings mUiSettings;
    private Marker screenMarker;
    private OnLocationChangedListener mListener;
    private AMapLocationClient mLocationClient;
    private AMapLocationClientOption mLocationOption;
    private AMapLocation mMapLocation;
    private Marker marker;
    private MarkerOptions markerOption;
    private MapView mMapView;
    private Marker markerClose;
    private boolean mStopNow;
    private Context mContext;
    private boolean isOpenLock;
    private int waitTwoMinute;
    private Intent startServiceIntent;
    private LocationBroadcastReceiver locationBroadcastReceiver;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);
        openLock.setOnClickListener(this);
        btnVoice.setOnClickListener(this);
        String default1 = PrefUtils.getString(this, "123", null);
        etImei.setText(default1);
        etImei.setSelection(etImei.length());
        etImei.addTextChangedListener(this);
        etImei.setFilters(new InputFilter[]{new InputFilter.LengthFilter(12)});
        btnStop.setOnClickListener(this);
        clearMap.setOnClickListener(this);
        btnXingOpenLock.setOnClickListener(this);
        isOpenLock = true;
        waitTwoMinute = 0;
        mContext = this;
        startServiceIntent = new Intent(MainActivity.this,MyService.class);
        startService(startServiceIntent);
        IntentFilter filter = new IntentFilter();
        filter.addAction(Utils.ACTION_BROADCAST_IN_ACITIVITY);
        locationBroadcastReceiver = new LocationBroadcastReceiver();
        registerReceiver(locationBroadcastReceiver,filter);
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectDiskReads().detectDiskWrites().detectNetwork()
                .penaltyLog().build());
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects().detectLeakedClosableObjects()
                .penaltyLog().penaltyDeath().build());
        initMap();
        mMapView.onCreate(savedInstanceState);
    }

    private void initMap() {
        mMapView = (MapView) findViewById(R.id.map_view);
        aMap = mMapView.getMap();
        aMap.setMapType(AMap.MAP_TYPE_NORMAL);
        MyLocationStyle myLocationStyle = new MyLocationStyle();
        myLocationStyle.myLocationIcon(BitmapDescriptorFactory
                .fromResource(R.mipmap.gps_point));// 设置小蓝点的图标
        myLocationStyle.strokeColor(Color.argb(0, 0, 0, 0));// 设置圆形的边框颜色
        myLocationStyle.radiusFillColor(Color.argb(0, 0, 0, 0));// 设置圆形的填充颜色
        myLocationStyle.strokeWidth(0f);// 设置圆形的边框粗细
        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_SHOW);
        aMap.setMyLocationStyle(myLocationStyle);
        aMap.getUiSettings().setMyLocationButtonEnabled(true);
        // 设置定位监听
        aMap.setLocationSource(this);
        // 设置为true表示显示定位层并可触发定位，false表示隐藏定位层并不可触发定位，默认是false
        aMap.setMyLocationEnabled(true);
        // 设置定位的类型为定位模式，有定位、跟随或地图根据面向方向旋转几种
        mUiSettings = aMap.getUiSettings();
       /* aMap.setOnMapLoadedListener(new AMap.OnMapLoadedListener() {
            @Override
            public void onMapLoaded() {
                addMarkerInScreenCenter();
            }
        });
        // 设置可视范围变化时的回调的接口方法
        aMap.setOnCameraChangeListener(new AMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition position) {
            }

            @Override
            public void onCameraChangeFinish(CameraPosition postion) {
                //屏幕中心的Marker跳动
                startJumpAnimation();
            }
        });*/
    }

    @Override
    protected void onStart() {
        super.onStart();
        if ((PrefUtils.getInt(mContext, ContastValue.PREF_TIME, 0)) == 0) {
            llOpenLockFailed.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //在activity执行onSaveInstanceState时执行mMapView.onSaveInstanceState (outState)，保存地图当前的状态
        mMapView.onSaveInstanceState(outState);
    }

    /**
     * 屏幕中心marker 跳动
     */
    public void startJumpAnimation() {

        if (screenMarker != null) {
            //根据屏幕距离计算需要移动的目标点
            final LatLng latLng = screenMarker.getPosition();
            Point point = aMap.getProjection().toScreenLocation(latLng);
            point.y -= dip2px(this, 125);
            LatLng target = aMap.getProjection()
                    .fromScreenLocation(point);
            //使用TranslateAnimation,填写一个需要移动的目标点
            TranslateAnimation animation = new TranslateAnimation(target);
            animation.setInterpolator(new Interpolator() {
                @Override
                public float getInterpolation(float input) {
                    // 模拟重加速度的interpolator
                    if (input <= 0.5) {
                        return (float) (0.5f - 2 * (0.5 - input) * (0.5 - input));
                    } else {
                        return (float) (0.5f - Math.sqrt((input - 0.5f) * (1.5f - input)));
                    }
                }
            });
            //整个移动所需要的时间
            animation.setDuration(600);
            //设置动画
            screenMarker.setAnimation(animation);
            //开始动画
            screenMarker.startAnimation();

        } else {
            Log.e("ama", "screenMarker is null");
        }
    }

    //dip和px转换
    private static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }
/*
    private void addMarkerInScreenCenter() {
        LatLng latLng = aMap.getCameraPosition().target;
        Point screenPosition = aMap.getProjection().toScreenLocation(latLng);
        screenMarker = aMap.addMarker(new MarkerOptions()
                .anchor(0.5f, 0.5f)
                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.purple_pin)));
        screenMarker.setPositionByPixels(screenPosition.x, screenPosition.y);
    }*/


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_settings:
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
                return true;
        }
        return false;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.open_lock:
                mStopNow = false;
                PrefUtils.setString(this, "123", etImei.getText().toString());
                time_up = PrefUtils.getInt(mContext, ContastValue.PREF_TIME, 0);
                mHandler.sendEmptyMessage(WAIT_SERVER_RETURN);
                if (waitTwoMinute > 0) {
                    llOpenLockFailed.setVisibility(View.GONE);
                    isOpenLock = false;
                }else if(waitTwoMinute <= 0 || !llOpenWaitServer.isShown() && time_up > 0){
                    llOpenWaitServer.setVisibility(View.GONE);
                    if (etImei.getText().length() == 12) {
                        isOpenLock = false;
                        Intent intent = new Intent();
                        intent.setAction(Utils.ACTION_BROADCAST_IN_SERVICE);
                        intent.putExtra("cmd",1);
                        intent.putExtra("device",etImei.getText().toString());
                        sendBroadcast(intent);
                    } else {
                        Toast.makeText(this, "请输入12位device id", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            case R.id.btn_voice:
                if (etImei.getText().length() == 12) {
                    Intent intent = new Intent();
                    intent.setAction(Utils.ACTION_BROADCAST_IN_SERVICE);
                    intent.putExtra("cmd",2);
                    intent.putExtra("device",etImei.getText().toString());
                    sendBroadcast(intent);
                } else {
                    Toast.makeText(this, "请输入12位device id", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.btn_stop:
                mStopNow = true;
                openLock.setEnabled(true);
                lastNew.setText("...");
                llOpenLockFailed.setVisibility(View.GONE);
                break;
            case R.id.clear_map:
                aMap.clear();
                aMap.reloadMap();
                break;
            case R.id.btn_xing_open_lock:
                if(openLock.isEnabled()){
                    Intent intent = new Intent(MainActivity.this, CaptureActivity.class);
                    startActivityForResult(intent, ConstValue.REQUEST_FROM_MOBILE_BYCLE);
                }else{
                    showTip("正在开锁中，请等待！！");
                }

                break;
        }
    }


    public static byte[] toByteArray(String hexString) {
        if (TextUtils.isEmpty(hexString))
            throw new IllegalArgumentException("this hexString must not be empty");

        hexString = hexString.toLowerCase();
        final byte[] byteArray = new byte[hexString.length() / 2];
        int k = 0;
        for (int i = 0; i < byteArray.length; i++) {//因为是16进制，最多只会占用4位，转换成字节需要两个16进制的字符，高位在先
            byte high = (byte) (Character.digit(hexString.charAt(k), 16) & 0xff);
            byte low = (byte) (Character.digit(hexString.charAt(k + 1), 16) & 0xff);
            byteArray[i] = (byte) (high << 4 | low);
            k += 2;
        }
        return byteArray;
    }

    private static byte toByte(char c) {
        byte b = (byte) "0123456789ABCDEF".indexOf(c);
        return b;
    }


    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case RECEIVE_FROM_SERVER:
                    Log.d("wjb sprocomm", "bytes:" + (String)msg.obj);
                    byte[] mToByteArray = toByteArray((String)msg.obj);
                    if (mToByteArray.length > 3) {
                        if (mToByteArray[0] == 0x7e && mToByteArray[1] == 0x05) {
                            if (mToByteArray.length > 32) {
                                // Log.d("wjb sprocomm","lat:" + lat + "," + "lng:" + lng);
                                //01dc3811 073fd69b
                                try {
                                    isOpenLock = true;
                                    String latHexToString = formatByte2HexStr(mToByteArray[23]) + formatByte2HexStr(mToByteArray[24]) + formatByte2HexStr(mToByteArray[25]) + formatByte2HexStr(mToByteArray[26]);
                                    String lngHexToString = formatByte2HexStr(mToByteArray[27]) + formatByte2HexStr(mToByteArray[28]) + formatByte2HexStr(mToByteArray[29]) + formatByte2HexStr(mToByteArray[30]);
                                    int latToInt = Integer.parseInt(latHexToString, 16);
                                    int lngToInt = Integer.parseInt(lngHexToString, 16);
                                    if (latToInt == 0 || lngToInt == 0) {
                                        Toast.makeText(mContext, "锁定位错误", Toast.LENGTH_SHORT).show();
                                    }
                                    double lastLat = latToInt / Math.pow(10, 6);
                                    double lastLng = lngToInt / Math.pow(10, 6);
                                    Log.d("wjb sprocomm", "latToInt:" + latToInt + ",lastLat:" + lastLat);
                                    Log.d("wjb sprocomm", "lngToInt:" + lngToInt + ",lastLng:" + lastLng);
                                    LatLng latLng = new LatLng(lastLat, lastLng);
                                    addMarkersToMap(latLng);
                                }catch (Exception e){
                                    e.printStackTrace();
                                }
                            }
                        } else if (mToByteArray[0] == 0x7e && mToByteArray[1] == 0x02) {
                            if (mToByteArray.length > 30) {
                                try {
                                    isOpenLock = true;
                                    String latCloseHexToString = formatByte2HexStr(mToByteArray[21]) + formatByte2HexStr(mToByteArray[22]) + formatByte2HexStr(mToByteArray[23]) + formatByte2HexStr(mToByteArray[24]);
                                    String lngCloseHexToString = formatByte2HexStr(mToByteArray[25]) + formatByte2HexStr(mToByteArray[26]) + formatByte2HexStr(mToByteArray[27]) + formatByte2HexStr(mToByteArray[28]);
                                    int latToIntClose = Integer.parseInt(latCloseHexToString, 16);
                                    int lngToIntClose = Integer.parseInt(lngCloseHexToString, 16);
                                    if (latToIntClose == 0 || lngToIntClose == 0) {
                                        Toast.makeText(mContext, "锁定位错误", Toast.LENGTH_SHORT).show();
                                    }
                                    double lastLatClose = latToIntClose / Math.pow(10, 6);
                                    double lastLngClose = lngToIntClose / Math.pow(10, 6);
                                    LatLng latLngClose = new LatLng(lastLatClose, lastLngClose);
                                    addMarkersToMapCloseLock(latLngClose);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }

                    }
                    break;
                case CHANGE_TEXT:
                    if (mStopNow) {
                        lastNew.setText("...");
                        break;
                    }
                    if (!llOpenWaitServer.isShown() && time_up > 0) {
                        llOpenLockFailed.setVisibility(View.VISIBLE);
                        openLock.setEnabled(false);
                        lastNew.setText("" + time_up);
                        time_up--;
                    } else if (time_up == 0) {
                        openLock.setEnabled(true);
                        if (!mStopNow) {
                            openLock.callOnClick();
                        }
                    }
                    break;
                case WAIT_SERVER_RETURN:
                    if (!isOpenLock) {
                        openLock.setEnabled(false);
                        mStopNow = true;
                        llOpenLockFailed.setVisibility(View.GONE);
                        llOpenWaitServer.setVisibility(View.VISIBLE);
                        waitTwoMinute = 120;
                        time_up = 0;
                        new Thread(){
                            @Override
                            public void run() {
                                super.run();
                                try {
                                    while(waitTwoMinute != 0){
                                        sleep(1000);
                                        sendEmptyMessage(WAIT_CHANGE_TEXT);
                                    }
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }.start();
                    } else {
                        openLock.setEnabled(true);
                        llOpenWaitServer.setVisibility(View.GONE);
                    }
                    break;
                case WAIT_CHANGE_TEXT:
                    if(isOpenLock){
                        waitTwoMinute = 0;
                    }
                    if (waitTwoMinute > 0) {
                        openLock.setEnabled(false);
                        mStopNow = true;
                        llOpenLockFailed.setVisibility(View.GONE);
                        btnStop.setEnabled(false);
                        waitTwoMinute--;
                    } else if (waitTwoMinute == 0) {
                        llOpenWaitServer.setVisibility(View.GONE);
                        btnStop.setEnabled(true);
                        openLock.setEnabled(true);
                        mStopNow = false;
                        if(time_up ==0 ){
                            time_up = PrefUtils.getInt(mContext, ContastValue.PREF_TIME, 0);
                            new Thread() {
                                @Override
                                public void run() {
                                    super.run();
                                    while (time_up != 0) {
                                        try {
                                            sleep(1000);
                                            mHandler.sendEmptyMessage(CHANGE_TEXT);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            }.start();
                        }
                    }else{
                        waitTwoMinute = 0;
                    }
                    tvWaitTime.setText("" + waitTwoMinute);
                    break;
            }
        }
    };


    public static String formatByte2HexStr(byte value) {
        return String.format("%02X", value);
    }

    private void showTip(final String str) {
        Toast.makeText(MainActivity.this, str, Toast.LENGTH_SHORT).show();
    }

    private void showErrorConnectToast() {
        Toast.makeText(MainActivity.this, "没有连接到服务器", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mLocationClient.stopLocation();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
        stopService(startServiceIntent);
        unregisterReceiver(locationBroadcastReceiver);
        if (mLocationClient != null) {
            mLocationClient.onDestroy();
        }
    }

    @Override
    public void activate(OnLocationChangedListener listener) {
        mListener = listener;
        if (mLocationClient == null) {
            //初始化定位
            mLocationClient = new AMapLocationClient(this);
            //初始化定位参数
            mLocationOption = new AMapLocationClientOption();
            //设置定位回调监听
            mLocationClient.setLocationListener(this);
            //设置为高精度定位模式
            mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
            mLocationOption.setInterval(2000);
            //设置定位参数
            mLocationClient.setLocationOption(mLocationOption);
            // 此方法为每隔固定时间会发起一次定位请求，为了减少电量消耗或网络流量消耗，
            // 注意设置合适的定位时间的间隔（最小间隔支持为2000ms），并且在合适时间调用stopLocation()方法来取消定位请求
            // 在定位结束后，在合适的生命周期调用onDestroy()方法
            // 在单次定位情况下，定位无论成功与否，都无需调用stopLocation()方法移除请求，定位sdk内部会移除
            mLocationClient.startLocation();//启动定位
        }
    }

    @Override
    public void deactivate() {
        mListener = null;
        if (mLocationClient != null) {
            mLocationClient.stopLocation();
            mLocationClient.onDestroy();
        }
        mLocationClient = null;
    }

    /**
     * 在地图上添加marker
     */
    private void addMarkersToMap(LatLng latLng) {
        latLng = fromGpsToAmap(latLng);
        Log.i("simon", "--------------latlng:" + latLng);
        markerOption = new MarkerOptions().icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory
                .decodeResource(getResources(), R.mipmap.amap_start)))
                .position(latLng)
                .visible(true)
                .draggable(false);
        marker = aMap.addMarker(markerOption);
        aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17));
    }

    private void addMarkersToMapCloseLock(LatLng latLng) {
        latLng = fromGpsToAmap(latLng);
        Log.i("simon", "--------------latlng:" + latLng);
        markerOption = new MarkerOptions().icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory
                .decodeResource(getResources(), R.mipmap.amap_end)))
                .position(latLng)
                .visible(true)
                .draggable(false);
        markerClose = aMap.addMarker(markerOption);
        aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17));
    }

    /**
     * @param sourceLatLng:
     * @return
     */

    public LatLng fromGpsToAmap(LatLng sourceLatLng) {
        LatLng latLng = new LatLng(sourceLatLng.latitude, sourceLatLng.longitude);
        latLng = CoordinateUtil.transformFromWGSToGCJ(latLng);
        return latLng;
    }


    @Override
    public void onLocationChanged(AMapLocation amapLocation) {
        if (amapLocation != null) {
            if (amapLocation.getErrorCode() == 0) {
                mListener.onLocationChanged(amapLocation);
                mMapLocation = amapLocation;
                //定位成功回调信息，设置相关消息
                amapLocation.getLocationType();//获取当前定位结果来源，如网络定位结果，详见定位类型表
                amapLocation.getLatitude();//获取纬度
                amapLocation.getLongitude();//获取经度
                amapLocation.getAccuracy();//获取精度信息
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date date = new Date(amapLocation.getTime());
                df.format(date);//定位时间
                Log.d("simon", "amapLocation:" + amapLocation.getLatitude() + "," + amapLocation.getLongitude());
            } else {
                //定位失败时，可通过ErrCode（错误码）信息来确定失败的原因，errInfo是错误信息，详见错误码表。
                Log.e("AmapError", "location Error, ErrCode:"
                        + amapLocation.getErrorCode() + ", errInfo:"
                        + amapLocation.getErrorInfo());
            }
        }
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

    }

    @Override
    public void afterTextChanged(Editable editable) {
        etImei.setSelection(editable.length());
        if(!isOpenLock){
            waitTwoMinute =1;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case ConstValue.REQUEST_FROM_MOBILE_BYCLE:
                if(data != null){
                    String returnBycleId = data.getStringExtra(ConstValue.RETURN_BYCLE_ID);
                    if (returnBycleId != null && returnBycleId.length() == 12) {
                        PrefUtils.setString(this, "123", returnBycleId);
                        etImei.setText(returnBycleId + "");
                        openLock.callOnClick();
                    }
                }
        }
    }
    public class LocationBroadcastReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            if(null != intent){
                String buffer = intent.getStringExtra(Utils.SEND_BROADCAST_FOR_BUFFER);
                int sendOk= intent.getIntExtra(Utils.SEND_OK,-1);
                if(buffer != null){
                    Message message = new Message();
                    message.what = RECEIVE_FROM_SERVER;
                    message.obj = buffer;
                    mHandler.sendMessage(message);
                }
                if(sendOk != -1){
                    if(sendOk == Utils.SEND_OK_CMD){
                        Toast.makeText(mContext, "指令发送成功", Toast.LENGTH_SHORT).show();
                    }else if(sendOk == Utils.ERROR_CONNECT_TOAST_CMD){
                        showErrorConnectToast();
                    }
                }

            }
        }
    }

}
