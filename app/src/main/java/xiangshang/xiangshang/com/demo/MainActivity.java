package xiangshang.xiangshang.com.demo;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.tbruyelle.rxpermissions.RxPermissions;

public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btDownload).setOnClickListener(v -> {
            Intent service = new Intent(MainActivity.this, DownLoadService.class);
//            service.putExtra("downloadurl", "http://www.51yi.org/app/Volunteer.apk");
            service.putExtra("downloadurl", "http://gftoutiao.com/app-XianXia2.0.1.apk");
            RxPermissions.getInstance(MainActivity.this)
                    .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    .subscribe(granted -> {
                        if (granted) {
                            Toast.makeText(MainActivity.this, "正在下载中", Toast.LENGTH_SHORT).show();
                            startService(service);
                        } else {
                            Toast.makeText(MainActivity.this, "SD卡下载权限被拒绝", Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }

}
