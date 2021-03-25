package com.example.server;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;

import java.util.Random;

/**
 * Created by ygj on 2021/3/25.
 */
public class RemoteService extends Service {

    private Random random = new Random();

    //callbacks存储了所有注册过的客户端回调
    private final RemoteCallbackList<IRemoteInterfaceCallback> callbacks = new RemoteCallbackList<IRemoteInterfaceCallback>();

    private static final int MSG_REPORT_DATA = 0;

    //该handler用来每隔一秒主动向所有注册过回调的客户端发送信息
    private Handler handler = new Handler(Looper.myLooper()){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case MSG_REPORT_DATA:
                    //开始广播，获取客户端的数量
                    final int n = callbacks.beginBroadcast();
                    int data = random.nextInt(100);
                    for(int i = 0; i < n; i++){
                        try{
                            //遍历客户单回调
                            IRemoteInterfaceCallback callback = callbacks.getBroadcastItem(i);
                            //执行我们自定义的dataChanged方法，客户端会受到信息
                            Log.i("DemoLog", "RemoteService: handleMessage -> callback.dataChanged(data), PID=" + android.os.Process.myPid() + ", Thread=" + Thread.currentThread().getName());
                            callback.dataChanged(data);
                        }catch (RemoteException e){
                            e.printStackTrace();
                        }
                    }
                    //结束广播
                    callbacks.finishBroadcast();
                    //构建新的Message，延迟1秒发送，这样handler每隔一秒都会受到Message
                    Message pendingMsg = obtainMessage(MSG_REPORT_DATA);
                    sendMessageDelayed(pendingMsg, 1000);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    };


    //我们要实现
    IRemoteInterface.Stub binder = new IRemoteInterface.Stub(){
        @Override
        public int getPid() throws RemoteException {
            return android.os.Process.myPid();
        }

        @Override
        public int getData() throws RemoteException {
            Log.i("DemoLog", "RemoteService: binder -> getData, PID=" + android.os.Process.myPid() + ", Thread=" + Thread.currentThread().getName());
            return random.nextInt(100);
        }

        @Override
        public void changePersonAge(Student p) throws RemoteException {
            p.setAge(100);
        }

        @Override
        public void registerCallback(IRemoteInterfaceCallback cb) throws RemoteException {
            Log.i("DemoLog", "RemoteService: binder -> registerCallback, PID=" + android.os.Process.myPid() + ", Thread=" + Thread.currentThread().getName());
            if(cb != null){
                //注册客户端回调
                callbacks.register(cb);
            }
        }

        @Override
        public void unregisterCallback(IRemoteInterfaceCallback cb) throws RemoteException {
            Log.i("DemoLog", "RemoteService: binder -> unregisterCallback, PID=" + android.os.Process.myPid() + ", Thread=" + Thread.currentThread().getName());
            if(cb != null){
                //反注册客户端回调
                callbacks.unregister(cb);
            }
        }
    };

    public RemoteService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        handler.sendEmptyMessage(MSG_REPORT_DATA);
        Log.i("DemoLog", "RemoteService -> onCreate, PID=" + android.os.Process.myPid() + ", Thread=" + Thread.currentThread().getName());
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        Log.i("DemoLog", "RemoteService -> onDestroy, PID=" + android.os.Process.myPid() + ", Thread=" + Thread.currentThread().getName());

        //反注册所有的客户端回调，并且不再接收新的客户端回调
        callbacks.kill();

        //移除pedding message，停止message循环，防止内存泄露
        handler.removeMessages(MSG_REPORT_DATA);

        super.onDestroy();
    }
}
