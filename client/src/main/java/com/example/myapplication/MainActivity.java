package com.example.myapplication;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.server.IRemoteInterface;
import com.example.server.IRemoteInterfaceCallback;
import com.example.server.Student;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {


    private Button btnBindService;
    private Button btnGetData;
    private Button btnRegister;
    private Button btnUnregister;
    private Button btnUnbindService;
    private Button btnKillProcess;
    private TextView textView;
    private boolean isRegistered = false;
    private IRemoteInterface remoteInterface;
    private static final int MSG_GET_DATA = 0;
    private static final int MSG_DATA_CHANGED = 1;

    //handler用于在主线程中更新UI
    private Handler handler = new Handler(Looper.myLooper()){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case MSG_GET_DATA:
                    //通过远程服务的getData方法获取数据
                    Toast.makeText(MainActivity.this, "Data: " + msg.arg1, Toast.LENGTH_LONG).show();
                    break;
                case MSG_DATA_CHANGED:
                    //远程服务通过客户端回调向客户端推送数据
                    textView.setText("Receive data from service: " + msg.arg1);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    };


    private ServiceConnection sc = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            Log.i("DemoLog", "MainActivity: ServiceConnection -> onServiceConnected");
            remoteInterface = IRemoteInterface.Stub.asInterface(binder);

            textView.setText("已连接到RemoteService");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i("DemoLog", "MainActivity: ServiceConnection -> onServiceDisconnected");
            remoteInterface = null;
            textView.setText("与RemoteService断开连接");
        }
    };

    //callback为客户端向RemoteService注册的回调接口
    private IRemoteInterfaceCallback callback = new IRemoteInterfaceCallback.Stub(){
        @Override
        public void dataChanged(int data) throws RemoteException {
            Log.i("DemoLog", "MainActivity: callback -> dataChanged, data: " + data + ", PID=" +  + android.os.Process.myPid() + ", Thread=" + Thread.currentThread().getName());
            Message msg = Message.obtain();
            msg.what = MSG_DATA_CHANGED;
            msg.arg1 = data;
            handler.sendMessage(msg);
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnBindService = (Button)findViewById(R.id.btnBindService);
        btnGetData = (Button)findViewById(R.id.btnGetData);
        btnRegister = (Button)findViewById(R.id.btnRegister);
        btnUnregister = (Button)findViewById(R.id.btnUnregister);
        btnUnbindService = (Button)findViewById(R.id.btnUnbindService);
        btnKillProcess = (Button)findViewById(R.id.btnKillProcess);
        textView = (TextView)findViewById(R.id.textView);
        btnBindService.setOnClickListener(this);
        btnGetData.setOnClickListener(this);
        btnRegister.setOnClickListener(this);
        btnUnregister.setOnClickListener(this);
        btnUnbindService.setOnClickListener(this);
        btnKillProcess.setOnClickListener(this);
        findViewById(R.id.updateAge).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btnBindService:
                bindService();
                break;
            case R.id.btnGetData:
                getData();
                break;
            case R.id.btnRegister:
                registerCallback();
                break;
            case R.id.btnUnregister:
                unregisterCallback();
                break;
            case R.id.btnUnbindService:
                unbindService();
                break;
            case R.id.btnKillProcess:
                killServiceProcess();
                break;
            case R.id.updateAge:
                if(remoteInterface == null){
                    return;
                }
                Student student=new Student(10);
                try {
                    remoteInterface.changePersonAge(student);
                    textView.setText("年龄："+student.getAge());
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    private void bindService(){
        if(remoteInterface != null){
            return;
        }
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.example.server","com.example.server.RemoteService"));
        bindService(intent, sc, BIND_AUTO_CREATE);
    }

    private void getData(){
        if(remoteInterface == null){
            return;
        }

        try{
            Log.i("DemoLog", "MainActivity -> getData");
            int data = remoteInterface.getData();
            Message msg = Message.obtain();
            msg.what = MainActivity.MSG_GET_DATA;
            msg.arg1 = data;
            handler.sendMessage(msg);
        }catch (RemoteException e){
            e.printStackTrace();
        }
    }

    private void registerCallback(){
        if(remoteInterface == null || isRegistered){
            return;
        }
        try{
            Log.i("DemoLog", "MainActivity -> registerCallback");
            //客户端向远程服务注册客户端回调
            remoteInterface.registerCallback(callback);
            isRegistered = true;

            //更新UI
            btnRegister.setEnabled(false);
            btnUnregister.setEnabled(true);
            Toast.makeText(this, "已向Service注册Callback", Toast.LENGTH_LONG).show();
        }catch (RemoteException e){
            e.printStackTrace();
        }
    }

    private void unregisterCallback(){
        if(remoteInterface == null || !isRegistered){
            return;
        }
        try{
            Log.i("DemoLog", "MainActivity -> unregisterCallback");
            //远程服务反注册客户端回调
            remoteInterface.unregisterCallback(callback);
            isRegistered = false;

            //更新UI
            btnRegister.setEnabled(true);
            btnUnregister.setEnabled(false);
        }catch (RemoteException e){
            e.printStackTrace();
        }
    }

    private void unbindService(){
        if(remoteInterface == null){
            return;
        }

        unregisterCallback();
        unbindService(sc);
        remoteInterface = null;

        textView.setText("与RemoteService断开连接");
    }

    private void killServiceProcess(){
        if(remoteInterface == null){
            return;
        }

        try{
            Log.i("DemoLog", "MainActivity -> killServiceProcess");
            //获取远程服务的进程ID，并杀死远程服务
            int pid = remoteInterface.getPid();
            android.os.Process.killProcess(pid);
            remoteInterface = null;
            //Service进程被杀死后，会触发onServiceDisconnected的执行
        }catch (RemoteException e){
            e.printStackTrace();
        }
    }
}