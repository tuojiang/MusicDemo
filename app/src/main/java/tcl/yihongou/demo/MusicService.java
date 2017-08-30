package tcl.yihongou.demo;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcelable;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;



import tcl.yihongou.demo.model.LrcContent;
import tcl.yihongou.demo.model.Music;

/**
 * Created by yihong.ou on 17-8-23.
 */
public class MusicService extends Service {

    private MediaPlayer mPlayer;
    private ArrayList<Music> musicPathLists;
    private int currentPos;         // 记录当前正在播放的音乐
    private int nextPlay;
    public static ObjectAnimator animator;
    private LrcProcess mLrcProcess; //歌词处理
    private List<LrcContent> lrcList = new ArrayList<LrcContent>(); //存放歌词列表对象
    private int index = 0;          //歌词检索值
    private int currentTime;		//当前播放进度
    private int duration;			//播放长度
    private List<Music> mp3Infos;	//存放Mp3Info对象的集合
    private MyBinder myBinder=new MyBinder();


    public interface CallBack {
        boolean isPlayerMusic();
        int callTotalDate();
        int callCurrentTime();
        void iSeekTo(int m_second);
        void isPlayPre();
        void shPlayPre();
        void isPlayNext();
        void shPlayNext();
        boolean isPlayering();
        void currentList();
        void toggleShuffle();
        void cycleRepeat();
        void initLrc();
        int lrcIndex();
        String getTitle();
        String getArtist();
    }

    public class MyBinder extends Binder implements CallBack {

        @Override
        public boolean isPlayerMusic() {
//            initLrc();
            return playerMusic();
        }

        @Override
        public int callTotalDate() {
            if (mPlayer != null) {
                return mPlayer.getDuration();
            } else {
                return 0;
            }
        }

        @Override
        public int callCurrentTime() {
            if (mPlayer != null) {
                return mPlayer.getCurrentPosition();
            } else {
                return 0;
            }
        }

        @Override
        public void iSeekTo(int m_second) {
            if (mPlayer != null) {
                mPlayer.seekTo(m_second);
            }
        }

        @Override
        public void isPlayPre() {
            if (--currentPos < 0) {
                currentPos = 0;
            }
//            initLrc();
            initMusic();
            playerMusic();
        }

        @Override
        public void shPlayPre() {
//            initLrc();
            shuffleMusic();
            playerMusic();
        }



        @Override
        public void isPlayNext() {
            if (++currentPos > musicPathLists.size() - 1) {
                currentPos = musicPathLists.size() - 1;
            }
//            initLrc();
            initMusic();
            playerMusic();
        }

        @Override
        public void shPlayNext() {
//            initLrc();
            shuffleMusic();
            playerMusic();
        }


        @Override
        public boolean isPlayering() {
            if(mPlayer.isPlaying()){
                return true;
            }else{
                return false;
            }
        }
        /*顺序播放*/
        @Override
        public void currentList() {
                    initLrc();
            mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    currentPos++;
                    if (currentPos >= musicPathLists.size()) {
                        currentPos = 0;
                    }
//                    initLrc();
                    initMusic();
                    playerMusic();
                }
            });
        }
        /*随机播放*/
        @Override
        public void toggleShuffle() {
//            int min=0;
//            int max=musicPathLists.size();
//            Random random = new Random();
//
//            int s = random.nextInt(max-min+1) + min;
//            currentPos=s;
                    initLrc();
            mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

                @Override
                public void onCompletion(MediaPlayer mp) {
//                    initLrc();
                    shuffleMusic();
                    playerMusic();
                }
            });
        }
        /*单曲播放*/
        @Override
        public void cycleRepeat() {
                    initLrc();
            mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

                @Override
                public void onCompletion(MediaPlayer mp) {

                    repeatMusic();
                    playerMusic();
                }
            });
        }
        /*显示歌词*/
        @Override
        public void initLrc() {
            mLrcProcess = new LrcProcess();
            //读取歌词文件
            mLrcProcess.readLRC(musicPathLists.get(currentPos).getMusicPath());
            //传回处理后的歌词文件
            lrcList = mLrcProcess.getLrcList();
//            MainActivity.lrcView.setmLrcList(lrcList);
//          发送广播
            Intent intent = new Intent();
            Bundle bundle = new Bundle();
            intent.putParcelableArrayListExtra("LRC_LIST", (ArrayList<? extends Parcelable>) lrcList);
            intent.putExtra("SIZE",lrcList.size());
            intent.setAction("yihong.lrc");
            sendBroadcast(intent);


        }
        /*显示歌词索引值*/
        @Override
        public int lrcIndex() {
            if(mPlayer.isPlaying()) {
                currentTime = mPlayer.getCurrentPosition();
                duration = mPlayer.getDuration();
            }
            if(currentTime < duration) {
                for (int i = 0; i < lrcList.size(); i++) {
                    if (i < lrcList.size() - 1) {
                        if (currentTime < lrcList.get(i).getLrcTime() && i == 0) {
                            index = i;
                        }
                        if (currentTime > lrcList.get(i).getLrcTime()
                                && currentTime < lrcList.get(i + 1).getLrcTime()) {
                            index = i;
                        }
                    }
                    if (i == lrcList.size() - 1
                            && currentTime > lrcList.get(i).getLrcTime()) {
                        index = i;
                    }
                }
            }
            return index;
        }

        @Override
        public String getTitle() {
            String musicTitle=musicPathLists.get(currentPos).getTitle();
            return musicTitle;
        }

        @Override
        public String getArtist() {
            String musicArtist=musicPathLists.get(currentPos).getArtist();
            return musicArtist;
        }


    }

    @Override
    public void onCreate() {
        super.onCreate();
        mPlayer = new MediaPlayer();
    }
