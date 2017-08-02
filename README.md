### 简书文章地址：http://www.jianshu.com/p/d896a09b9aca

#### 文章更新说明，之前写的太乱直接删了，大家重新看这个就好，么么哒。
---
APP下载更新，所有的APP都有这样的功能，在6.0之前都是这么写的：
```
Intent intents = new Intent();
intents.setAction("android.intent.action.VIEW"); 
intents.addCategory("android.intent.category.DEFAULT");
intents.setData(uri);
intents.setDataAndType(Uri.fromFile(file),"application/vnd.android.package-archive");
intents.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
startActivity(intents);
```

但是6.0之后这样写部分手机会调不起来这个intent,是因为文件的**uri**地址不对。
7.0之后文件共享需要使用FileProvider的功能，**uri**的获取又不一样了。

因此，为了兼容这个问题，下面就给出正确的解决办法。

### 第一步，manifest文件中加入：
```
<provider
            android:name="android.support.v4.content.FileProvider"
            android:authorities="${applicationId}.provider"
            android:exported="false"
            android:grantUriPermissions="true">
            <meta-data
                android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/provider_paths"/>
        </provider>
<service android:name=".DownLoadService"/>
```
### 第二步，在res中创建xml文件夹，新建provider_paths.xml文件
```
<?xml version="1.0" encoding="utf-8"?>
<paths xmlns:android="http://schemas.android.com/apk/res/android">
    <external-path name="external_files" path="."/>
</paths>
```
### 第三步，创建一个可以后台下载APK的Service，直接复制下面的就行啦
```
import android.app.DownloadManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import java.io.File;

/**
 * 更新APP
 */
public class DownLoadService extends Service {

    private DownloadManager manager;
    private DownloadCompleteReceiver receiver;
    private String url;
    private String DOWNLOADPATH = "/demo/apk/";

    private void initDownManager() {
        manager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        receiver = new DownloadCompleteReceiver();
        DownloadManager.Request down = new DownloadManager.Request(Uri.parse(url));
        down.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE
                | DownloadManager.Request.NETWORK_WIFI);
        down.setAllowedOverRoaming(false);
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        String mimeString = mimeTypeMap.getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(url));
        down.setMimeType(mimeString);
        down.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
        down.setVisibleInDownloadsUi(true);
        down.setDestinationInExternalPublicDir(DOWNLOADPATH,"demo.apk");
        down.setTitle("demo");
        manager.enqueue(down);
        registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        url = intent.getStringExtra("downloadurl");
        String path = Environment.getExternalStorageDirectory().getAbsolutePath()+ DOWNLOADPATH + "demo.apk";
        File file = new File(path);
        if(file.exists()){
            deleteFileWithPath(path);
        }
        try{
            initDownManager();
        }catch (Exception e){
            e.printStackTrace();
            try {
                Uri uri = Uri.parse("market://details?id=" + getPackageName());
                Intent intent0 = new Intent(Intent.ACTION_VIEW, uri);
                intent0.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent0);
            } catch (Exception ex) {
                Toast.makeText(getApplicationContext(),"下载失败",Toast.LENGTH_SHORT).show();
            }
        }
        return Service.START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {

        return null;
    }

    @Override
    public void onDestroy() {
        if (receiver != null)
            unregisterReceiver(receiver);
        super.onDestroy();
    }

    class DownloadCompleteReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals(DownloadManager.ACTION_DOWNLOAD_COMPLETE)) {
                long downId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if(manager.getUriForDownloadedFile(downId)!=null){
                    installAPK(context,getRealFilePath(context,manager.getUriForDownloadedFile(downId)));
                }else{
                    Toast.makeText(context,"下载失败",Toast.LENGTH_SHORT).show();
                }
                DownLoadService.this.stopSelf();
            }
        }

        private void installAPK(Context context,String path) {
            File file = new File(path);
            if(file.exists()){
                openFile(file,context);
            }else{
                Toast.makeText(context,"下载失败",Toast.LENGTH_SHORT).show();
            }
        }
    }

    public String getRealFilePath(Context context, Uri uri) {
        if (null == uri) return null;
        final String scheme = uri.getScheme();
        String data = null;
        if (scheme == null)
            data = uri.getPath();
        else if (ContentResolver.SCHEME_FILE.equals(scheme)) {
            data = uri.getPath();
        } else if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
            Cursor cursor = context.getContentResolver().query(uri, new String[]{MediaStore.Images.ImageColumns.DATA}, null, null, null);
            if (null != cursor) {
                if (cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                    if (index > -1) {
                        data = cursor.getString(index);
                    }
                }
                cursor.close();
            }
        }
        return data;
    }

/**
*重点在这里
*/
    public void openFile(File var0, Context var1) {
        Intent var2 = new Intent();
        var2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        var2.setAction(Intent.ACTION_VIEW);
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N){
            Uri uriForFile = FileProvider.getUriForFile(var1, var1.getApplicationContext().getPackageName() + ".provider", var0);
            var2.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            var2.setDataAndType(uriForFile, var1.getContentResolver().getType(uriForFile));
        }else{
            var2.setDataAndType(Uri.fromFile(var0), getMIMEType(var0));
        }
        try {
            var1.startActivity(var2);
        } catch (Exception var5) {
            var5.printStackTrace();
            Toast.makeText(var1, "没有找到打开此类文件的程序", Toast.LENGTH_SHORT).show();
        }
    }
    public String getMIMEType(File var0) {
        String var1 = "";
        String var2 = var0.getName();
        String var3 = var2.substring(var2.lastIndexOf(".") + 1, var2.length()).toLowerCase();
        var1 = MimeTypeMap.getSingleton().getMimeTypeFromExtension(var3);
        return var1;
    }

    public static boolean deleteFileWithPath(String filePath) {
        SecurityManager checker = new SecurityManager();
        File f = new File(filePath);
        checker.checkDelete(filePath);
        if (f.isFile()) {
            f.delete();
            return true;
        }
        return false;
    }

}
```
### 第四步，**测试**
在MainActivity中获取点击下载的按钮，我们来开启下载任务。

