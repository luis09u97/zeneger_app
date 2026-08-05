package com.seunome.zeneger;

import android.view.*;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    static final int VIEW_SENT     = 1;
    static final int VIEW_RECEIVED = 2;

    List<Message> messages;
    List<String> keys = new ArrayList<>();
    String myUid;
    OnMessageLongClick listener;

    public interface OnMessageLongClick {
        void onLongClick(int position, String key);
    }

    public ChatAdapter(List<Message> messages, String myUid, OnMessageLongClick listener) {
        this.messages = messages;
        this.myUid    = myUid;
        this.listener = listener;
    }

    public void setKeys(List<String> keys) {
        this.keys = new ArrayList<>(keys);
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).senderId.equals(myUid)
                ? VIEW_SENT : VIEW_RECEIVED;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = viewType == VIEW_SENT
                ? R.layout.item_message_sent
                : R.layout.item_message_received;
        View v = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        PremiumUi.styleDynamic(v);
        return new MsgHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message msg = messages.get(position);
        MsgHolder h = (MsgHolder) holder;

        if (msg.deletedForAll) {
            h.text.setText("🚫 Mensagem apagada");
            h.text.setAlpha(0.5f);
        } else {
            h.text.setText(msg.text);
            h.text.setAlpha(1f);
        }

        h.time.setText(msg.timestamp);

        if (h.readStatus != null) {
            if (msg.read) {
                h.readStatus.setText(" ✓✓");
                h.readStatus.setTextColor(0xFF4DA6FF);
            } else {
                h.readStatus.setText(" ✓");
                h.readStatus.setTextColor(0xFFCCE5FF);
            }
        }

        // Reações
        if (h.reactionText != null) {
            if (msg.reactions != null && !msg.reactions.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (String emoji : msg.reactions.values()) sb.append(emoji);
                h.reactionText.setText(sb.toString());
                h.reactionText.setVisibility(View.VISIBLE);
            } else {
                h.reactionText.setVisibility(View.GONE);
            }
        }

        h.itemView.setOnLongClickListener(v -> {
            if (!keys.isEmpty() && position < keys.size()) {
                listener.onLongClick(position, keys.get(position));
            }
            return true;
        });
    }

    @Override
    public int getItemCount() { return messages.size(); }

    static class MsgHolder extends RecyclerView.ViewHolder {
        TextView text, time, readStatus, reactionText;

        MsgHolder(View itemView) {
            super(itemView);
            text         = itemView.findViewById(R.id.messageText);
            time         = itemView.findViewById(R.id.timeText);
            readStatus   = itemView.findViewById(R.id.readStatus);
            reactionText = itemView.findViewById(R.id.reactionText);
        }
    }
}
