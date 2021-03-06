package com.angelatech.yeyelive.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.PermissionChecker;
import android.support.v4.view.ViewPager;
import android.view.KeyEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.angelatech.yeyelive.CommonResultCode;
import com.angelatech.yeyelive.CommonUrlConfig;
import com.angelatech.yeyelive.GlobalDef;
import com.angelatech.yeyelive.R;
import com.angelatech.yeyelive.activity.base.BaseActivity;
import com.angelatech.yeyelive.activity.function.ChatManager;
import com.angelatech.yeyelive.activity.function.ChatRoom;
import com.angelatech.yeyelive.adapter.MyFragmentPagerAdapter;
import com.angelatech.yeyelive.application.App;
import com.angelatech.yeyelive.db.BaseKey;
import com.angelatech.yeyelive.db.model.BasicUserInfoDBModel;
import com.angelatech.yeyelive.fragment.CallFragment;
import com.angelatech.yeyelive.fragment.LiveFinishFragment;
import com.angelatech.yeyelive.fragment.ReadyLiveFragment;
import com.angelatech.yeyelive.model.BarInfoModel;
import com.angelatech.yeyelive.model.ChatLineModel;
import com.angelatech.yeyelive.model.CommonListResult;
import com.angelatech.yeyelive.model.CommonModel;
import com.angelatech.yeyelive.model.GiftAnimationModel;
import com.angelatech.yeyelive.model.GiftModel;
import com.angelatech.yeyelive.model.OnlineListModel;
import com.angelatech.yeyelive.model.RoomModel;
import com.angelatech.yeyelive.qiniu.QiniuUpload;
import com.angelatech.yeyelive.service.IServiceValues;
import com.angelatech.yeyelive.socket.room.ServiceManager;
import com.angelatech.yeyelive.util.CacheDataManager;
import com.angelatech.yeyelive.util.JsonUtil;
import com.angelatech.yeyelive.util.LivePush.LivePush;
import com.angelatech.yeyelive.util.PictureObtain;
import com.angelatech.yeyelive.util.SPreferencesTool;
import com.angelatech.yeyelive.util.ScreenUtils;
import com.angelatech.yeyelive.util.StartActivityHelper;
import com.angelatech.yeyelive.util.UriHelper;
import com.angelatech.yeyelive.util.VerificationUtil;
import com.angelatech.yeyelive.view.ActionSheetDialog;
import com.angelatech.yeyelive.view.CommChooseDialog;
import com.angelatech.yeyelive.view.CommDialog;
import com.angelatech.yeyelive.view.FrescoBitmapUtils;
import com.angelatech.yeyelive.view.GaussAmbiguity;
import com.angelatech.yeyelive.view.LoadingDialogNew;
import com.angelatech.yeyelive.view.NomalAlertDialog;
import com.angelatech.yeyelive.web.HttpFunction;
import com.framework.socket.model.SocketConfig;
import com.google.gson.reflect.TypeToken;
import com.will.common.log.DebugLogs;
import com.will.common.tool.network.NetWorkUtil;
import com.will.common.tool.time.DateTimeTool;
import com.will.libmedia.MediaCenter;
import com.will.libmedia.MediaNative;
import com.will.libmedia.OnLiveListener;
import com.will.libmedia.OnPlayListener;
import com.will.view.ToastUtils;
import com.will.web.handle.HttpBusinessCallback;

import org.cocos2dx.lib.util.Cocos2dxGift;
import org.cocos2dx.lib.util.Cocos2dxGiftCallback;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 视频直播主界面
 */