```
<?xml version="1.0" encoding="utf-8"?>
<android.support.constraint.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context="xiangshang.xiangshang.com.demo.MainActivity">

    <Button
        android:id="@+id/btDownload"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="点击下载"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintLeft_toLeftOf="parent"
        app:layout_constraintRight_toRightOf="parent"
        app:layout_constraintTop_toTopOf="parent" />

</android.support.constraint.ConstraintLayout>
```

```
import android.Manifest;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.tbruyelle.rxpermissions.RxPermissions;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btDownload).setOnClickListener(v->{
            
          RxPermissions.getInstance(MainActivity.this).request(Manifest.permission.WRITE_EXTERNAL_STORAGE).subscribe(granted -> {
                if (granted) {
                    Intent service = new Intent(MainActivity.this, DownLoadService.class);
                    service.putExtra("downloadurl", "http://www.51yi.org/app/Volunteer.apk");
                    Toast.makeText(MainActivity.this,"正在下载中",Toast.LENGTH_SHORT).show();
                    startService(service);
                } else {
                    Toast.makeText(MainActivity.this,"SD卡下载权限被拒绝",Toast.LENGTH_SHORT).show();
                }
            });
        });

    }
}
```
这里用到了RxPermissions和Lambda表达式，需要在gradle里配置一下。

```
apply plugin: 'com.android.application'

android {
    compileSdkVersion 25
    buildToolsVersion "25.0.2"
    defaultConfig {
        applicationId "xiangshang.xiangshang.com.demo"
        minSdkVersion 15
        targetSdkVersion 25
        versionCode 1
        versionName "1.0"
        testInstrumentationRunner "android.support.test.runner.AndroidJUnitRunner"
        jackOptions{
            enabled true
        }
    }
    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.pro'
        }
    }
}

dependencies {
    compile fileTree(dir: 'libs', include: ['*.jar'])
    androidTestCompile('com.android.support.test.espresso:espresso-core:2.2.2', {
        exclude group: 'com.android.support', module: 'support-annotations'
    })
    compile 'com.android.support:appcompat-v7:25.2.0'
    compile 'com.android.support.constraint:constraint-layout:1.0.2'
    testCompile 'junit:junit:4.12'
    compile 'com.jakewharton.rxbinding:rxbinding:0.4.0'
    compile 'com.tbruyelle.rxpermissions:rxpermissions:0.8.0@aar'
}
```

重点在openFile(File var0, Context var1)这个方法中哦。

**好了，以上就是Demo的全部代码，我就不传Demo了，经过测试，完美适配6.0，7.0等系统。**

---


