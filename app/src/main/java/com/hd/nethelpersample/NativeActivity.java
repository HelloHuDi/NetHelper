package com.hd.nethelpersample;

import android.annotation.SuppressLint;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.hd.nethelper.NetHelper;
import com.hd.nethelper.NetObserver;
import com.hd.nethelper.NetWorkSpeedListener;
import com.hd.nethelper.NetworkListener;
import com.hd.nethelper.test.NetConnectionQuality;
import com.hd.nethelper.test.NetSpeedSampler;
import com.hd.nethelper.test.passive.NetSpeedPassiveSampler;
import com.hd.nethelper.test.ping.NetPingTest;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.NumberFormat;

public class NativeActivity extends AppCompatActivity implements NetworkListener {

    private TextView tvContent, tvNetSpeed, tvNetState;

    private EditText etInputIp;

    private ScrollView svContent;

    private boolean netAvailable = false;

    @Override
    public void isAvailable(boolean available) {
        Toast.makeText(this, "网络可用 ：$available", Toast.LENGTH_SHORT).show();
        netAvailable = available;
        if (!available)
            NetHelper.openWifiSetting(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvContent = findViewById(R.id.tvContent);
        etInputIp = findViewById(R.id.etInputIp);
        svContent = findViewById(R.id.svContent);
        tvNetSpeed = findViewById(R.id.tvNetSpeed);
        tvNetState = findViewById(R.id.tvNetState);
        NetObserver.addObserver(this);
        netAvailable = NetHelper.checkNetConnect(this);
        printContent("😀当前网络是否连接 ：" + netAvailable//
                             + "\n😀当前网络是否使用的手机网络 ：" + NetHelper.checkNetConnectByType(this, ConnectivityManager.TYPE_MOBILE)//
                             + "\n😀当前网络int类型 : " + NetHelper.getNetConnectType(this)//
                             + "\n😀当前网络string类型 : " + NetHelper.getNetConnectTypeStr(this)//
                             + "\n😀当前网络enum类型 : " + NetHelper.getNetConnectTypeInfo(this)//
                             + "\n😀当前网络ip地址 : " + NetHelper.getNetConnectAddress(this)//
                             + "\n😀当前设备所有wifi密码,设备需要root : " + NetHelper.getAllWifiPassword()//
                             + "\n😀当前设备连接的wifi的密码,设备需要root : " + NetHelper.getConnectWifiPassword(this));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // passive sampler
        if (netAvailable)
            setPassiveSampler();
    }

    public void checkExternalNetwork(View view) {
        Editable ip = etInputIp.getText();
        final String ipStr = TextUtils.isEmpty(ip) ? "www.baidu.com" : ip.toString();
        new Thread(new Runnable() {
            @Override
            public void run() {
                final boolean state = NetHelper.checkNetConnect(ipStr);
                printContent("\n😀ping " + ipStr + " = " + state);
            }
        }).start();

        //ping test
        new NetPingTest("www.baidu.com", 6, new NetPingTest.NetPingTestListener() {
            @SuppressLint("DefaultLocale")
            @Override
            public void reportPing(boolean finished, double instantRtt, double avgRtt) {
                if (finished) {
                    printContent(String.format("\n😀ping结果，是否结束：%b , 实时时长 %f ms ,最终时长 %f ms", true, instantRtt, avgRtt));
                }
            }
        }).start();
    }

    private NetConnectionQuality uq = NetConnectionQuality.UNKNOWN;
    private NetConnectionQuality dq = NetConnectionQuality.UNKNOWN;
    private void setPassiveSampler() {
        final double[] downLink = {0.0};
        final double[] upLink = {0.0};
        sampler = new NetSpeedPassiveSampler(this, new NetWorkSpeedListener() {
            @Override
            public void step(int step) {
                printContent("\n😀采样进度：$step");
            }

            @Override
            public void upLink(double up, @NotNull NetConnectionQuality upQuality) {
                upLink[0] = up;
                uq = upQuality;
                setSpeedContent(up, downLink[0]);
                setSpeedQualityContent(uq, dq);
            }

            @Override
            public void downLink(double down, @NotNull NetConnectionQuality downQuality) {
                downLink[0] = down;
                dq = downQuality;
                setSpeedContent(upLink[0], down);
                setSpeedQualityContent(uq, dq);
            }

            @Override
            public void error() {
                printContent("\n😀采样失败");
            }

            @Override
            public void finished() {
                printContent("\n😀采样结束");
            }
        });

        new DownloadImage().execute(mURL);
    }

    @SuppressLint("SetTextI18n")
    private void setSpeedContent(final Double upLink, final Double downLink) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvNetSpeed.setText(formatDouble(upLink) + "kbps/" + formatDouble(downLink) + "kbps");
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void setSpeedQualityContent(final NetConnectionQuality upQuality, final NetConnectionQuality downQuality) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvNetState.setText(upQuality + "/" + downQuality);
            }
        });
    }

    private String formatDouble(Double link) {
        NumberFormat df = java.text.NumberFormat.getNumberInstance();
        df.setMaximumFractionDigits(2);
        return df.format(link);
    }

    private Handler handler = new Handler();

    private void printContent(final String str) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvContent.append(str);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        svContent.fullScroll(ScrollView.FOCUS_DOWN);
                    }
                });
            }
        });
    }

    private final String mURL = "https://developers.google.cn/products/";

    private NetSpeedPassiveSampler sampler;

    private int mTries = 0;

    @SuppressLint("StaticFieldLeak")
    private class DownloadImage extends AsyncTask<String, Void, Void> {

        @Override
        protected void onPreExecute() {
            sampler.setCustomTestMode(NetSpeedSampler.DOWN_LINK_SAMPLING);
            sampler.startSampling();
        }

        @Override
        protected Void doInBackground(String... url) {
            String imageURL = url[0];
            try {
                // Open a stream to download the image from our URL.
                URLConnection connection = new URL(imageURL).openConnection();
                connection.setUseCaches(false);
                connection.connect();
                InputStream input = connection.getInputStream();
                try {
                    byte[] buffer = new byte[1024];
                    // Do some busy waiting while the stream is open.
                    while (input.read(buffer) != -1) { }
                } finally {
                    input.close();
                }
            } catch (IOException e) {
                Log.e("hd", "Error while downloading image.");
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void v) {
            sampler.stopSampling();
            // Retry for up to 10 times until we find a ConnectionClass.
            if (dq==NetConnectionQuality.UNKNOWN && mTries < 10) {
                mTries++;
                new DownloadImage().execute(mURL);
            }
        }
    }

}
