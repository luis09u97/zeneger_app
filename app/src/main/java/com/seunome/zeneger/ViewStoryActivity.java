package com.seunome.zeneger;

import android.os.Bundle;
import android.os.Handler;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import de.hdodenhof.circleimageview.CircleImageView;

public class ViewStoryActivity extends AppCompatActivity {

    ImageView storyImageView;
    ProgressBar storyProgress;
    TextView storyViewerName, storyViewerTime;
    CircleImageView storyUserAvatar;
    Handler handler = new Handler();
    int progressValue = 0;
    static final int STORY_DURATION = 5000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_story);
        PremiumUi.apply(this);

        storyImageView  = findViewById(R.id.storyImageView);
        storyProgress   = findViewById(R.id.storyProgress);
        storyViewerName = findViewById(R.id.storyViewerName);
        storyViewerTime = findViewById(R.id.storyViewerTime);
        storyUserAvatar = findViewById(R.id.storyUserAvatar);

        String imageUrl = getIntent().getStringExtra("imageUrl");
        String userName = getIntent().getStringExtra("userName");
        String timeAgo  = getIntent().getStringExtra("timeAgo");

        if (storyViewerName != null) storyViewerName.setText(userName);
        if (storyViewerTime != null) storyViewerTime.setText(timeAgo);

        if (imageUrl != null && !imageUrl.isEmpty()) {
            try {
                Glide.with(this).load(imageUrl).into(storyImageView);
                if (storyUserAvatar != null)
                    Glide.with(this).load(imageUrl).circleCrop().into(storyUserAvatar);
            } catch (Exception ignored) {}
        }

        android.view.View closeBtn = findViewById(R.id.storyCloseBtn);
        if (closeBtn != null) closeBtn.setOnClickListener(v -> finish());

        startProgress();
    }

    private void startProgress() {
        progressValue = 0;
        if (storyProgress != null) storyProgress.setProgress(0);

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                progressValue += 2;
                if (storyProgress != null) storyProgress.setProgress(progressValue);
                if (progressValue < 100) {
                    handler.postDelayed(this, STORY_DURATION / 50);
                } else {
                    finish();
                }
            }
        }, STORY_DURATION / 50);
    }

    @Override
    protected void onResume() {
        super.onResume();
        ZenegerApp.activityStarted();
    }

    @Override
    protected void onPause() {
        super.onPause();
        ZenegerApp.activityStopped();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}
