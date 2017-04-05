package com.jzy.demo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;

import com.jzy.aImage.library.BitmapHelper;
import com.jzy.aImage.library.IDownImageCallBack;

import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private ImageView img;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        img = (ImageView) findViewById(R.id.img);

        BitmapHelper helper = BitmapHelper.getInstance(MainActivity.this);
        helper.initDownListner(new IDownImageCallBack() {
            @Override
            public InputStream getStream(String url) {
                return getDownStream(url);
            }
        });

        helper.display(img, "https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1491985886&di=a125887cafaebdd9d56597fb24209333&imgtype=jpg&er=1&src=http%3A%2F%2Fs4.51cto.com%2Fwyfs02%2FM02%2F70%2F81%2FwKioL1W5e5GQU3qEAAEXNvlEskQ360.jpg-wh_651x363-s_2834990474.jpg");

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
