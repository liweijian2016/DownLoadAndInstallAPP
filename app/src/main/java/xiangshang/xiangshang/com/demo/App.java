package xiangshang.xiangshang.com.demo;

import android.app.Application;
import android.util.Log;

/**
 * Created by ZhongZiMing on 2017/3/21.
 */

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Thread.setDefaultUncaughtExceptionHandler(new MyUncaughtExceptionHandler());
    }

    /**
     * 抓取错误
     */
    private class MyUncaughtExceptionHandler implements
            Thread.UncaughtExceptionHandler {
        @Override
        public void uncaughtException(Thread thread, Throwable ex) {
            Log.e("DEMO","发现了错误:");
            ex.printStackTrace();
        }
    }

}
