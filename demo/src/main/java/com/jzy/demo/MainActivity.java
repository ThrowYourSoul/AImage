package com.jzy.demo;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;

import com.jzy.aimage.BitmapHelper;

import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private ImageView img, img1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        img = (ImageView) findViewById(R.id.img);
        img1 = (ImageView) findViewById(R.id.img1);

        BitmapHelper helper = BitmapHelper.getInstance(MainActivity.this);
//        helper.initDownListner(new IDownImageCallBack() {
//            @Override
//            public InputStream getStream(String url) {
//                return getDownStream(url);
//            }
//        });

//        helper.display(img, "http://img.ivsky.com/img/tupian/slides/201707/02/hubo-007.jpg");
        helper.getBitMap("http://img.ivsky.com/img/tupian/slides/201707/02/hubo-007.jpg", new BitmapHelper.ImageLoadListener() {
            @Override
            public void onLoad(Bitmap bm) {
                img.setImageBitmap(bm);
            }
        });
        helper.getBitMap("http://img.ivsky.com/img/tupian/slides/201707/02/hubo-007.jpg", new BitmapHelper.ImageLoadListener() {
            @Override
            public void onLoad(Bitmap bm) {
                img1.setImageBitmap(bm);
            }
        });

    }

    private InputStream getDownStream(String url) {
        // step 1: 创建 OkHttpClient 对象
        OkHttpClient okHttpClient = new OkHttpClient();
        // step 2： 创建一个请求，不指定请求方法时默认是GET。
        Request.Builder requestBuilder = new Request.Builder().url(url);
        //可以省略，默认是GET请求
        requestBuilder.method("GET", null);

        // step 3：创建 Call 对象
        Call call = okHttpClient.newCall(requestBuilder.build());
        try {
            Response response = call.execute();
            if (response.isSuccessful()) {
                return response.body().byteStream();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
