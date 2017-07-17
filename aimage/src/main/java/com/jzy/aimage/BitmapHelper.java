package com.jzy.aimage;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.Handler;
import android.text.TextUtils;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by JiangZhuyang on 2017/4/1.
 * <p>
 * 图片缓存技术的核心类，用于缓存所有下载好的图片，在程序内存达到设定值时会将最少最近使用的图片移除掉。
 */

public class BitmapHelper {

    public static final int MAX_SIZE = 20 * 1024 * 1024;

    private LruCache<String, Bitmap> mMemoryCache;

    private static ExecutorService mThreadPool;
    private static Map<String, Future<?>> mTaskTags = new LinkedHashMap<String, Future<?>>();

    private static Map<String, Set<ImageView>> mViewTags = new LinkedHashMap<String, Set<ImageView>>();
    private static Map<String, Set<ImageLoadListener>> mListenerTags = new LinkedHashMap<String, Set<ImageLoadListener>>();

    /**
     * 图片硬盘缓存核心类。
     */
    private LruDiskCache mDiskLruCache;

    private BitmapHelper(Context context) {
        init(context);
    }

    private static BitmapHelper instance;

    private OkHttpClient mClient;

    /**
     * BitmapUtils不是单例的 根据需要重载多个获取实例的方法
     *
     * @param appContext application context
     * @return
     */
    public static BitmapHelper getInstance(Context appContext) {
        if (instance == null) {
            instance = new BitmapHelper(appContext);
        }
        return instance;
    }

    private Context mContext;
    private Handler mHandler;

