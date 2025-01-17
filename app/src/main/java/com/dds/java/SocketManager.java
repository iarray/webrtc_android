package com.dds.java;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.dds.App;
import com.dds.skywebrtc.AVEngineKit;
import com.dds.skywebrtc.CallSession;
import com.dds.voip.Utils;
import com.dds.voip.VoipReceiver;
import com.dds.webrtclib.ws.JavaWebSocket;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

/**
 * Created by dds on 2019/7/26.
 * android_shuai@163.com
 */
public class SocketManager implements IEvent {
    private final static String TAG = "dds_SocketManager";
    private DWebSocket webSocket;
    private int userState;
    private String myId;

    private Handler handler = new Handler(Looper.getMainLooper());

    private SocketManager() {

    }


    private static class Holder {
        private static SocketManager socketManager = new SocketManager();
    }

    public static SocketManager getInstance() {
        return Holder.socketManager;
    }

    public void connect(String url, String userId, int device) {
        if (webSocket == null || !webSocket.isOpen()) {
            URI uri;
            try {
                String urls = url + "/" + userId + "/" + device;
                uri = new URI(urls);
            } catch (URISyntaxException e) {
                e.printStackTrace();
                return;
            }
            webSocket = new DWebSocket(uri, this);
            // 设置wss
            if (url.startsWith("wss")) {
                try {
                    SSLContext sslContext = SSLContext.getInstance("TLS");
                    if (sslContext != null) {
                        sslContext.init(null, new TrustManager[]{new JavaWebSocket.TrustManagerTest()}, new SecureRandom());
                    }

                    SSLSocketFactory factory = null;
                    if (sslContext != null) {
                        factory = sslContext.getSocketFactory();
                    }

                    if (factory != null) {
                        webSocket.setSocket(factory.createSocket());
                    }
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                } catch (KeyManagementException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            // 开始connect
            webSocket.connect();
        }


    }

    public void unConnect() {
        if (webSocket != null) {
            webSocket.close();
            webSocket = null;
        }

    }

    @Override
    public void onOpen() {
        Log.i(TAG, "socket is open!");

    }

    @Override
    public void loginSuccess(String userId, String avatar) {
        Log.i(TAG, "loginSuccess:" + userId);
        myId = userId;
        userState = 1;
        if (iUserState != null && iUserState.get() != null) {
            iUserState.get().userLogin();
        }
    }


    public void createRoom(String room, int roomSize) {
        if (webSocket != null) {
            webSocket.createRoom(room, roomSize, myId);
        }

    }

    public void sendInvite(String room, String userId, boolean audioOnly) {
        if (webSocket != null) {
            webSocket.sendInvite(room, myId, userId, audioOnly);
        }
    }

    public void sendLeave(String room, String userId) {
        if (webSocket != null) {
            webSocket.sendLeave(room, userId);
        }
    }

    public void sendRingBack(String targetId) {
        if (webSocket != null) {
            webSocket.sendRing(myId, targetId);
        }
    }

    public void sendRefuse(String inviteId, int refuseType) {
        if (webSocket != null) {
            webSocket.sendRefuse(inviteId, myId, refuseType);
        }
    }

    public void sendCancel(String userId) {
        if (webSocket != null) {
            webSocket.sendCancel(myId, userId);
        }
    }

    public void sendJoin(String room) {
        if (webSocket != null) {
            webSocket.sendJoin(room, myId);
        }
    }


    public void sendMeetingInvite(String userList) {

    }

    public void sendOffer(String userId, String sdp) {
        if (webSocket != null) {
            webSocket.sendOffer(userId, sdp);
        }
    }

    public void sendAnswer(String userId, String sdp) {
        if (webSocket != null) {
            webSocket.sendAnswer(userId, sdp);
        }
    }

    public void sendIceCandidate(String userId, String id, int label, String candidate) {
        if (webSocket != null) {
            webSocket.sendIceCandidate(userId, id, label, candidate);
        }
    }


    @Override
    public void onInvite(String room, boolean audioOnly, String inviteId, String userList) {
        Intent intent = new Intent();
        intent.putExtra("room", room);
        intent.putExtra("audioOnly", audioOnly);
        intent.putExtra("inviteId", inviteId);
        intent.putExtra("userList", userList);
        intent.setAction(Utils.ACTION_VOIP_RECEIVER);
        intent.setComponent(new ComponentName(App.getInstance().getPackageName(), VoipReceiver.class.getName()));
        App.getInstance().sendBroadcast(intent);

    }

    @Override
    public void onCancel(String inviteId) {

    }

    @Override
    public void onRing(String userId) {
        handler.post(() -> {
            CallSession currentSession = AVEngineKit.Instance().getCurrentSession();
            if (currentSession != null) {
                currentSession.onRingBack();
            }
        });


    }

    @Override
    public void onPeers(String myId, String userId) {
        handler.post(() -> {

        });
        //自己进入了房间，然后开始发送offer
        CallSession currentSession = AVEngineKit.Instance().getCurrentSession();
        if (currentSession != null) {
            currentSession.onJoinHome(myId, userId);
        }
    }

    @Override
    public void onNewPeer(String userId) {
        handler.post(() -> {
            CallSession currentSession = AVEngineKit.Instance().getCurrentSession();
            if (currentSession != null) {
                currentSession.newPeer(userId);
            }
        });

    }


    @Override
    public void onReject(String userId, int type) {
        handler.post(() -> {

        });

    }

    @Override
    public void onOffer(String userId, String sdp) {
        handler.post(() -> {
            CallSession currentSession = AVEngineKit.Instance().getCurrentSession();
            if (currentSession != null) {
                currentSession.onReceiveOffer(userId, sdp);
            }
        });


    }

    @Override
    public void onAnswer(String userId, String sdp) {
        handler.post(() -> {
            CallSession currentSession = AVEngineKit.Instance().getCurrentSession();
            if (currentSession != null) {
                currentSession.onReceiverAnswer(userId, sdp);
            }
        });

    }

    @Override
    public void onIceCandidate(String userId, String id, int label, String candidate) {
        handler.post(() -> {
            CallSession currentSession = AVEngineKit.Instance().getCurrentSession();
            if (currentSession != null) {
                currentSession.onRemoteIceCandidate(userId, id, label, candidate);
            }
        });

    }

    @Override
    public void logout(String str) {
        Log.i(TAG, "logout:" + str);
        userState = 0;
        if (iUserState != null && iUserState.get() != null) {
            iUserState.get().userLogout();
        }
    }

    public int getUserState() {
        return userState;
    }


    private WeakReference<IUserState> iUserState;

    public void addUserStateCallback(IUserState userState) {
        iUserState = new WeakReference<>(userState);
    }

}
