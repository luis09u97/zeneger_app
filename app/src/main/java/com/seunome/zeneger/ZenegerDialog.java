package com.seunome.zeneger;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** Premium-styled dialog matching the Zeneger visual system. */
public final class ZenegerDialog {

    public interface ChoiceListener {
        void onChoice(int index, String label);
    }

    public interface ConfirmListener {
        void onConfirm(Dialog dialog);
    }

    public interface CancelListener {
        void onCancel(Dialog dialog);
    }

    public interface InputListener {
        void onConfirm(Dialog dialog, String text);
    }

    public interface FieldsListener {
        void onConfirm(Dialog dialog, List<EditText> fields);
    }

    private ZenegerDialog() {}

    public static Builder on(Activity activity) {
        return new Builder(activity);
    }

    public static final class Builder {
        private final Activity activity;
        private String eyebrow = "ZENEGER";
        private String title;
        private String message;
        private int iconRes = R.drawable.ic_zen_info;
        private boolean showIcon = true;
        private String confirmText = "Salvar";
        private String cancelText = "Agora não";
        private boolean showConfirm = true;
        private boolean showCancel = true;
        private ConfirmListener confirmListener;
        private CancelListener cancelListener;
        private InputListener inputListener;
        private FieldsListener fieldsListener;
        private ChoiceListener choiceListener;
        private ChoiceListener itemListener;
        private String inputHint;
        private int inputType = InputType.TYPE_CLASS_TEXT;
        private int inputMinLines = 1;
        private int inputMaxLines = 1;
        private String inputPrefill;
        private final List<FieldSpec> fieldSpecs = new ArrayList<>();
        private String[] choices;
        private int selectedChoice = -1;
        private boolean dismissOnChoice = true;
        private boolean hideButtonsForChoices;

        private Builder(Activity activity) {
            this.activity = activity;
        }

        public Builder eyebrow(String value) {
            this.eyebrow = value;
            return this;
        }

        public Builder title(String value) {
            this.title = value;
            return this;
        }

        public Builder message(String value) {
            this.message = value;
            return this;
        }

        public Builder icon(int resId) {
            this.iconRes = resId;
            this.showIcon = true;
            return this;
        }

        public Builder hideIcon() {
            this.showIcon = false;
            return this;
        }

        public Builder confirm(String text, ConfirmListener listener) {
            this.confirmText = text;
            this.confirmListener = listener;
            this.showConfirm = true;
            return this;
        }

        public Builder confirmInput(String text, InputListener listener) {
            this.confirmText = text;
            this.inputListener = listener;
            this.showConfirm = true;
            return this;
        }

        public Builder confirmFields(String text, FieldsListener listener) {
            this.confirmText = text;
            this.fieldsListener = listener;
            this.showConfirm = true;
            return this;
        }

        public Builder cancel(String text) {
            this.cancelText = text;
            this.showCancel = true;
            return this;
        }

        public Builder cancel(String text, CancelListener listener) {
            this.cancelText = text;
            this.cancelListener = listener;
            this.showCancel = true;
            return this;
        }

        public Builder hideCancel() {
            this.showCancel = false;
            return this;
        }

        public Builder hideConfirm() {
            this.showConfirm = false;
            return this;
        }

        public Builder input(String hint) {
            return input(hint, InputType.TYPE_CLASS_TEXT);
        }

        public Builder input(String hint, int type) {
            this.inputHint = hint;
            this.inputType = type;
            return this;
        }

        public Builder inputLines(int min, int max) {
            this.inputMinLines = min;
            this.inputMaxLines = max;
            return this;
        }

        public Builder prefill(String value) {
            this.inputPrefill = value;
            return this;
        }

        public Builder field(String hint, int type) {
            fieldSpecs.add(new FieldSpec(hint, type));
            return this;
        }

        public Builder singleChoice(String[] options, int selected, ChoiceListener listener) {
            this.choices = options;
            this.selectedChoice = selected;
            this.choiceListener = listener;
            this.hideButtonsForChoices = true;
            return this;
        }

        public Builder items(String[] options, ChoiceListener listener) {
            this.choices = options;
            this.selectedChoice = -1;
            this.itemListener = listener;
            this.dismissOnChoice = true;
            this.hideButtonsForChoices = true;
            return this;
        }