    private void init(Context context) {
        if (mThreadPool == null) {
            // 最多同时允许的线程数为3个
            mThreadPool = Executors.newFixedThreadPool(3);
        }
        mContext = context;
        mHandler = new Handler();
        mClient = new OkHttpClient();
        // 获取应用程序最大可用内存
        int maxMemory = (int) Runtime.getRuntime().maxMemory();
        int cacheSize = maxMemory / 8;
        // 设置图片缓存大小为程序最大可用内存的1/8
        mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getByteCount();
            }
        };
        createLruCache();
    }

    private void createLruCache() {
        try {
            // 获取图片缓存路径
            File cacheDir = getDiskCacheDir(mContext, "thumb");
            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
            }
            // 创建DiskLruCache实例，初始化缓存数据
            mDiskLruCache = LruDiskCache
                    .open(cacheDir, 215, 1, MAX_SIZE);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * 将一张图片存储到LruCache中。
     *
     * @param key    LruCache的键，这里传入图片的URL地址。
     * @param bitmap LruCache的键，这里传入从网络上下载的Bitmap对象。
     */
    public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemoryCache(key) == null) {
            mMemoryCache.put(key, bitmap);
        }
    }

    /**
     * 从LruCache中获取一张图片，如果不存在就返回null。
     *
     * @param key LruCache的键，这里传入图片的URL地址。
     * @return 对应传入键的Bitmap对象，或者null。
     */
    private Bitmap getBitmapFromMemoryCache(String key) {
        return mMemoryCache.get(key);
    }

    /**
     * 加载Bitmap对象。此方法会在LruCache中检查所有屏幕中可见的ImageView的Bitmap对象，
     * 如果发现任何一个ImageView的Bitmap对象不在缓存中，就会开启异步线程去下载图片。
     */
    public void display(ImageView imageView, String imageUrl) {
        try {
            if (TextUtils.isEmpty(imageUrl)) {
                return;
            }
            //先从缓存取
            Bitmap bitmap = getBitmapFromMemoryCache(imageUrl);
            checkViewTags(imageUrl, imageView);
            if (bitmap == null) {
                // 开线程去网络获取
                // 使用线程池管理
                // 判断是否有线程在为 imageView加载数据
                Future<?> futrue = mTaskTags.get(imageUrl);
                if (futrue != null && !futrue.isCancelled() && !futrue.isDone()) {
                    // 线程正在执行
//                    futrue.cancel(true);
//                    futrue = null;
                    return;
                }

                // mThreadPool.execute(new ImageLoadTask(iv, url));
                futrue = mThreadPool.submit(new ImageLoadTask(imageView, imageUrl));
                mTaskTags.put(imageUrl, futrue);
            } else {
                //从hashset里面取值
                setBitmap(imageUrl, bitmap);
                notifyBitMap(imageUrl, bitmap);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void getBitMap(String imageUrl, ImageLoadListener listener) {
        try {
            if (TextUtils.isEmpty(imageUrl)) {
                return;
            }
            //先从缓存取
            Bitmap bitmap = getBitmapFromMemoryCache(imageUrl);
            checkListenerTags(imageUrl, listener);
            if (bitmap == null) {
                // 开线程去网络获取
                // 使用线程池管理
                // 判断是否有线程在为 imageView加载数据
                Future<?> futrue = mTaskTags.get(imageUrl);
                if (futrue != null && !futrue.isCancelled() && !futrue.isDone()) {
                    // 线程正在执行
//                    futrue.cancel(true);
//                    futrue = null;
                    return;
                }

//                mThreadPool.execute(new ImageLoadTask(iv, url));
                futrue = mThreadPool.submit(new ImageLoadTask(listener, imageUrl));
                mTaskTags.put(imageUrl, futrue);
            } else {
                setBitmap(imageUrl, bitmap);
                //从hashset里面取值
                notifyBitMap(imageUrl, bitmap);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 取消所有正在下载或等待下载的任务。
     */
    public void cancelAllTasks() {
        try {
            if (mTaskTags != null) {
                for (Map.Entry<String, Future<?>> entry : mTaskTags.entrySet()) {
                    Future<?> futrue = entry.getValue();
                    futrue.cancel(true);
                    futrue = null;
                }
                mTaskTags.clear();
                mViewTags.clear();
                mListenerTags.clear();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 根据传入的uniqueName获取硬盘缓存的路径地址。
     */
    public File getDiskCacheDir(Context context, String uniqueName) {
        String cachePath;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                || !Environment.isExternalStorageRemovable()) {
            cachePath = context.getExternalCacheDir().getPath();
        } else {
            cachePath = context.getCacheDir().getPath();
        }
        return new File(cachePath + File.separator + uniqueName);
    }


    /**
     * 使用MD5算法对传入的key进行加密并返回。
     */
    private String hashKeyForDisk(String key) {
        String cacheKey;
        try {
            final MessageDigest mDigest = MessageDigest.getInstance("MD5");
            mDigest.update(key.getBytes());
            cacheKey = bytesToHexString(mDigest.digest());
        } catch (NoSuchAlgorithmException e) {
            cacheKey = String.valueOf(key.hashCode());
        }
        return cacheKey;
    }

    /**
     * 将缓存记录同步到journal文件中。
     */
    public void fluchCache() {
        if (mDiskLruCache != null) {
            try {
                mDiskLruCache.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(0xFF & bytes[i]);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    /*
    * 获取图片的线程
    * 先取本地磁盘，取不到从网络取
    * */
    class ImageLoadTask implements Runnable {

        private String mUrl;
        private ImageView iv;
        private ImageLoadListener mLister;

        public ImageLoadTask(ImageView iv, String url) {
            this.mUrl = url;
            this.iv = iv;
        }

        public ImageLoadTask(ImageLoadListener listener, String url) {
            this.mUrl = url;
            this.mLister = listener;
        }

        @Override
        public void run() {
            // 查找key对应的缓存
            LruDiskCache.Snapshot snapShot = null;

            FileDescriptor fileDescriptor = null;
            FileInputStream fileInputStream = null;

            try {
                // 生成图片URL对应的key
                final String key = hashKeyForDisk(mUrl);
                if (mDiskLruCache == null || mDiskLruCache.isClosed()) {
                    createLruCache();
                }
                // 查找key对应的缓存
                snapShot = mDiskLruCache.get(key);
                if (snapShot == null) {
                    // 如果没有找到对应的缓存，则准备从网络上请求数据，并写入缓存
                    LruDiskCache.Editor editor = mDiskLruCache.edit(key);
                    if (editor != null) {
                        OutputStream outputStream = editor.newOutputStream(0);
                        if (downloadUrlToStream(outputStream)) {
                            editor.commit();
                        } else {
                            editor.abort();
                        }
                    }
                    // 缓存被写入后，再次查找key对应的缓存
                    snapShot = mDiskLruCache.get(key);
                }
                if (snapShot != null) {
                    fileInputStream = (FileInputStream) snapShot.getInputStream(0);
                    fileDescriptor = fileInputStream.getFD();
                }
                // 将缓存数据解析成Bitmap对象
                Bitmap bitmap = null;
                if (fileDescriptor != null) {
                    bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor);
                }
                if (bitmap != null) {
                    // 将Bitmap对象添加到内存缓存当中
                    addBitmapToMemoryCache(mUrl, bitmap);
                    mTaskTags.remove(mUrl);
                    // 图片显示
                    mHandler.post(new Runnable() {

                        @Override
                        public void run() {
                            if (iv != null) {
                                display(iv, mUrl);
                            } else {
                                getBitMap(mUrl, mLister);
                            }
                        }
                    });
                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        private boolean downloadUrlToStream(OutputStream outputStream) {
            BufferedOutputStream out = null;
            BufferedInputStream in = null;
            try {
                Request request = new Request.Builder().url(mUrl).build();
                Response response = mClient.newCall(request).execute();

                if (response.isSuccessful()) {

                    in = new BufferedInputStream(response.body().byteStream(), 8 * 1024);
                    out = new BufferedOutputStream(outputStream, 8 * 1024);
                    int b;
                    while ((b = in.read()) != -1) {
                        out.write(b);
                    }
                    return true;
                }
            } catch (final IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (out != null) {
                        out.close();
                    }
                    if (in != null) {
                        in.close();
                    }
                } catch (final IOException e) {
                    e.printStackTrace();
                }
            }
            return false;
        }
    }

    private void checkViewTags(String url, ImageView view) {
        Set<ImageView> viewList = mViewTags.get(url);
        if (viewList == null) {
            viewList = new LinkedHashSet<ImageView>();
            mViewTags.put(url, viewList);
        }
        viewList.add(view);
    }

    private void checkListenerTags(String url, ImageLoadListener listener) {
        Set<ImageLoadListener> listenerList = mListenerTags.get(url);
        if (listenerList == null) {
            listenerList = new LinkedHashSet<ImageLoadListener>();
            mListenerTags.put(url, listenerList);
        }
        listenerList.add(listener);
    }

    private void setBitmap(String url, Bitmap bm) {
        try {
            if (bm == null) {
                return;
            }
            Set<ImageView> viewList = mViewTags.get(url);
            if (viewList == null) {
                return;
            }

            for (ImageView imgView : viewList) {
                if (imgView != null) {
                    imgView.setImageBitmap(bm);
                }
            }
            viewList.clear();
            mViewTags.remove(url);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void notifyBitMap(String url, Bitmap bm) {
        try {
            if (bm == null) {
                return;
            }
            Set<ImageLoadListener> listenerList = mListenerTags.get(url);
            if (listenerList == null) {
                return;
            }

            for (ImageLoadListener listener : listenerList) {
                if (listener != null) {
                    listener.onLoad(bm);
                }
            }
            listenerList.clear();
            mListenerTags.remove(url);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public interface ImageLoadListener {
        void onLoad(Bitmap bm);
    }
}
