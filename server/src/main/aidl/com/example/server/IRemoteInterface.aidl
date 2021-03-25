// IRemoteInterface.aidl
package com.example.server;

import com.example.server.Student;
import com.example.server.IRemoteInterfaceCallback;

interface IRemoteInterface {

   //获取Service运行的进程ID
   int getPid();

   //从Service中获取最新的数据
   int getData();

    void changePersonAge(in Student p);

   //通过向Service中注册回调，可以实现Service主动向客户端推送数据
   void registerCallback(IRemoteInterfaceCallback cb);

   //删除注册的回调
   void unregisterCallback(IRemoteInterfaceCallback cb);
}