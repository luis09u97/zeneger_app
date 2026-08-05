package com.seunome.zeneger;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;

/** Animated profile mark for login and registration screens. */
public class AuthBrandAvatar extends FrameLayout {

    private View glowView;
    private View ringView;
    private FrameLayout coreView;
    private Animation floatAnim;
    private Animation pulseAnim;
    private Animation spinAnim;

    public AuthBrandAvatar(Context context) {
        super(context);
        init(context);
    }

    public AuthBrandAvatar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public AuthBrandAvatar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        int size = getResources().getDimensionPixelSize(R.dimen.brand_mark_size);
        int coreSize = Math.round(size * 0.68f);
        int iconSize = Math.round(coreSize * 0.52f);

        glowView = new View(context);
        LayoutParams glowParams = new LayoutParams(size, size);
        glowParams.gravity = Gravity.CENTER;
        glowView.setBackgroundResource(R.drawable.bg_auth_avatar_glow);
        addView(glowView, glowParams);

        ringView = new View(context);
        LayoutParams ringParams = new LayoutParams(size, size);
        ringParams.gravity = Gravity.CENTER;
        ringView.setBackgroundResource(R.drawable.bg_auth_avatar_ring);
        addView(ringView, ringParams);

        coreView = new FrameLayout(context);
        LayoutParams coreParams = new LayoutParams(coreSize, coreSize);
        coreParams.gravity = Gravity.CENTER;
        coreView.setBackgroundResource(R.drawable.bg_avatar);
        coreView.setElevation(dp(6));
        addView(coreView, coreParams);

        ImageView avatarIcon = new ImageView(context);
        LayoutParams iconParams = new LayoutParams(iconSize, iconSize);
        iconParams.gravity = Gravity.CENTER;
        avatarIcon.setImageResource(R.drawable.ic_zen_auth_avatar);
        coreView.addView(avatarIcon, iconParams);

        floatAnim = AnimationUtils.loadAnimation(context, R.anim.auth_float);
        pulseAnim = AnimationUtils.loadAnimation(context, R.anim.auth_pulse);
        spinAnim = AnimationUtils.loadAnimation(context, R.anim.auth_spin);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        coreView.startAnimation(floatAnim);
        glowView.startAnimation(pulseAnim);
        ringView.startAnimation(spinAnim);
    }

    @Override
    protected void onDetachedFromWindow() {
        coreView.clearAnimation();
        glowView.clearAnimation();
        ringView.clearAnimation();
        super.onDetachedFromWindow();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
