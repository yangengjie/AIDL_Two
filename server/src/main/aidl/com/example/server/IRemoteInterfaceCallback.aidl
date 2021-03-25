// IRemoteInterfaceCallback.aidl
package com.example.server;

 interface IRemoteInterfaceCallback {
    //关键字oneway表示该方法不会造成客户端阻塞等待服务端方法执行完成
    oneway void dataChanged(int data);
}