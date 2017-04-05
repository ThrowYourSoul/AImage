package com.jzy.aImage.library;

import java.io.InputStream;

/**
 * Created by JiangZhuyang on 2017/4/5.
 */

public interface IDownImageCallBack {
    InputStream getStream(String url);
}
