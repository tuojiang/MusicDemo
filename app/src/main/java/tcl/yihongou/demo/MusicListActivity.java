package tcl.yihongou.demo;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Parcelable;
import android.os.PersistableBundle;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


import tcl.yihongou.demo.model.LrcContent;
import tcl.yihongou.demo.model.Music;

/**
 * Created by yihong.ou on 17-8-22.
 */
public class MusicListActivity extends Activity implements AdapterView.OnItemClickListener {
    private LrcProcess mLrcProcess; //歌词处理
    private ArrayList<LrcContent> lrcList = new ArrayList<>(); //存放歌词列表对象
    private int currentPos;         // 记录当前正在播放的音乐
    public  LrcView lrcView; // 自定义歌词视图

    private ListView mListView;
    private Handler mHandler = new Handler();
    private ArrayList<Music> mMediaLists = new ArrayList<>();
    private MusicListAdapter adapter;
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    @Override
    public void onCreate(Bundle savedInstanceState, PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_music_list_layout_sec);
            verifyStoragePermissions(this);
            mListView = (ListView) findViewById(R.id.music_list_view_sec);
            mListView.setOnItemClickListener(this);

            adapter = new MusicListAdapter(this);
            mListView.setAdapter(adapter);

            asyncQueryMedia();
    }

    public void asyncQueryMedia() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                mMediaLists.clear();
                queryMusic(Environment.getExternalStorageDirectory() + File.separator);
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        adapter.setListData(mMediaLists);
                    }
                });
            }
        }).start();
    }

    /**
     * 获取目录下的歌曲
     *
     * @param dirName
     */
    public void queryMusic(String dirName) {
        Cursor cursor = getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null,
                MediaStore.Audio.Media.DATA + " like ?",
                new String[]{dirName + "%"},
                MediaStore.Audio.Media.DEFAULT_SORT_ORDER);

        if (cursor == null) return;
        Music music;
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            String isMusic = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.IS_MUSIC));
            if (isMusic != null && isMusic.equals("")) continue;

            String title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
            String artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));

            if (isRepeat(title, artist)) continue;

            music = new Music();
            music.setId(cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)));
            music.setTitle(title);
            music.setArtist(artist);
            music.setMusicPath(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)));
            music.setLength(cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)));
            music.setImage(getAlbumImage(cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID))));
            music.setUrl(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA)));
            mMediaLists.add(music);
        }

        cursor.close();
    }


    /**
     * 根据音乐名称和艺术家来判断是否重复包含了
     *
     * @param title
     * @param artist
     * @return
     */
    private boolean isRepeat(String title, String artist) {
        for (Music music : mMediaLists) {
            if (title.equals(music.getTitle()) && artist.equals(music.getArtist())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 根据歌曲id获取图片
     *
     * @param albumId
     * @return
     */
    private String getAlbumImage(int albumId) {
        String result = "";
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(
                    Uri.parse("content://media/external/audio/albums/"
                            + albumId), new String[]{"album_art"}, null,
                    null, null);
            for (cursor.moveToFirst(); !cursor.isAfterLast(); ) {
                result = cursor.getString(0);
                break;
            }
        } catch (Exception e) {
        } finally {
            if (null != cursor) {
                cursor.close();
            }
        }

        return null == result ? null : result;
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        MusicListAdapter adapter = (MusicListAdapter) parent.getAdapter();
        adapter.setPlayingPosition(position);
        currentPos=position;
        Intent intent = new Intent(this,MainActivity.class);
        intent.putParcelableArrayListExtra("MUSIC_LIST", mMediaLists);
        intent.putExtra("CURRENT_POSITION", position);
//        intent.putExtra("LRC_LIST", (Parcelable) lrcList);
//        initLrc();
        startActivity(intent);

    }
    /**
     * 歌词显示
     */

//    public void initLrc() {
//        mLrcProcess = new LrcProcess();
//        //读取歌词文件
//        mLrcProcess.readLRC(mMediaLists.get(currentPos).getMusicPath());
//        //传回处理后的歌词文件
//        lrcList = (ArrayList<LrcContent>) mLrcProcess.getLrcList();
//        lrcView.setmLrcList(lrcList);
//        lrcView.setAnimation(AnimationUtils.loadAnimation(MusicListActivity.this,R.anim.alpha_z));
//        lrcView.invalidate();
//    }

    public class MusicListAdapter extends BaseAdapter {

        private LayoutInflater inflater;

        private ArrayList<Music> list = new ArrayList<>();
        private int mPlayingPosition;

        public MusicListAdapter(Context context) {
            inflater = LayoutInflater.from(context);
        }

        public void setPlayingPosition(int position) {
            mPlayingPosition = position;
            notifyDataSetChanged();
        }

        public void setListData(ArrayList<Music> list) {
            this.list = list;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public Object getItem(int position) {
            return list.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }


        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder= null;
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.music_list_item, parent, false);
                viewHolder = new ViewHolder();
                viewHolder.icon = (ImageView)convertView.findViewById(R.id.music_list_icon);
                viewHolder.title = (TextView)convertView.findViewById(R.id.tv_music_list_title);
                viewHolder.artist = (TextView)convertView.findViewById(R.id.tv_music_list_artist);
                viewHolder.mark = convertView.findViewById(R.id.music_list_selected);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder)convertView.getTag();
            }
            if (mPlayingPosition == position)
                viewHolder.mark.setVisibility(View.VISIBLE);
            else
                viewHolder.mark.setVisibility(View.INVISIBLE);

            Music music = (Music) getItem(position);

            Bitmap icon = BitmapFactory.decodeFile(music.getImage());
            viewHolder.icon.setImageBitmap(icon == null ?
                    BitmapFactory.decodeResource(
                            getResources(), R.drawable.play_bg) : icon);
            viewHolder.title.setText(music.getTitle());
            viewHolder.artist.setText(music.getArtist());

            return convertView;
        }

        class ViewHolder {
            ImageView icon;
            TextView title, artist;
            View mark;
        }
    }
    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }
}