//    Handler mHandler = new Handler();
//    Runnable mRunnable = new Runnable() {
//        @Override
//        public void run() {
//            MainActivity.lrcView.setIndex(lrcIndexx());
//            MainActivity.lrcView.invalidate();
//            mHandler.postDelayed(mRunnable, 100);
//        }
//    };
    /**
     * 初始化歌词配置
     */
    public void initLrcx(){
         /*mLrcProcess = new LrcProcess();
        //读取歌词文件
        mLrcProcess.readLRC(musicPathLists.get(currentPos).getMusicPath());
        //传回处理后的歌词文件
        lrcList = mLrcProcess.getLrcList();
        MainActivity.lrcView.setmLrcList(lrcList);
        //切换带动画显示歌词
        MainActivity.lrcView.setAnimation(AnimationUtils.loadAnimation(MusicService.this,R.anim.alpha_z));
            MainActivity.lrcView.setIndex(lrcIndexx());
            MainActivity.lrcView.invalidate();
//        mHandler.post(mRunnable);*/

    }
    /*显示歌词索引值*/
    public int lrcIndexx() {
        if(mPlayer.isPlaying()) {
            currentTime = mPlayer.getCurrentPosition();
            duration = mPlayer.getDuration();
        }
        if(currentTime < duration) {
            for (int i = 0; i < lrcList.size(); i++) {
                if (i < lrcList.size() - 1) {
                    if (currentTime < lrcList.get(i).getLrcTime() && i == 0) {
                        index = i;
                    }
                    if (currentTime > lrcList.get(i).getLrcTime()
                            && currentTime < lrcList.get(i + 1).getLrcTime()) {
                        index = i;
                    }
                }
                if (i == lrcList.size() - 1
                        && currentTime > lrcList.get(i).getLrcTime()) {
                    index = i;
                }
            }
        }
        return index;
    }
    /**
     * 随机播放
     */
    private void shuffleMusic() {
        int min=0;
        int max=musicPathLists.size();
        Random random = new Random();

        final int s = random.nextInt(max-min+1) + min;
        currentPos=s;
        mPlayer.reset();
        try {
            mPlayer.setDataSource(musicPathLists.get(currentPos).getMusicPath());
            mPlayer.prepare();
            initLrcx();
            mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    currentPos=s;


                    shuffleMusic();
                    playerMusic();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    /**
     * 单曲播放
     */
    private void repeatMusic() {

        mPlayer.reset();

        try {
//            nextPlay=currentPos;

            mPlayer.setDataSource(musicPathLists.get(currentPos).getMusicPath());
            mPlayer.prepare();
            initLrcx();
            mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    nextPlay=currentPos;


                    repeatMusic();
                    playerMusic();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    private void initMusic() {
        // 根路径
        //      String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Xxx.mp3";
        mPlayer.reset();
        try {
            mPlayer.setDataSource(musicPathLists.get(currentPos).getMusicPath());
            mPlayer.prepare();
//            initLrcx();
            mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    currentPos++;
                    if (currentPos >= musicPathLists.size()) {
                        currentPos = 0;
                    }

                    initMusic();
                    playerMusic();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public boolean playerMusic() {
        if (mPlayer.isPlaying()) {
            mPlayer.pause();
            animator.pause();
            return false;
        } else {
            mPlayer.start();
            animator.setDuration(5000);
            animator.setInterpolator(new LinearInterpolator()); // 均速旋转
            animator.setRepeatCount(ValueAnimator.INFINITE); // 无限循环
            animator.setRepeatMode(ValueAnimator.INFINITE);
            animator.start();
            return true;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        musicPathLists = intent.getParcelableArrayListExtra("MUSIC_LIST");
        currentPos = intent.getIntExtra("CURRENT_POSITION", -1);
//       initLrcx();
        myBinder.initLrc();
            initMusic();
            playerMusic();
//        initMusic();
//
//        playerMusic();

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new MyBinder();
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public void onDestroy() {
        super.onDestroy();
        animator.pause();
        if (mPlayer != null) {
            if (mPlayer.isPlaying()) {
                mPlayer.stop();
            }
            mPlayer.release();
        }
    }
    public  void AnimatorAction() {
        if (mPlayer.isPlaying()) {
            animator.setDuration(5000);
            animator.setInterpolator(new LinearInterpolator()); // 均速旋转
            animator.setRepeatCount(ValueAnimator.INFINITE); // 无限循环
            animator.setRepeatMode(ValueAnimator.INFINITE);
            animator.start();
        }
    }
}

