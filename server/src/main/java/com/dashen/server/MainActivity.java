package com.dashen.server;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    private TextView tv_state;
    private TextView tv_message;
    private Button btn_send;

    private Messenger mService;
    private boolean isConn;

    private static final int MSG_FROM_SERVER = 0x10002;
    private static final int MSG_FROM_CLIENT_LOCAL = 0x10003;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tv_state = findViewById(R.id.tv_state);
        tv_message = findViewById(R.id.tv_message);
        btn_send = findViewById(R.id.btn_send);

        bindService();
        initListener();
    }

    private void initListener() {
        btn_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendToLocalService("你的消息我已收到");
            }
        });
    }

    private ServiceConnection mConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = new Messenger(service);
            isConn = true;
            tv_state.setText("本地服务连接状态：connected!");
            sendToLocalService("连接");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            isConn = false;
            tv_state.setText("本地服务连接状态：disconnected!");
        }
    };

    private void bindService() {
        Intent intent = new Intent(this, MessengerServer.class);
        bindService(intent, mConn, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mConn);
    }

    private void sendToLocalService(String string){
        Message replyMessage = Message.obtain(null, MSG_FROM_CLIENT_LOCAL);
        Bundle bundle = new Bundle();
        bundle.putParcelable("book",new Book("android 开发艺术探索"));
        bundle.putString("msgLocal", string);
        replyMessage.setData(bundle);

        replyMessage.replyTo = mGetReplyMessenger;

        if (isConn) {
            //往本地服务发送消息
            try {
                mService.send(replyMessage);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 处理本地服务返回信息的Messenger
     */
    private Messenger mGetReplyMessenger = new Messenger(new MessengerHandler());

    private class MessengerHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_FROM_SERVER:
                    tv_message.setText("接受到的消息：" + msg.getData().getString("reply"));
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
}