        public Dialog show() {
            View content = LayoutInflater.from(activity).inflate(R.layout.dialog_zeneger, null, false);

            TextView eyebrowView = content.findViewById(R.id.dialogEyebrow);
            TextView titleView = content.findViewById(R.id.dialogTitle);
            TextView messageView = content.findViewById(R.id.dialogMessage);
            ImageView iconView = content.findViewById(R.id.dialogIcon);
            LinearLayout fieldsContainer = content.findViewById(R.id.dialogFields);
            LinearLayout choicesContainer = content.findViewById(R.id.dialogChoices);
            TextView cancelBtn = content.findViewById(R.id.dialogCancel);
            TextView confirmBtn = content.findViewById(R.id.dialogConfirm);
            View iconFrame = iconView != null ? (View) iconView.getParent() : null;

            if (eyebrowView != null) {
                if (eyebrow == null || eyebrow.isEmpty()) {
                    eyebrowView.setVisibility(View.GONE);
                } else {
                    eyebrowView.setText(eyebrow);
                }
            }

            if (titleView != null) {
                if (title == null || title.isEmpty()) {
                    titleView.setVisibility(View.GONE);
                } else {
                    titleView.setText(title);
                }
            }

            if (messageView != null) {
                if (message == null || message.isEmpty()) {
                    messageView.setVisibility(View.GONE);
                } else {
                    messageView.setText(message);
                }
            }

            if (iconView != null) {
                if (showIcon) {
                    iconView.setImageResource(iconRes);
                } else if (iconFrame != null) {
                    iconFrame.setVisibility(View.GONE);
                }
            }

            List<EditText> inputs = new ArrayList<>();
            if (fieldsContainer != null) {
                if (inputHint != null) {
                    fieldsContainer.setVisibility(View.VISIBLE);
                    EditText input = createField(inputHint, inputType, inputMinLines, inputMaxLines);
                    if (inputPrefill != null) input.setText(inputPrefill);
                    fieldsContainer.addView(input);
                    inputs.add(input);
                } else if (!fieldSpecs.isEmpty()) {
                    fieldsContainer.setVisibility(View.VISIBLE);
                    for (FieldSpec spec : fieldSpecs) {
                        EditText input = createField(spec.hint, spec.type, 1, 1);
                        fieldsContainer.addView(input);
                        inputs.add(input);
                    }
                }
            }

            Dialog dialog = new Dialog(activity);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(content);
            dialog.setCancelable(showCancel);

            if (choicesContainer != null && choices != null && choices.length > 0) {
                choicesContainer.setVisibility(View.VISIBLE);
                for (int i = 0; i < choices.length; i++) {
                    View row = LayoutInflater.from(activity)
                            .inflate(R.layout.item_zeneger_dialog_choice, choicesContainer, false);
                    TextView label = row.findViewById(R.id.choiceLabel);
                    TextView marker = row.findViewById(R.id.choiceMarker);
                    if (label != null) label.setText(choices[i]);

                    final int index = i;
                    final String labelText = choices[i];

                    if (choiceListener != null) {
                        boolean selected = i == selectedChoice;
                        if (marker != null) {
                            marker.setBackgroundResource(selected
                                    ? R.drawable.bg_choice_indicator_selected
                                    : R.drawable.bg_choice_indicator);
                            marker.setText(selected ? "✓" : "");
                        }
                        row.setOnClickListener(v -> {
                            choiceListener.onChoice(index, labelText);
                            if (dismissOnChoice) dialog.dismiss();
                        });
                    } else if (itemListener != null) {
                        if (marker != null) marker.setVisibility(View.GONE);
                        row.setOnClickListener(v -> {
                            itemListener.onChoice(index, labelText);
                            if (dismissOnChoice) dialog.dismiss();
                        });
                    }

                    choicesContainer.addView(row);
                }
            }

            if (hideButtonsForChoices) {
                if (cancelBtn != null) cancelBtn.setVisibility(View.GONE);
                if (confirmBtn != null) confirmBtn.setVisibility(View.GONE);
            } else {
                if (cancelBtn != null) {
                    if (showCancel) {
                        cancelBtn.setText(cancelText);
                        cancelBtn.setOnClickListener(v -> {
                            if (cancelListener != null) cancelListener.onCancel(dialog);
                            dialog.dismiss();
                        });
                    } else {
                        cancelBtn.setVisibility(View.GONE);
                    }
                }
                if (confirmBtn != null) {
                    if (showConfirm) {
                        confirmBtn.setText(confirmText);
                        confirmBtn.setOnClickListener(v -> {
                            if (fieldsListener != null) {
                                fieldsListener.onConfirm(dialog, inputs);
                            } else if (inputListener != null && !inputs.isEmpty()) {
                                inputListener.onConfirm(dialog, inputs.get(0).getText().toString().trim());
                            } else if (confirmListener != null) {
                                confirmListener.onConfirm(dialog);
                            } else {
                                dialog.dismiss();
                            }
                        });
                    } else {
                        confirmBtn.setVisibility(View.GONE);
                    }
                }
            }

            styleInputs(inputs);
            PremiumUi.styleDynamic(content);

            Window window = dialog.getWindow();
            if (window != null) {
                window.setBackgroundDrawableResource(android.R.color.transparent);
                WindowManager.LayoutParams params = window.getAttributes();
                int margin = activity.getResources().getDimensionPixelSize(R.dimen.responsive_edge_padding);
                params.width = ViewGroup.LayoutParams.MATCH_PARENT;
                params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                params.gravity = Gravity.CENTER;
                params.horizontalMargin = margin > 0 ? margin / (float) activity.getResources()
                        .getDisplayMetrics().widthPixels : 0.06f;
                window.setAttributes(params);
                window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                window.setDimAmount(0.62f);
            }

            dialog.show();
            return dialog;
        }

        private EditText createField(String hint, int type, int minLines, int maxLines) {
            EditText input = new EditText(activity);
            input.setHint(hint);
            input.setInputType(type);
            input.setMinLines(minLines);
            input.setMaxLines(maxLines);
            input.setBackground(fieldBackground());
            int pad = dp(14);
            input.setPadding(pad, pad, pad, pad);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = dp(10);
            input.setLayoutParams(lp);
            return input;
        }

        private GradientDrawable fieldBackground() {
            GradientDrawable drawable = new GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT,
                    new int[]{activity.getColor(R.color.input_gradient_start),
                            activity.getColor(R.color.input_gradient_end)});
            drawable.setCornerRadius(dp(14));
            drawable.setStroke(dp(1), activity.getColor(R.color.control_stroke));
            return drawable;
        }

        private void styleInputs(List<EditText> inputs) {
            for (EditText input : inputs) {
                input.setTextColor(activity.getColor(R.color.text_primary));
                input.setHintTextColor(activity.getColor(R.color.text_secondary));
            }
        }

        private int dp(int value) {
            return Math.round(value * activity.getResources().getDisplayMetrics().density);
        }
    }

    private static final class FieldSpec {
        final String hint;
        final int type;

        FieldSpec(String hint, int type) {
            this.hint = hint;
            this.type = type;
        }
    }
}