public class ChatRoomActivity extends BaseActivity implements CallFragment.OnCallEvents, ReadyLiveFragment.OnCallEvents {
    //权限检测
    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final String[] permissionManifest = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
    };
    private Boolean boolCloseRoom = false;
    private CallFragment callFragment;//房间操作
    private ReadyLiveFragment readyLiveFragment = null;//准备播放页面
    private ImageView face;
    private ImageView room_guide;
    private RelativeLayout viewPanel, body;
    private ViewPager mAbSlidingTabView;
    private ServiceManager serviceManager;
    private RoomModel roomModel;                                  //房间信息，其中包括房主信息

    private ChatManager chatManager;
    private ArrayList<Fragment> fragmentList;
    private MyFragmentPagerAdapter fragmentPagerAdapter;
    private int beginTime = 0;          //房间直播开始时间，用来计算房间直播时长

    private BasicUserInfoDBModel userModel;  //登录用户信息
    private BasicUserInfoDBModel liveUserModel; //直播用户信息

    //重连的次数
    private int connectionServiceNumber = 0;
    private int rtmpConnectNumber = 0; //流媒体连接次数
    private LoadingDialogNew LoadingDialog;
    //房间是否初始化
    private boolean isInit = false;
    private boolean isSysMsg = false;
    private static boolean isCloseLiveDialog = false;
    private LiveFinishFragment liveFinishFragment;
    private TimeCount timeCount;
    private long BigData = 0;
    private boolean isbigData = false;
    private SurfaceView camera_surface;
    private boolean isStart = false;
    private List<Cocos2dxGift.Cocos2dxGiftModel> bigGift = new ArrayList<>();
    private ChatRoom chatRoom;
    public LivePush livePush = null;
    private int connTotalNum = 0; //总连接次数
    public boolean isqupai = true;
    private boolean boolConnRoom = true; //
    private String watemarkUrl = "wartermark/bg_room_mercury.png";
    private QiniuUpload qiNiuUpload;
    private PictureObtain mObtain;
    private Uri distUri;
    private String imgPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_room);
        if (Build.VERSION.SDK_INT >= 23) {
            permissionCheck();
        }
        initView();
        findView();
        //魅族适配
        if (Build.BRAND.equals("Meizu")) {
            body.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
        //趣拍初始化
        if (isqupai) {
            livePush = new LivePush();
            livePush.setWatermark(watemarkUrl, 14, 55, 1);
            livePush.init(this, camera_surface);
        }
        App.chatRoomApplication = this;
        //屏幕的计算
        int statusBarHeight = ScreenUtils.getStatusHeight(this);
        ViewGroup.LayoutParams params2 = body.getLayoutParams();
        params2.height = App.screenDpx.heightPixels - statusBarHeight;
        params2.width = App.screenDpx.widthPixels;
        body.setLayoutParams(params2);
    }


    private void permissionCheck() {
        int permissionCheck = PackageManager.PERMISSION_GRANTED;
        for (String permission : permissionManifest) {
            if (PermissionChecker.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionCheck = PackageManager.PERMISSION_DENIED;
            }
        }
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, permissionManifest, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                for (int i = 0; i < permissions.length; i++) {
                    if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                        int toastTip = 0;
                        if (Manifest.permission.CAMERA.equals(permissions[i])) {
                            toastTip = R.string.no_camera_permission;
                        } else if (Manifest.permission.RECORD_AUDIO.equals(permissions[i])) {
                            toastTip = R.string.no_record_audio_permission;
                        }
                        if (toastTip != 0) {
                            Toast.makeText(this, toastTip, Toast.LENGTH_SHORT).show();
                        }
                    }
                }
                break;
        }
    }

    private void initView() {
        Cocos2dxGiftCallback.onCreate(uiHandler);
        userModel = CacheDataManager.getInstance().loadUser();
        LoadingDialog = new LoadingDialogNew();
        chatRoom = new ChatRoom(this);
        camera_surface = (SurfaceView) findViewById(R.id.camera_surface);
        viewPanel = (RelativeLayout) findViewById(R.id.view);
        body = (RelativeLayout) findViewById(R.id.body);
        ImageView button_call_disconnect = (ImageView) findViewById(R.id.button_call_disconnect);
        face = (ImageView) findViewById(R.id.face);
        room_guide = (ImageView) findViewById(R.id.room_guide);
        mAbSlidingTabView = (ViewPager) findViewById(R.id.mAbSlidingTabView);
        room_guide.setOnClickListener(this);
        button_call_disconnect.setOnClickListener(this);

        qiNiuUpload = new QiniuUpload(this);
        mObtain = new PictureObtain();
    }

    private void findView() {
        if (App.roomModel.getUserInfoDBModel() != null) {
            roomModel = App.roomModel;
            liveUserModel = roomModel.getUserInfoDBModel();
        } else {
            finish();
            return;
        }
        connectionServiceNumber = 0;
        isInit = false;
        isCloseLiveDialog = false;
        chatManager = new ChatManager(this);
        fragmentList = new ArrayList<>();
        callFragment = new CallFragment();
        readyLiveFragment = new ReadyLiveFragment();
        fragmentList.add(readyLiveFragment);
        fragmentPagerAdapter = new MyFragmentPagerAdapter(this.getSupportFragmentManager(), fragmentList);
        mAbSlidingTabView.setAdapter(fragmentPagerAdapter);
        mAbSlidingTabView.setCurrentItem(0);
        //清空聊天记录
        App.mChatlines.clear();
        FrescoBitmapUtils.getImageBitmap(App.getInstance(), VerificationUtil.getImageUrl100(liveUserModel.headurl), new FrescoBitmapUtils.BitCallBack() {
            @Override
            public void onNewResultImpl(Bitmap bitmap) {
                final Drawable drawable = GaussAmbiguity.BlurImages(bitmap, App.getInstance());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        face.setImageDrawable(drawable);
                        face.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    }
                });
            }
        });
        camera_surface.setVisibility(View.GONE);
        //如果是观众，直接启动房间
        if (roomModel.getRoomType().equals(App.LIVE_WATCH)) {
            face.setVisibility(View.GONE);
            fragmentList.add(callFragment);
            fragmentPagerAdapter.notifyDataSetChanged();
            MediaCenter.initPlay(this);
            roomStart();
        }
        //如果是预览，进入预览流程
        if (roomModel.getRoomType().equals(App.LIVE_PREVIEW)) {
            face.setVisibility(View.GONE);
            if (!isqupai) {
                MediaCenter.initLive(this);
                //美颜开启此属性
                MediaCenter.startRecording(viewPanel, App.screenWidth, App.screenHeight);
            } else {
                camera_surface.setVisibility(View.VISIBLE);
            }
        }
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.room_guide:
                room_guide.setVisibility(View.GONE);
                SPreferencesTool.getInstance().putValue(this, SPreferencesTool.room_guide_key, false);
                break;
            case R.id.button_call_disconnect:
                CloseLiveDialog();
                break;
        }
    }

    /**
     * 关闭房间
     */
    private void CloseLiveDialog() {
        CommChooseDialog dialog = new CommChooseDialog();
        CommChooseDialog.Callback callback = new CommChooseDialog.Callback() {
            @Override
            public void onCancel() {
                isCloseLiveDialog = false;
            }

            @Override
            public void onOK(boolean choose) {
                //如果是直播，发送下麦通知
                if (roomModel.getRoomType().equals(App.LIVE_HOST) && serviceManager != null) {
                    serviceManager.downMic();
                    App.roomModel.setLivetime(DateTimeTool.DateFormathms(((int) (DateTimeTool.GetDateTimeNowlong() / 1000) - beginTime)));
                    StartActivityHelper.jumpActivity(ChatRoomActivity.this, LiveFinishActivity.class, roomModel);
                    if (choose && (DateTimeTool.GetDateTimeNowlong() / 1000) - beginTime > 60) {
                        LiveQiSaveVideo();
                    }
                } else if (roomModel.getRoomType().equals(App.LIVE_PREVIEW)) {
                    //收起键盘
                    if (readyLiveFragment != null) {
                        readyLiveFragment.closekeybord();
                    }
                }
                exitRoom();
            }
        };
        if (!isCloseLiveDialog) {
            isCloseLiveDialog = true;
            String title;
            boolean isShowSave;
            if (liveUserModel.userid.equals(userModel.userid)) {
                if (beginTime == 0) {//直播预览结束直播
                    isShowSave = false;
                    title = getString(R.string.finish_room);
                } else {
                    if ((DateTimeTool.GetDateTimeNowlong() / 1000) - beginTime > 60) {
                        isShowSave = true;
                        title = getString(R.string.finish_room);
                    } else {
                        isShowSave = false;//直播时间不足一分钟
                        title = getString(R.string.live_time_short);
                    }
                }
                dialog.dialog(this, title, true, isShowSave, callback, userModel);
            } else {
                dialog.dialog(this, getString(R.string.quit_room), true, false, callback, userModel);
            }
        }
    }

    /**
     * 结束直播 对话框
     *
     * @param resId 字符串
     */
    private void endLive(String resId) {
        CommDialog commDialog = new CommDialog();
        CommDialog.Callback callback = new CommDialog.Callback() {
            @Override
            public void onCancel() {
                //结束直播
                if (roomModel.getRoomType().equals(App.LIVE_HOST) && serviceManager != null) {
                    serviceManager.downMic();
                    roomModel.setLivetime(DateTimeTool.DateFormathms(((int) (DateTimeTool.GetDateTimeNowlong() / 1000) - beginTime)));
                    StartActivityHelper.jumpActivity(ChatRoomActivity.this, LiveFinishActivity.class, roomModel);
                } else if (roomModel.getRoomType().equals(App.LIVE_PREVIEW)) {
                    //收起键盘
                    if (readyLiveFragment != null) {
                        readyLiveFragment.closekeybord();
                    }
                }
                exitRoom();
            }

            @Override
            public void onOK() {
                restartConnection();
            }
        };
        commDialog.CommDialog(ChatRoomActivity.this, resId, true, callback);
    }

    /**
     * 保存直播视频
     */
    private boolean isRun = false;

    private void LiveQiSaveVideo() {
        HttpBusinessCallback callback = new HttpBusinessCallback() {
            @Override
            public void onFailure(Map<String, ?> errorMap) {
                DebugLogs.e("=========response=====保存录像失败");
                if (!isRun) {
                    LiveQiSaveVideo();
                    isRun = true;
                }
            }

            @Override
            public void onSuccess(String response) {
                DebugLogs.e("=========response=====保存录像" + response);
                CommonModel results = JsonUtil.fromJson(response, CommonModel.class);
                if (results != null) {
                    if (!HttpFunction.isSuc(results.code)) {
                        onBusinessFaild(results.code);
                    }
                }
            }
        };
        chatRoom.LiveQiSaveVideo(CommonUrlConfig.LiveQiSaveVideo, CacheDataManager.getInstance().loadUser(), roomModel.getLiveid(), callback);
    }


    private void roomStart() {
        //房间引导页展示
        boolean boolGuide = SPreferencesTool.getInstance().getBooleanValue(this, SPreferencesTool.room_guide_key);
        if (boolGuide) {
            room_guide.setVisibility(View.VISIBLE);
        } else {
            room_guide.setVisibility(View.GONE);
        }
        if (serviceManager == null && roomModel.getIp() != null && roomModel.getPort() != 0) {
            SocketConfig socketConfig = new SocketConfig();
            socketConfig.setHost(roomModel.getIp());
            socketConfig.setPort(roomModel.getPort());
            serviceManager = new ServiceManager(this, socketConfig, roomModel.getId(), uiHandler, userModel);
            if (imgPath != null) {
                UpDataPhoto(imgPath,String.valueOf(roomModel.getId()));
            }
        } else {
            ToastUtils.showToast(this, getString(R.string.login_room_fail));
        }
    }

    /**
     * 重连房间
     */
    private void restartConnection() {
        connectionServiceNumber++;
        if (connectionServiceNumber < 5) {
            if (NetWorkUtil.isNetworkConnected(this) && serviceManager != null) {
                serviceManager.connectionService();
            } else {
                noNetWork();
            }
        } else {
            //五次还是连不上就退出房间
            peerDisConnection(getString(R.string.room_net_toast_error));
        }
    }

    /**
     * 无网络
     */
    private void noNetWork() {
        if (!this.isFinishing()) {
            new NomalAlertDialog().alwaysShow(this, getString(R.string.setting_network),
                    getString(R.string.not_network), getString(R.string.ok), getString(R.string.cancel),
                    new NomalAlertDialog.HandlerDialog() {
                        @Override
                        public void handleOk() {
                            startActivity(new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS));
                        }

                        @Override
                        public void handleCancel() {
                            exitRoom();
                        }
                    }
            );
        }
    }

    @Override
    public void doHandler(final Message msg) {
        switch (msg.what) {
            case IServiceValues.NETWORK_SUCCESS:
                if (!boolConnRoom) {
                    connectionServiceNumber = 0;
                    restartConnection();
                }
                break;
            case GlobalDef.WM_ROOM_LOGIN_OUT://退出房间
                exitRoom();
                break;
            case GlobalDef.SERVICE_STATUS_FAILD://连接失败
                DebugLogs.e("network test---------faild");
                //如果首次连接失败，给出提示并退出房间
                boolConnRoom = false;
                connTotalNum++;
                if (connectionServiceNumber < 1 || connTotalNum >= 10) {
                    ToastUtils.showToast(this, getString(R.string.the_server_connect_fail));
                    exitRoom();
                } else {
                    restartConnection();
                }
                break;
            case GlobalDef.SERVICE_STATUS_CONNETN:
                boolConnRoom = false;
                connTotalNum++;
                DebugLogs.e("network test---------SERVICE_STATUS_CONNETN");
                if (NetWorkUtil.getActiveNetWorkType(this) == NetWorkUtil.TYPE_MOBILE) {
                    endLive(getString(R.string.traffic_alert));
                } else if (connTotalNum >= 10) {
                    endLive(getString(R.string.the_server_connect_fail));
                } else {
                    //失去了连接，重连5次
                    restartConnection();
                }
                break;
            case GlobalDef.SERVICE_STATUS_SUCCESS://房间服务器连接成功
                connectionServiceNumber = 1;
                boolConnRoom = true;
                //房间信息没有初始化才进行下一步，防止断线重连后重复初始化房间信息
                if (!isInit && roomModel != null) {
                    callFragment.setRoomInfo();
                    //检查关注状态
                }
                break;
            case GlobalDef.WM_ROOM_LOGIN://房间登录成功，身份验证通过
                //先判断是不是重连的状态，是重连的登录成功，跳过此步骤
                if (fragmentList.size() > 1) {
                    mAbSlidingTabView.setCurrentItem(1);
                }
                try {
                    BarInfoModel loginMessage = JsonUtil.fromJson(msg.obj.toString(), BarInfoModel.class);
                    if (loginMessage != null && loginMessage.code.equals("0")) {
                        LoadingDialog.cancelLoadingDialog();
                        long coin = loginMessage.coin;
                        //更新金币
                        userModel.diamonds = String.valueOf(coin);
                        CacheDataManager.getInstance().update(BaseKey.USER_DIAMOND, userModel.diamonds, userModel.userid);
                        callFragment.setDiamonds(userModel.diamonds);
                        App.roomModel.setLikenum(Integer.parseInt(loginMessage.hot));
                        callFragment.setLikeNum(App.roomModel.getLikenum());
                        callFragment.setOnline(loginMessage.online);
                        serviceManager.getOnlineListUser();
                        if (!isSysMsg) {
                            isSysMsg = true;
                            ChatLineModel chatlinemodel = new ChatLineModel();
                            ChatLineModel.from from = new ChatLineModel.from();
                            from.name = userModel.nickname;
                            from.uid = userModel.userid;
                            from.headphoto = userModel.headurl;
                            from.level = "0";
                            chatlinemodel.from = from;
                            chatlinemodel.type = 9;
                            chatlinemodel.isFirst = true;
                            chatlinemodel.message = String.format(getString(R.string.room_system_message), userModel.nickname);
                            chatManager.AddChatMessage(chatlinemodel);
                            callFragment.notifyData();
                        }
                        callFragment.StartChronometer();
                        if (roomModel.getRoomType().equals(App.LIVE_HOST)) {
                            //如果状态是直播，发送直播上麦
                            serviceManager.sendRTMP_WM_SDP(roomModel.getRtmpwatchaddress(), "");
                        } else if (roomModel.getRoomType().equals(App.LIVE_WATCH)) {
                            //如果是观看流程，首先检查是否正在直播
                            //上麦
                            if (loginMessage.live == 1) {
                                App.roomModel.setRtmpwatchaddress(loginMessage.live_uri);
                                MediaCenter.startPlay(viewPanel, App.screenWidth, App.screenHeight, roomModel.getRtmpwatchaddress(), onPlayListener);
                            } else {
                                //房间未直播
                                liveFinish();
                            }
                        }
                    } else {
                        ToastUtils.showToast(this, getString(R.string.room_login_failed));
                        exitRoom();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                break;
            case GlobalDef.WM_SDP:
                //判断房间信息是否加载成功，如果没有加载，设置房间信息
                if (!isInit) {
                    callFragment.setLikeNum(App.roomModel.getLikenum());
                    serviceManager.getOnlineListUser();
                    isInit = true;
                }
                break;
            case GlobalDef.WM_ROOM_MESSAGE://接受服务器消息
                CommonModel commonModel_chat = JsonUtil.fromJson(msg.obj.toString(), CommonModel.class);
                if (commonModel_chat != null && commonModel_chat.code.equals("0")) {
                    chatManager.receivedChatMessage(msg.obj, callFragment);
                    callFragment.notifyData();
                    if (timeCount == null) {
                        timeCount = new TimeCount(1000, 100);
                        timeCount.start();
                        BigData = 0;
                        isbigData = false;
                    }
                    BigData++;
                }
                break;
            case GlobalDef.WM_LIVE_SHOWMIC: //主播上麦了，重新观看
                JSONObject jsobj;
                try {
                    jsobj = new JSONObject(msg.obj.toString());
                    int liveState = jsobj.optInt("live");
                    String uri = jsobj.optString("uri");
                    if (liveState == 0 && !liveUserModel.userid.equals(userModel.userid)) {
                        //主播停止直播了
                        liveFinish();
                    } else if (liveState == 1 && !liveUserModel.userid.equals(userModel.userid)) {
                        if (liveFinishFragment != null && roomModel != null) {
                            //恢复播放
                            liveFinishFragment.dismiss();
                            if (uri.isEmpty()) {
                                MediaCenter.startPlay(viewPanel, App.screenWidth, App.screenHeight, roomModel.getRtmpwatchaddress(), onPlayListener);
                            } else {
                                roomModel.setRtmpwatchaddress(uri);
                                MediaCenter.startPlay(viewPanel, App.screenWidth, App.screenHeight, uri, onPlayListener);
                            }
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            case GlobalDef.WM_ROOM_EXIT_ENTRY: //进出房间通知
                OnlineListModel.OnlineNotice onlineNotice = JsonUtil.fromJson(msg.obj.toString(), OnlineListModel.OnlineNotice.class);
                if (onlineNotice != null) {
                    if (onlineNotice.kind == 0) {//上线
                        roomModel.addlivenum();
                        ChatLineModel chatlinemodel = new ChatLineModel();
                        ChatLineModel.from from = new ChatLineModel.from();
                        from.name = onlineNotice.user.name;
                        from.uid = String.valueOf(onlineNotice.user.uid);
                        from.headphoto = onlineNotice.user.headphoto;
                        from.level = String.valueOf(onlineNotice.user.level);
                        chatlinemodel.from = from;
                        chatlinemodel.type = 0;
                        chatlinemodel.isFirst = true;
                        chatlinemodel.message = getString(R.string.me_online);
                        chatManager.AddChatMessage(chatlinemodel);
                        callFragment.notifyData();
                    }
                    //更新界面
                    callFragment.updateOnline(onlineNotice);
                }
                break;
            case GlobalDef.WM_ROOM_LIKENUM: //有人点赞
                JSONObject jsonlikenum;
                try {
                    jsonlikenum = new JSONObject((String) msg.obj);
                    if (jsonlikenum.getInt("data") > roomModel.getLikenum()) {
                        int count = jsonlikenum.getInt("data") - roomModel.getLikenum();
                        if (!isbigData) {
                            callFragment.runAddLove(count);
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            case GlobalDef.WM_ROOM_RECEIVE_PEOPLE: //收到在线列表
                CommonListResult<OnlineListModel> results = JsonUtil.fromJson(msg.obj.toString(), new TypeToken<CommonListResult<OnlineListModel>>() {
                }.getType());
                if (results != null && results.code.equals("0")) {
                    callFragment.InitializeOnline(results.users);
                }
                break;
            case GlobalDef.WM_ROOM_SENDGIFT:
                GiftModel.AcceptGift giftModel = JsonUtil.fromJson(msg.obj.toString(), GiftModel.AcceptGift.class);
                if (giftModel == null) {
                    break;
                }
                int gift_Num = giftModel.number;
                String msgstr = "%/%";
                int resultCode = Integer.parseInt(giftModel.code);
                if (resultCode == 0) {
                    if (giftModel.type == 6) {//收礼物消息，礼物接受者
                        //加币
                        callFragment.setDiamonds(String.valueOf(giftModel.coin));
                        CacheDataManager.getInstance().update(BaseKey.USER_DIAMOND, String.valueOf(giftModel.coin), userModel.userid);
                        //记录金币数量
                        roomModel.addLivecoin(callFragment.getGiftCoinToId(giftModel.giftid) * giftModel.number);
                    } else if (giftModel.type == 61) {//发送礼物消息。发送者消息结果，
                        //减币
                        callFragment.setDiamonds(String.valueOf(giftModel.coin));
                        CacheDataManager.getInstance().update(BaseKey.USER_DIAMOND, String.valueOf(giftModel.coin), userModel.userid);
                    }
                    Cocos2dxGift.Cocos2dxGiftModel cocos2dxGiftModel;
                    switch (giftModel.giftid) {
                        case 5://Racing Car白色的跑车
                            for (int m = 0; m < gift_Num; m++) {
                                cocos2dxGiftModel = new Cocos2dxGift.Cocos2dxGiftModel();
                                cocos2dxGiftModel.aniName = "fx_car_white";
                                cocos2dxGiftModel.exportJsonPath = "fx_car_white.ExportJson";
                                cocos2dxGiftModel.x = ScreenUtils.getScreenWidth(this) / 2;
                                cocos2dxGiftModel.y = ScreenUtils.getScreenHeight(this) / 2;
                                cocos2dxGiftModel.scale = 2f;
                                bigGift.add(cocos2dxGiftModel);
                                if (!isStart) {
                                    isStart = true;
                                    startPlayBigGift();
                                }
                            }
                            break;
                        case 11://Red Racing Car红色的跑车
                            for (int m = 0; m < gift_Num; m++) {
                                cocos2dxGiftModel = new Cocos2dxGift.Cocos2dxGiftModel();
                                cocos2dxGiftModel.aniName = "fx_car";
                                cocos2dxGiftModel.exportJsonPath = "fx_car.ExportJson";
                                cocos2dxGiftModel.x = ScreenUtils.getScreenWidth(this) / 2;
                                cocos2dxGiftModel.y = ScreenUtils.getScreenHeight(this) / 2;
                                cocos2dxGiftModel.scale = 2f;
                                bigGift.add(cocos2dxGiftModel);
                                if (!isStart) {
                                    isStart = true;
                                    startPlayBigGift();
                                }
                            }
                            break;
                        case 37://烟花
                            for (int m = 0; m < gift_Num; m++) {
                                cocos2dxGiftModel = new Cocos2dxGift.Cocos2dxGiftModel();
                                cocos2dxGiftModel.aniName = "firework_01_4";
                                cocos2dxGiftModel.exportJsonPath = "firework_01_4.ExportJson";
                                cocos2dxGiftModel.x = ScreenUtils.getScreenWidth(this) / 2;
                                cocos2dxGiftModel.y = ScreenUtils.getScreenHeight(this) / 2;
                                cocos2dxGiftModel.scale = 2f;
                                bigGift.add(cocos2dxGiftModel);
                                if (!isStart) {
                                    isStart = true;
                                    startPlayBigGift();
                                }
                            }
                            break;
                        case 7: //飞机
                            Cocos2dxGift.Cocos2dxGiftModel cocosPlaneModel;
                            for (int m = 0; m < gift_Num; m++) {
                                cocosPlaneModel = new Cocos2dxGift.Cocos2dxGiftModel();
                                cocosPlaneModel.aniName = "fjxxxg";
                                cocosPlaneModel.exportJsonPath = "fjxxxg.ExportJson";
                                cocosPlaneModel.x = ScreenUtils.getScreenWidth(this) / 2;
                                cocosPlaneModel.y = ScreenUtils.getScreenHeight(this) / 2;
                                cocosPlaneModel.scale = 1.6f;
                                cocosPlaneModel.speedScale = 0.5f;
                                bigGift.add(cocosPlaneModel);
                                if (!isStart) {
                                    isStart = true;
                                    startPlayBigGift();
                                }
                            }
                            break;
                    }

                    GiftModel giftmodelInfo = chatRoom.getGifPath(giftModel.giftid);
                    //礼物特效
                    GiftAnimationModel giftaModel = new GiftAnimationModel();
                    if (giftModel.from != null) {
                        giftaModel.userheadpoto = giftModel.from.headphoto;
                        giftaModel.from_uname = giftModel.from.name;
                    }
                    giftaModel.giftmodel = giftmodelInfo;
                    giftaModel.giftnum = gift_Num;
                    callFragment.addGifAnimation(giftaModel);
                    ChatLineModel chatLineModel = new ChatLineModel();
                    chatLineModel.giftmodel = giftModel;
                    chatLineModel.type = 0;
                    chatLineModel.message = msgstr;
                    chatManager.AddChatMessage(chatLineModel);
                    callFragment.notifyData();
                } else if (resultCode == GlobalDef.NOT_SUFFICIENT_COIN_1012) {
                    ToastUtils.showToast(this, getString(R.string.gift_code1012));
                } else if (resultCode == GlobalDef.USER_NOTFOUND_1003) {
                    ToastUtils.showToast(this, getString(R.string.microom_code_1));
                }
                break;
            case GlobalDef.WM_ROOM_Kicking:
                JSONObject jsonKicking;
                try {
                    jsonKicking = new JSONObject((String) msg.obj);
                    int code = jsonKicking.optInt("code");
                    if (code == 0 && jsonKicking.getJSONObject("from") != null) {
                        ToastUtils.showToast(this, getString(R.string.you_are_invited_out_of_the_room));
                        exitRoom();
                    } else if (code == GlobalDef.NO_PERMISSION_OPE_1009) {
                        ToastUtils.showToast(this, getString(R.string.not_font));
                    } else if (code == GlobalDef.USER_NOTFOUND_1003) {
                        ToastUtils.showToast(this, getString(R.string.microom_code_1));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            case Cocos2dxGiftCallback.MSG_FINISH://一个特效播放结束通知
                bigGift.remove(0);
                startPlayBigGift();
                break;
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case CommonResultCode.SET_ADD_PHOTO_CAMERA:
                    //拍照
                    distUri = mObtain.obtainUrl();
                    mObtain.notifyChange(this, mObtain.getUri(this));
                    mObtain.cropBig(this, mObtain.getUri(this), distUri, CommonResultCode.REQUEST_CROP_PICTURE, 800, 800);
                    break;
                case CommonResultCode.SET_ADD_PHOTO_ALBUM:
                    //从相册获取
                    if (data != null) {
                        distUri = mObtain.obtainUrl();
                        mObtain.cropBig(this, data.getData(), distUri, CommonResultCode.REQUEST_CROP_PICTURE, 800, 800);
                    }
                    break;
                case CommonResultCode.REQUEST_CROP_PICTURE:
                    //裁剪后的图片
                    String path = mObtain.getRealPathFromURI(this, distUri);
                    if (!new File(path).exists()) {
                        return;
                    }
                    try {
                        Bitmap bitmap = mObtain.getimage(path);
                        imgPath = mObtain.saveBitmapFile(bitmap);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    readyLiveFragment.setPhoto(UriHelper.fromFile(imgPath));
                    //UpDataPhoto(imgPath);
                    break;
            }
        }
    }

    /**
     * 上传服务器
     *
     * @param imgPath 图像地址;
     */
    private void UpDataPhoto(String imgPath,String id) {
        qiNiuUpload.setQiniuResultCallback(new QiniuUpload.QiniuResultCallback() {
            @Override
            public void onUpTokenError() {

            }

            @Override
            public void onUpQiniuError() {
                ToastUtils.showToast(ChatRoomActivity.this, getString(R.string.upload_photo_error));
            }

            @Override
            public void onCallServerError() {

            }

            @Override
            public void onUpQiniuSuc(String key) {
            }

            @Override
            public void onUpProgress(String key, double percent) {

            }
        });
        qiNiuUpload.doUpload(userModel.userid, userModel.token, imgPath, id, "2");
    }

    /**
     * 开始播放大礼物特效
     */

    private void startPlayBigGift() {
        if (bigGift.size() > 0) {
            callFragment.play(bigGift.get(0));
        } else {
            isStart = false;
        }
    }

    //主播结束了直播
    private void liveFinish() {
        face.setVisibility(View.GONE);
        liveFinishFragment = new LiveFinishFragment();
        liveFinishFragment.setRoomModel(roomModel);
        liveFinishFragment.show(getSupportFragmentManager(), "");
        if (isqupai && livePush != null) {
            livePush.onDestroy();
        }
    }

    //切换摄像头
    @Override
    public void onCameraSwitch() {
        if (isqupai) {
            livePush.mCamera();
        } else {
            MediaCenter.switchCamera();
        }
    }

    @Override
    public void onCamera() {
        new ActionSheetDialog(this)
                .builder()
                .setCancelable(true)
                .setCanceledOnTouchOutside(true)
                .addSheetItem(getString(R.string.camera), ActionSheetDialog.SheetItemColor.BLACK_222222,
                        new ActionSheetDialog.OnSheetItemClickListener() {
                            @Override
                            public void onClick(int which) {
                                mObtain.dispatchTakePictureIntent(ChatRoomActivity.this, CommonResultCode.SET_ADD_PHOTO_CAMERA);
                            }
                        })
                .addSheetItem(getString(R.string.album), ActionSheetDialog.SheetItemColor.BLACK_222222,
                        new ActionSheetDialog.OnSheetItemClickListener() {
                            @Override
                            public void onClick(int which) {
                                mObtain.getLocalPicture(ChatRoomActivity.this, CommonResultCode.SET_ADD_PHOTO_ALBUM);
                            }
                        }).show();
        readyLiveFragment.closekeybord();
    }

    //发送私人消息
    @Override
    public void onSendMessage(String msg) {
        if (serviceManager != null) {
            serviceManager.sendRoomMessage(msg);
            ChatLineModel chatLineModel = new ChatLineModel();
            ChatLineModel.from from = new ChatLineModel.from();
            from.name = userModel.nickname;
            from.uid = String.valueOf(userModel.userid);
            chatLineModel.from = from;
            chatLineModel.type = 0;
            chatLineModel.message = msg;
            chatManager.AddChatMessage(chatLineModel);
            callFragment.notifyData();
        }
    }

    @Override
    public void onSendGift(int toId, int giftId, int nNum) {
        if (serviceManager != null) {
            serviceManager.sendGift(toId, giftId, nNum);
        }
    }

    @Override
    public void sendLove(int num) {
        if (serviceManager != null) {
            serviceManager.sendLove(num);
        }
    }

    @Override
    public void kickedOut(String userId) {
        if (serviceManager != null) {
            serviceManager.kickedOut(userId);
        }
    }

    @Override
    public void closeLive() {
        CloseLiveDialog();
    }

    /**
     * 点击物理返回按钮**
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            isCloseLiveDialog = false;
            if (!roomModel.getRoomType().equals(App.LIVE_PREVIEW)) {
                if (callFragment.getBackState()) {
                    callFragment.keyBack();
                    return true;
                } else {
                    CloseLiveDialog();
                }
            } else {
                CloseLiveDialog();
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onPause() {
        if (roomModel != null && liveUserModel.userid.equals(userModel.userid)) {
            MediaCenter.onPause();
        }
        if (isqupai) {
            livePush.onPause();
        }
        super.onPause();
    }

    @Override
    public void onResume() {
        if (roomModel != null && liveUserModel.userid.equals(userModel.userid)) {
            MediaCenter.onResume();
        }
        if (isqupai) {
            livePush.onResume();
        }
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        if (!boolCloseRoom) {
            exitRoom();
        }
        super.onDestroy();
        if (isqupai) {
            livePush.onDestroy();
        }
        System.gc();
    }

    /**
     * 退房间操作
     */
    private void roomFinish() {
        uiHandler.removeCallbacksAndMessages(null);
        if (serviceManager != null) {
            serviceManager.quitRoom();
            serviceManager = null;
        }
        if (liveUserModel != null && userModel != null &&
                !liveUserModel.userid.equals(userModel.userid)) {
            MediaCenter.destoryPlay();
        } else {
            MediaCenter.destoryLive();
        }
        App.mChatlines.clear();
        App.roomModel = new RoomModel();
        boolCloseRoom = true;
        App.chatRoomApplication = null;
        Cocos2dxGiftCallback.onDestroy();
    }

    private void peerDisConnection(final String s) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                CommDialog commDialog = new CommDialog();
                CommDialog.Callback callback = new CommDialog.Callback() {
                    @Override
                    public void onCancel() {
                        isCloseLiveDialog = false;
                        if (roomModel.getRoomType().equals(App.LIVE_HOST)) {
                            StartActivityHelper.jumpActivity(ChatRoomActivity.this, LiveFinishActivity.class, roomModel);
                        }
                        exitRoom();
                    }

                    @Override
                    public void onOK() {
                        //如果是直播，发送下麦通知
                        if (roomModel.getRoomType().equals(App.LIVE_HOST)) {
                            StartActivityHelper.jumpActivity(ChatRoomActivity.this, LiveFinishActivity.class, roomModel);
                        } else if (roomModel.getRoomType().equals(App.LIVE_PREVIEW)) {
                            if (readyLiveFragment != null) {
                                readyLiveFragment.closekeybord();
                            }
                        }
                        exitRoom();
                    }
                };
                if (!isCloseLiveDialog) {
                    if (roomModel.getRoomType().equals(App.LIVE_HOST) && serviceManager != null) {
                        serviceManager.downMic();
                        roomModel.setLivetime(DateTimeTool.DateFormathms(((int) (DateTimeTool.GetDateTimeNowlong() / 1000) - beginTime)));
                    }
                    isCloseLiveDialog = true;
                    commDialog.CommDialog(ChatRoomActivity.this, s, false, callback);
                }
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        //No call for super(). Bug on API Level > 11.
    }

    private class TimeCount extends CountDownTimer {
        public TimeCount(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onTick(long millisUntilFinished) {
            isbigData = BigData > 100;
        }

        @Override
        public void onFinish() {
            BigData = 0;
            timeCount.cancel();
            timeCount = null;
        }
    }

    /***
     * 开播
     */
    @Override
    public void onBeginLive() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!isqupai) {
                    MediaCenter.startLive(roomModel.getRtmpip(), onLiveListener);
                } else {
                    livePush.StartLive(roomModel.getRtmpip());
                }
                beginTime = (int) (DateTimeTool.GetDateTimeNowlong() / 1000);
                if (callFragment != null) {
                    fragmentList.add(callFragment);
                    fragmentPagerAdapter.notifyDataSetChanged();
                    if (fragmentList.size() > 1) {
                        mAbSlidingTabView.setCurrentItem(1);
                    }
                }
                roomModel.setRoomType(App.LIVE_HOST);
                roomStart();
            }
        });
    }

    /**
     * 直播者回调函数
     *
     * @param rtmpUrl url
     * @param event   event
     */
    private OnLiveListener onLiveListener = new OnLiveListener() {
        @Override
        public void onLiveCallback(String s, int event) {
            DebugLogs.e("rtmp event" + event);
            switch (event) {
                case MediaNative.RTMP_LIVE_RECONNECTING:
                    //ToastUtils.showToast(ChatRoomActivity.this,"网络开小差了");
                    //断线重连中
                    if (rtmpConnectNumber >= 5) {
                        peerDisConnection(getString(R.string.room_net_toast_error));
                        return;
                    }
                    rtmpConnectNumber++;
                    ChatLineModel msg = new ChatLineModel();
                    msg.type = 10;
                    msg.message = getString(R.string.room_peerconn);
                    chatManager.AddChatMessage(msg);
                    callFragment.notifyData();
                    break;
                case MediaNative.RTMP_LIVE_RECONNECT:
                    face.setVisibility(View.GONE);
                    ChatLineModel msg_reconnect = new ChatLineModel();
                    msg_reconnect.type = 10;
                    msg_reconnect.message = getString(R.string.room_peerconn_success);
                    chatManager.AddChatMessage(msg_reconnect);
                    callFragment.notifyData();
                    //重连成功
                    break;
                case MediaNative.RTMP_LIVE_CONNECT_ERROR:
                    // ToastUtils.showToast(ChatRoomActivity.this,"主播正在化妆");
                    //直播错误
                    peerDisConnection(getString(R.string.room_net_toast_error));
                    break;
                case MediaNative.RTMP_LIVE_CONNECT:
                    break;
                case MediaNative.RTMP_LIVE_STOP:
                    // ToastUtils.showToast(ChatRoomActivity.this,"网络去哪了？");
                    // roomFinish();
                    break;
            }
        }
    };


    /**
     * 观看的人 回调
     */
    private OnPlayListener onPlayListener = new OnPlayListener() {
        @Override
        public void onPlayCallback(String rtmpUrl, int event) {
            switch (event) {
                case MediaNative.RTMP_PLAY_RECONNECTING://流媒体重连
                    //ToastUtils.showToast(ChatRoomActivity.this,"网络开小差了");
                    //重连
                    break;
                case MediaNative.RTMP_PLAY_RECONNECT://
                    //重连成功
                    break;
                case MediaNative.RTMP_PLAY_CONNECT:
                    face.setVisibility(View.GONE);
                    break;
                case MediaNative.RTMP_PLAY_STOP:
                    break;
                case MediaNative.RTMP_PLAY_CONNECT_ERROR:
                    peerDisConnection(getString(R.string.room_net_toast_error));
                    break;
            }
        }
    };

    /**
     * 退出房间
     */
    public void exitRoom() {
        if (!boolCloseRoom) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    roomFinish();
                }
            });
        }
        finish();
    }
}
