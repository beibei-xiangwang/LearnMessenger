package com.dashen.server;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

/**
 * @author anbeibei
 */

public class MessengerServer extends Service {
    private static final String TAG = "MessengerServer";
    private static final int MSG_FROM_CLIENT = 0x10001;
    private static final int MSG_FROM_SERVER = 0x10002;
    private static final int MSG_FROM_CLIENT_LOCAL = 0x10003;
    /**
     * 远程客户端
     */
    private Messenger client;

    /**
     * 本地客户端
     */
    private Messenger clientLocal;

    /**
     * 用来处理客户端发送过来消息，从消息中取出客户端发来的信息再发送给另一个客户端，让两个客户端交互
     */
    private class MessengerHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_FROM_CLIENT:
                    Log.e(TAG, "receive msg from client:" + msg.getData().getString("msg"));
                    client = msg.replyTo;//存储远程客户端，用于回复使用

                    if (clientLocal != null) {
                        try {
                            Message replyMessage = Message.obtain(null, MSG_FROM_SERVER);
                            Bundle bundle = new Bundle();
                            bundle.putString("reply", msg.getData().getString("msg"));
                            replyMessage.setData(bundle);
                            clientLocal.send(replyMessage);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                case MSG_FROM_CLIENT_LOCAL:
                    Log.e(TAG, "receive msg from server:" + msg.getData().getString("msgLocal"));
                    clientLocal = msg.replyTo;//存储本地客户端，用于回复使用

                    if (client != null) {
                        try {
                            Message replyMessage = Message.obtain(null, MSG_FROM_SERVER);
                            Bundle bundle = new Bundle();
                            bundle.putString("reply", msg.getData().getString("msgLocal"));
                            bundle.putParcelable("book",msg.getData().getParcelable("book"));
                            replyMessage.setData(bundle);
                            client.send(replyMessage);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                default:
                    super.handleMessage(msg);
            }

        }
    }

    private final Messenger mMessenger = new Messenger(new MessengerHandler());

    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }
}