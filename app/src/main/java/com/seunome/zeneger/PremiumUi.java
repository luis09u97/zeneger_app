package com.seunome.zeneger;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.content.res.Configuration;
import android.content.SharedPreferences;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.app.AppCompatDelegate;

/** Shared presentation layer. It does not change navigation or business behavior. */
public final class PremiumUi {

    private static final Typeface DISPLAY = Typeface.create("sans-serif-black", Typeface.NORMAL);
    private static final Typeface EMPHASIS = Typeface.create("sans-serif-medium", Typeface.NORMAL);
    private static final Typeface BODY = Typeface.create("sans-serif", Typeface.NORMAL);
    private static final Typeface CAPTION = Typeface.create("sans-serif-light", Typeface.NORMAL);

    private PremiumUi() {}

    public static void apply(Activity activity) {
        SharedPreferences themePrefs = activity.getSharedPreferences("zeneger_prefs", Activity.MODE_PRIVATE);
        if (themePrefs.contains("dark_mode_override")) {
            int desiredMode = themePrefs.getBoolean("dark_mode_override", false)
                    ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
            if (AppCompatDelegate.getDefaultNightMode() != desiredMode) {
                AppCompatDelegate.setDefaultNightMode(desiredMode);
                return;
            }
        }
        boolean darkMode = isDarkMode(activity);
        activity.getWindow().setStatusBarColor(activity.getColor(R.color.bg_dark));
        activity.getWindow().setNavigationBarColor(activity.getColor(R.color.bg_dark));
        int systemUi = View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!darkMode) systemUi |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !darkMode) {
            systemUi |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        }
        activity.getWindow().getDecorView().setSystemUiVisibility(systemUi);

        ViewGroup content = activity.findViewById(android.R.id.content);
        if (content == null || content.getChildCount() == 0) return;
        View root = content.getChildAt(0);

        PremiumBackgroundDrawable background = new PremiumBackgroundDrawable(darkMode);
        root.setBackground(background);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || ValueAnimator.areAnimatorsEnabled()) {
            background.start();
        }
        root.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override public void onViewAttachedToWindow(View view) {
                if (!background.isRunning()) background.start();
            }
            @Override public void onViewDetachedFromWindow(View view) { background.stop(); }
        });

        if (!(activity instanceof ViewStoryActivity)) {
            int edge = activity.getResources().getDimensionPixelSize(R.dimen.responsive_edge_padding);
            if (edge > 0) {
                root.setPadding(root.getPaddingLeft() + edge, root.getPaddingTop(),
                        root.getPaddingRight() + edge, root.getPaddingBottom());
            }
        }

        styleTree(root, activity);
        reveal(root);
        root.post(() -> enhanceInteractions(root, activity, new int[]{0}));
    }

    public static void bindPasswordToggle(EditText field, ImageButton toggle) {
        field.setTransformationMethod(PasswordTransformationMethod.getInstance());
        toggle.setImageResource(R.drawable.ic_zen_visibility_off);
        toggle.setContentDescription("Mostrar senha");

        toggle.setOnClickListener(v -> {
            int selection = field.getSelectionEnd();
            boolean hidden = field.getTransformationMethod() instanceof PasswordTransformationMethod;
            if (hidden) {
                field.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                toggle.setImageResource(R.drawable.ic_zen_visibility);
                toggle.setContentDescription("Ocultar senha");
            } else {
                field.setTransformationMethod(PasswordTransformationMethod.getInstance());
                toggle.setImageResource(R.drawable.ic_zen_visibility_off);
                toggle.setContentDescription("Mostrar senha");
            }
            if (selection >= 0) {
                field.setSelection(Math.min(selection, field.getText().length()));
            }
        });
    }

    public static void styleDynamic(View root) {
        if (!(root.getContext() instanceof Activity)) return;
        Activity activity = (Activity) root.getContext();
        styleTree(root, activity);
        root.post(() -> enhanceInteractions(root, activity, new int[]{0}));
    }

    private static void styleTree(View view, Activity activity) {
        if (view instanceof TextView) styleText((TextView) view, activity);

        if (view.getBackground() instanceof ColorDrawable && view.getParent() != null) {
            int color = ((ColorDrawable) view.getBackground()).getColor();
            if (color == activity.getColor(R.color.bg_surface)) {
                view.setBackground(glassSurface(activity));
                view.setElevation(dp(activity, 5));
            } else if (color == activity.getColor(R.color.bg_toolbar)) {
                view.setBackground(toolbarGlass(activity));
                view.setElevation(dp(activity, 10));
            } else if (color == activity.getColor(R.color.primary_surface)) {
                view.setBackground(softGlass(activity));
                view.setElevation(dp(activity, 3));
            }
        }

        if (view instanceof ViewGroup && !(view instanceof RecyclerView)
                && view.getBackground() != null && view.getParent() instanceof View
                && ((View) view.getParent()).getId() != android.R.id.content) {
            view.setElevation(Math.max(view.getElevation(), dp(activity, 4)));
            view.setTranslationZ(Math.max(view.getTranslationZ(), dp(activity, 1)));
        }

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                styleTree(group.getChildAt(i), activity);
            }
        }
    }

    private static void styleText(TextView text, Activity activity) {
        float sp = text.getTextSize() / activity.getResources().getDisplayMetrics().scaledDensity;
        if (text instanceof EditText) {
            text.setTypeface(BODY);
            text.setLetterSpacing(0.006f);
        } else if (sp >= 24f) {
            text.setTypeface(DISPLAY);
            text.setLetterSpacing(0.018f);
        } else if (sp >= 16f || (text.getTypeface() != null && text.getTypeface().isBold())) {
            text.setTypeface(EMPHASIS);
            text.setLetterSpacing(0.012f);
        } else if (sp <= 12f) {
            text.setTypeface(CAPTION);
            text.setLetterSpacing(0.018f);
        } else {
            text.setTypeface(BODY);
            text.setLetterSpacing(0.008f);
        }
    }

    private static void reveal(View root) {
        root.setAlpha(0f);
        root.setScaleX(0.985f);
        root.setScaleY(0.985f);
        root.setTranslationY(root.getResources().getDisplayMetrics().density * 12f);
        root.animate().alpha(1f).scaleX(1f).scaleY(1f).translationY(0f)
                .setDuration(650L).setInterpolator(new DecelerateInterpolator()).start();
    }

    private static void enhanceInteractions(View view, Activity activity, int[] order) {
        if (view.isClickable()) {
            view.setElevation(Math.max(view.getElevation(), dp(activity, 5)));
            view.setTranslationZ(dp(activity, 1));
            view.setOnTouchListener((target, event) -> {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    float horizontalTilt = event.getX() < target.getWidth() / 2f ? -3.5f : 3.5f;
                    float verticalTilt = event.getY() < target.getHeight() / 2f ? 2f : -2f;
                    target.animate().scaleX(0.955f).scaleY(0.955f).alpha(0.92f)
                            .rotationY(horizontalTilt).rotationX(verticalTilt)
                            .translationZ(dp(activity, 3)).setDuration(105L)
                            .setInterpolator(new DecelerateInterpolator()).start();
                } else if (event.getActionMasked() == MotionEvent.ACTION_UP
                        || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                    target.animate().scaleX(1f).scaleY(1f).alpha(1f)
                            .rotationX(0f).rotationY(0f).translationZ(dp(activity, 1))
                            .setDuration(280L).setInterpolator(new OvershootInterpolator(1.7f)).start();
                }
                return false;
            });

            if (!(view instanceof RecyclerView) && order[0] < 18) {
                long delay = Math.min(order[0]++, 12) * 28L;
                view.setAlpha(0f);
                view.setTranslationY(dp(activity, 8));
                view.animate().alpha(1f).translationY(0f).setStartDelay(delay)
                        .setDuration(420L).setInterpolator(new DecelerateInterpolator()).start();
            }
        }

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                enhanceInteractions(group.getChildAt(i), activity, order);
            }
        }
    }

    private static GradientDrawable glassSurface(Activity activity) {
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{activity.getColor(R.color.surface_gradient_start),
                        activity.getColor(R.color.surface_gradient_center),
                        activity.getColor(R.color.surface_gradient_end)});
        drawable.setCornerRadius(dp(activity, 18));
        drawable.setStroke(dp(activity, 1), activity.getColor(R.color.control_stroke));
        return drawable;
    }

    private static GradientDrawable softGlass(Activity activity) {
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{activity.getColor(R.color.input_gradient_start),
                        activity.getColor(R.color.input_gradient_end)});
        drawable.setCornerRadius(dp(activity, 14));
        drawable.setStroke(dp(activity, 1), activity.getColor(R.color.control_stroke));
        return drawable;
    }

    private static GradientDrawable toolbarGlass(Activity activity) {
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{activity.getColor(R.color.toolbar_gradient_start),
                        activity.getColor(R.color.toolbar_gradient_center),
                        activity.getColor(R.color.toolbar_gradient_end)});
        drawable.setStroke(dp(activity, 1), activity.getColor(R.color.control_stroke));
        return drawable;
    }

    private static boolean isDarkMode(Activity activity) {
        int nightMode = activity.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        return nightMode == Configuration.UI_MODE_NIGHT_YES;
    }

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
