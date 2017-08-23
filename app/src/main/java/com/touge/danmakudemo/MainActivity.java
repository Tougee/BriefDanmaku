package com.touge.danmakudemo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.SpannableString;
import android.view.View;

import com.touge.briefdanmaku.DanmakuItem;
import com.touge.briefdanmaku.DanmakuView;

import java.util.Random;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private DanmakuView danmakuView;
    private int count;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        danmakuView = (DanmakuView) findViewById(R.id.danmaku);
        findViewById(R.id.send_10).setOnClickListener(this);
        findViewById(R.id.start).setOnClickListener(this);
        findViewById(R.id.stop).setOnClickListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        danmakuView.finish();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.send_10:
                danmakuView.post(runnable);
                danmakuView.start();

                break;
            case R.id.start:
                danmakuView.start();
                break;
            case R.id.stop:
                danmakuView.stop();
                break;
        }
    }

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            long time = System.currentTimeMillis();
            DanmakuItem item = new DanmakuItem(
                    MainActivity.this,
                    new SpannableString(randomText()),
                    time + 50,
                    danmakuView.getWidth());
            danmakuView.addDanmaku(item);

            danmakuView.postDelayed(runnable, randomTime());
        }
    };

    private Random random = new Random();

    private int randomTime() {
        return random.nextInt(600);
    }

    private String randomText() {
        int x = random.nextInt(3);
        switch (x) {
            case 0:
                default:
                return "This is a danmaku " + count++;
            case 1:
                return "This is a very loooooooooooooooooooooooooong danmaku" + count++;
            case 2:
                return "This is a loooooooong danmaku" + count++;
        }
    }
}
