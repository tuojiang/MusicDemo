package tcl.yihongou.demo;

import android.animation.ObjectAnimator;
import android.annotation.TargetApi;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.icu.text.SimpleDateFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import tcl.yihongou.demo.model.LrcContent;
import tcl.yihongou.demo.model.Music;
/**
 * Created by yihong.ou on 17-8-22.
 */
@TargetApi(Build.VERSION_CODES.N)
public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private ImageButton bt_play, bt_pre, bt_next;
    private SeekBar seekBar;
    private MyHandler mHandler = new MyHandler(this);
    private SimpleDateFormat format = new SimpleDateFormat("mm:ss");
    private TextView currentTimeTxt, totalTimeTxt;
    private MusicService.CallBack callBack;
    private TextView mMusicTitle,mMusicArtist;
    private int currentPosition;
    private ImageView coverImage;
    private boolean mFlag = true;
    private ArrayList<Music> musicBeanList = new ArrayList<>();
    private int mProgress;
    private ImageButton mShuffleSong;
    private ImageButton mCurtListSong;
    private ImageButton mRepeatSong;
    public  LrcView lrcView; // 自定义歌词视图
    private String url; // 歌曲路径
    private static int ORDERMODE=0;
    private static int SHUFFLEMODE=1;
    private static int REPEATMODE=2;
    //    默认播放模式为顺序播放
    private static int CURRENTMODE=ORDERMODE;
    private IntentFilter intentFilter;
    private TextView testLrc;
    public ArrayList<LrcContent> mLrcList;//存放歌词列表对象
    private MyReceiver receiver;
    public class MyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            mLrcList =  intent.getParcelableArrayListExtra("LRC_LIST");
            //转换为list集合
            lrcView.setmLrcList(mLrcList);
            Log.i("MainActivity","setmLrcList"+String.valueOf(mLrcList.size()));
            lrcView.setAnimation(AnimationUtils.loadAnimation(MainActivity.this,R.anim.alpha_z));
            Log.i("MainActivity","setAnimation");
            mHandler.post(mRunnable);
            System.out.println(mLrcList.size());
            Log.i("MainActivity","post");
            for (int i = 0; i <mLrcList.size() ; i++) {
                System.out.println(mLrcList.get(i));
                Log.i("MainActivity","歌词内容："+mLrcList.get(i));
            }


        }
    }
    Runnable mRunnable = new Runnable() {

        @Override
        public void run() {

            lrcView.setIndex(callBack.lrcIndex());
            lrcView.invalidate();
            int a=callBack.lrcIndex();
            mHandler.postDelayed(mRunnable, 100);
        }
    };

    private static class MyHandler extends Handler {

        private WeakReference<MainActivity> reference;

        public MyHandler(MainActivity activity) {
            reference = new WeakReference<>(activity);
        }



        @Override
        public void handleMessage(Message msg) {
            MainActivity activity = reference.get();
            if (activity != null) {


                int currentTime = activity.callBack.callCurrentTime();
                int totalTime = activity.callBack.callTotalDate();
                activity.seekBar.setMax(totalTime);
                activity.seekBar.setProgress(currentTime);

                String current = activity.format .format(new Date(currentTime));
                String total = activity.format.format(new Date(totalTime));

                activity.currentTimeTxt.setText(current);
                activity.totalTimeTxt.setText(total);

                activity.mMusicTitle.setText(activity.callBack.getTitle());
                activity.mMusicArtist.setText(activity.callBack.getArtist());
            }

        }

    }

    private ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            callBack = (MusicService.MyBinder)service;

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            callBack = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        intentFilter=new IntentFilter();
        intentFilter.addAction("yihong.lrc");
        receiver=new MyReceiver();
        registerReceiver(receiver,intentFilter);
        initData();
        getMusInfoAndStService();

        seekTime();
        forSeekBar();
    }

    private void initData(){
        seekBar = (SeekBar)findViewById(R.id.seek_bar);
        bt_play = (ImageButton)findViewById(R.id.bt_play);
        bt_pre = (ImageButton)findViewById(R.id.bt_pre);
        bt_next = (ImageButton)findViewById(R.id.bt_next);
        mCurtListSong= (ImageButton) findViewById(R.id.im_curplaylist);
        mShuffleSong= (ImageButton) findViewById(R.id.im_shuffleSong);
        mRepeatSong= (ImageButton) findViewById(R.id.im_repeatSong);

        lrcView = (LrcView) findViewById(R.id.lrcShowView);
        mMusicTitle= (TextView) findViewById(R.id.musicTitle);
        mMusicArtist= (TextView) findViewById(R.id.musicArtist);


        currentTimeTxt = (TextView)findViewById(R.id.current_time_txt);
        totalTimeTxt = (TextView)findViewById(R.id.total_time_txt);
        if (CURRENTMODE==ORDERMODE){
            mCurtListSong.setVisibility(View.VISIBLE);
            mShuffleSong.setVisibility(View.GONE);
            mRepeatSong.setVisibility(View.GONE);
        }else if (CURRENTMODE==SHUFFLEMODE){
            mCurtListSong.setVisibility(View.GONE);
            mShuffleSong.setVisibility(View.VISIBLE);
            mRepeatSong.setVisibility(View.GONE);
        }else if (CURRENTMODE==REPEATMODE){
            mCurtListSong.setVisibility(View.GONE);
            mShuffleSong.setVisibility(View.GONE);
            mRepeatSong.setVisibility(View.VISIBLE);
        }


        bt_play.setOnClickListener(this);
        bt_pre.setOnClickListener(this);
        bt_next.setOnClickListener(this);

        mCurtListSong.setOnClickListener(this);
        mShuffleSong.setOnClickListener(this);
        mRepeatSong.setOnClickListener(this);
    }

    private void getMusInfoAndStService(){
        /** 接收音乐列表资源 */
        musicBeanList = getIntent().getParcelableArrayListExtra("MUSIC_LIST");
        currentPosition = getIntent().getIntExtra("CURRENT_POSITION", 0);
        //TODO
        /** 构造启动音乐播放服务的Intent，设置音乐资源 */
        Intent intent = new Intent(this, MusicService.class);
        intent.putParcelableArrayListExtra("MUSIC_LIST", musicBeanList);
        intent.putExtra("CURRENT_POSITION", currentPosition);
        startService(intent);
        bindService(intent, conn, Service.BIND_AUTO_CREATE);
        //专辑封面
//        coverImage = (ImageView) findViewById(R.id.coverImage);

        MusicService musicService = new MusicService();
        musicService.animator = ObjectAnimator.ofFloat(coverImage, "rotation", 0, 359);

    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onPause() {
        super.onPause();

    }


    //    private class MyMainActivityReceiver extends BroadcastReceiver {
//
//        @Override
//        public void onReceive(Context context, Intent intent) {
//
//        }
//    }

    private void seekTime(){

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (mFlag) {
                    if (callBack != null) {

                        mHandler.sendMessage(Message.obtain());

                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }).start();

    }

    private void forSeekBar(){
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (callBack != null)
                    mProgress = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (callBack != null) {
                    mProgress=seekBar.getProgress();
                    callBack.iSeekTo(mProgress);
                }
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            // 播放或者暂停
            case R.id.bt_play:
                playerMusicByIBinder();
                break;
            case R.id.bt_pre:
                if (CURRENTMODE==SHUFFLEMODE) {
                    callBack.shPlayPre();
                }else {
                    callBack.isPlayPre();
                }

                break;
            case R.id.bt_next:
                if (CURRENTMODE==SHUFFLEMODE) {
                    callBack.shPlayNext();
                }else {
                    callBack.isPlayNext();
                }
                break;
            case R.id.im_curplaylist:
                mCurtListSong.setVisibility(View.GONE);
                mShuffleSong.setVisibility(View.VISIBLE);
                mRepeatSong.setVisibility(View.GONE);
                //随机播放
                Toast.makeText(getApplicationContext(), "随机播放", Toast.LENGTH_SHORT).show();
                CURRENTMODE=SHUFFLEMODE;
                callBack.toggleShuffle();
                break;
            case R.id.im_shuffleSong:
                mCurtListSong.setVisibility(View.GONE);
                mShuffleSong.setVisibility(View.GONE);
                mRepeatSong.setVisibility(View.VISIBLE);
//                单曲播放
                Toast.makeText(getApplicationContext(), "单曲播放", Toast.LENGTH_SHORT).show();
                CURRENTMODE=REPEATMODE;
                callBack.cycleRepeat();
                break;
            case R.id.im_repeatSong:
                mCurtListSong.setVisibility(View.VISIBLE);
                mShuffleSong.setVisibility(View.GONE);
                mRepeatSong.setVisibility(View.GONE);
                Toast.makeText(getApplicationContext(), "顺序播放", Toast.LENGTH_SHORT).show();
                CURRENTMODE=ORDERMODE;
                callBack .currentList();
                break;
        }
    }

    /**
     * 播放音乐通过Binder接口实现
     */
    public void playerMusicByIBinder() {
        boolean playerState = callBack.isPlayerMusic();
        if (playerState) {
            bt_play.setImageResource(R.drawable.player_btn_pause_normal);
        } else {
            bt_play.setImageResource(R.drawable.player_btn_play_normal);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
        if (conn != null || callBack != null) {
            unbindService(conn);
            callBack = null;
        }
        Intent intent = new Intent(this, MusicService.class);
        stopService(intent);
        mFlag = false;
    }
}
