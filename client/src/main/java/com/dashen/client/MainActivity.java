package com.dashen.client;

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
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

/**
 * @author anbeibei
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private TextView tv_state;
    private TextView tv_message;
    private Button btn_send;
    private Button btn_invoke;
    private Button btn_bind;
    private Button btn_unbind;

    private static final String TAG = "MessengerActivity";
    private static final int MSG_FROM_CLIENT = 0x10001;
    private static final int MSG_FROM_SERVER = 0x10002;
    private boolean isConn;

    private static final String SERVER_PACKAGE_NAME = "com.dashen.server";

    /**
     * 服务端的Messenger，用来给服务端发送信息
     */
    private Messenger mService;

    /**
     * 客户端的Messenger，处理服务端返回的信息
     */
    private Messenger mGetReplyMessenger = new Messenger(new MessengerHandler());

    private ServiceConnection mConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = new Messenger(service);
            isConn = true;
            tv_state.setText("连接状态：connected!");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            isConn = false;
            tv_state.setText("连接状态：disconnected!");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tv_state = findViewById(R.id.tv_state);
        tv_message = findViewById(R.id.tv_message);
        btn_send = findViewById(R.id.btn_send);
        btn_invoke = findViewById(R.id.btn_invoke);
        btn_bind = findViewById(R.id.btn_bind);
        btn_unbind = findViewById(R.id.btn_unbind);
        initListener();
    }

    private void initListener() {
        btn_send.setOnClickListener(this);
        btn_invoke.setOnClickListener(this);
        btn_bind.setOnClickListener(this);
        btn_unbind.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_invoke:
                invokeApp();
                break;
            case R.id.btn_send:
                sendMsgToServer();
                break;
            case R.id.btn_bind:
                bindService();
                break;
            case R.id.btn_unbind:
                if (isConn) {
                    unbindService(mConn);
                }
                break;
        }
    }

    /**
     *  处理服务端返回的信息的Handler
     */
    private class MessengerHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_FROM_SERVER:
                    Book book = (Book) msg.getData().getParcelable("book");
                    tv_message.setText("接受到的消息：" + msg.getData().getString("reply")+ "  书名："+book.getName());
                    Log.e(TAG, "receive msg from Service:" + msg.getData().getString("reply"));
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    /**
     * 唤起服务端app
     */
    private void invokeApp() {
        if (myUtil.checkPackInfo(this, SERVER_PACKAGE_NAME)) {
            myUtil.openPackage(this, SERVER_PACKAGE_NAME);
        } else {
            Toast.makeText(this, "没有安装服务端app", Toast.LENGTH_SHORT).show();
            //TODO  下载安装操作
        }
    }

    /**
     * 绑定服务端app
     */
    private void bindService() {
        Intent intent = new Intent();
        intent.setAction("android.intent.action.MESSENGER");
        intent.setPackage(SERVER_PACKAGE_NAME);
        bindService(intent, mConn, Context.BIND_AUTO_CREATE);
    }

    /**
     * 发送消息给服务端
     */
    private void sendMsgToServer() {
        //设置消息
        Message msg = Message.obtain(null, MSG_FROM_CLIENT);
        Bundle data = new Bundle();
        data.putString("msg", "hello,我是远程客户端~");
        msg.setData(data);

        msg.replyTo = mGetReplyMessenger;

        if (isConn) {
            //往服务端发送消息
            try {
                mService.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isConn) {
            unbindService(mConn);
        }
    }
}
