package com.hd.nethelpersample;

import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.hd.nethelper.NetHelper;

public class NativeActivity extends AppCompatActivity {

    private TextView tvContent;

    private EditText etInputIp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvContent = findViewById(R.id.tvContent);
        etInputIp = findViewById(R.id.etInputIp);
        printContent("😀当前网络是否连接 ：" + NetHelper.checkNetConnect(this)//
                             + "\n😀当前网络是否使用的手机网络 ：" + NetHelper.checkNetConnectByType(this, ConnectivityManager.TYPE_MOBILE)//
                             + "\n😀当前网络int类型 : " + NetHelper.getNetConnectType(this)//
                             + "\n😀当前网络string类型 : " + NetHelper.getNetConnectTypeStr(this)//
                             + "\n😀当前网络enum类型 : " + NetHelper.getNetConnectTypeInfo(this)//
                             + "\n😀当前网络ip地址 : " + NetHelper.getNetConnectAddress(this)//
                             + "\n😀当前设备所有wifi密码,设备需要root : " + NetHelper.getAllWifiPassword()//
                             + "\n😀当前设备连接的wifi的密码,设备需要root : " + NetHelper.getConnectWifiPassword(this));
    }

    public void checkExternalNetwork(View view) {
        Editable ip = etInputIp.getText();
        final String ipStr = TextUtils.isEmpty(ip) ? "www.baidu.com" : ip.toString();
        new Thread(new Runnable() {
            @Override
            public void run() {
                final boolean state = NetHelper.checkNetConnect(ipStr);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        printContent("\n😀ping " + ipStr + " = " + state);
                    }
                });
            }
        }).start();
    }

    private void printContent(String str) {
        tvContent.append(str);
    }

}
