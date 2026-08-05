package com.seunome.zeneger;

import android.content.Context;
import android.view.*;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import de.hdodenhof.circleimageview.CircleImageView;
import java.text.SimpleDateFormat;
import java.util.*;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {

    List<Conversation> conversations;
    OnConversationClick listener;
    OnConversationLongClick longClickListener;

    public interface OnConversationClick    { void onClick(Conversation conv); }
    public interface OnConversationLongClick { void onLongClick(Conversation conv); }

    public UserAdapter(List<Conversation> conversations, OnConversationClick listener) {
        this.conversations = conversations;
        this.listener      = listener;
    }

    public void setOnLongClickListener(OnConversationLongClick l) {
        this.longClickListener = l;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user, parent, false);
        PremiumUi.styleDynamic(view);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (position >= conversations.size()) return;
        Conversation conv = conversations.get(position);
        if (conv == null) return;

        try {
            // Animação de item
            try {
                holder.itemView.startAnimation(
                        AnimationUtils.loadAnimation(holder.itemView.getContext(), R.anim.item_anim));
            } catch (Exception ignored) {}

            // Nome
            if (holder.name != null)
                holder.name.setText(conv.name != null ? conv.name : "Usuário");

            // Badges
            if (holder.groupBadge != null)
                holder.groupBadge.setVisibility(
                        conv.type == Conversation.TYPE_GROUP ? View.VISIBLE : View.GONE);

            if (holder.favoriteBadge != null)
                holder.favoriteBadge.setVisibility(conv.favorite ? View.VISIBLE : View.GONE);

            // Não lidas
            if (holder.unreadDot != null) {
                if (conv.unread && conv.unreadCount > 0) {
                    holder.unreadDot.setVisibility(View.VISIBLE);
                    holder.unreadDot.setText(
                            conv.unreadCount > 99 ? "99+" : String.valueOf(conv.unreadCount));
                } else {
                    holder.unreadDot.setVisibility(View.GONE);
                }
            }

            // Online dot
            if (holder.onlineDot != null)
                holder.onlineDot.setVisibility(
                        conv.online && conv.type == Conversation.TYPE_USER
                                ? View.VISIBLE : View.GONE);

            // Avatar
            if (conv.photoUrl != null && !conv.photoUrl.isEmpty()) {
                if (holder.avatar != null) {
                    holder.avatar.setVisibility(View.VISIBLE);
                    try {
                        Glide.with(holder.itemView.getContext())
                                .load(conv.photoUrl)
                                .placeholder(R.drawable.bg_avatar)
                                .error(R.drawable.bg_avatar)
                                .circleCrop()
                                .into(holder.avatar);
                    } catch (Exception ignored) {}
                }
                if (holder.avatarLetter != null) holder.avatarLetter.setVisibility(View.GONE);
            } else {
                if (holder.avatar != null) holder.avatar.setVisibility(View.GONE);
                if (holder.avatarLetter != null) {
                    holder.avatarLetter.setVisibility(View.VISIBLE);
                    holder.avatarLetter.setText(
                            conv.name != null && !conv.name.isEmpty()
                                    ? String.valueOf(conv.name.charAt(0)).toUpperCase() : "?");
                }
            }

            // Status
            if (holder.status != null) {
                if (conv.type == Conversation.TYPE_GROUP) {
                    holder.status.setText(
                            conv.lastMessage != null && !conv.lastMessage.isEmpty()
                                    ? conv.lastMessage : "Grupo");
                    holder.status.setTextColor(0xFF6B7280);
                } else if (conv.online) {
                    holder.status.setText("online agora");
                    holder.status.setTextColor(0xFF22C55E);
                } else if (conv.lastSeen != null && !conv.lastSeen.isEmpty()) {
                    try {
                        long ms = Long.parseLong(conv.lastSeen);
                        String fmt = new SimpleDateFormat("dd/MM HH:mm",
                                Locale.getDefault()).format(new Date(ms));
                        holder.status.setText("visto " + fmt);
                        holder.status.setTextColor(0xFF9CA3AF);
                    } catch (Exception e) {
                        holder.status.setText("Toque para conversar");
                        holder.status.setTextColor(0xFF9CA3AF);
                    }
                } else {
                    holder.status.setText("Toque para conversar");
                    holder.status.setTextColor(0xFF9CA3AF);
                }
            }

            // Hora
            if (holder.timeText != null) {
                if (conv.lastMessageTime != null && !conv.lastMessageTime.isEmpty()) {
                    try {
                        long ms   = Long.parseLong(conv.lastMessageTime);
                        long diff = System.currentTimeMillis() - ms;
                        SimpleDateFormat sdf = diff < 24 * 60 * 60 * 1000
                                ? new SimpleDateFormat("HH:mm", Locale.getDefault())
                                : new SimpleDateFormat("dd/MM", Locale.getDefault());
                        holder.timeText.setText(sdf.format(new Date(ms)));
                    } catch (Exception e) {
                        holder.timeText.setText("");
                    }
                } else {
                    holder.timeText.setText("");
                }
            }

        } catch (Exception e) {
            android.util.Log.e("ZENEGER", "Adapter error: " + e.getMessage());
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(conv);
        });
        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) longClickListener.onLongClick(conv);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return conversations != null ? conversations.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, avatarLetter, status, unreadDot, timeText;
        View groupBadge, favoriteBadge;
        CircleImageView avatar;
        View onlineDot;

        ViewHolder(View v) {
            super(v);
            name          = v.findViewById(R.id.usernameText);
            avatarLetter  = v.findViewById(R.id.avatarLetter);
            status        = v.findViewById(R.id.statusText);
            avatar        = v.findViewById(R.id.profileImage);
            groupBadge    = v.findViewById(R.id.groupBadge);
            favoriteBadge = v.findViewById(R.id.favoriteBadge);
            unreadDot     = v.findViewById(R.id.unreadDot);
            timeText      = v.findViewById(R.id.timeText);
            onlineDot     = v.findViewById(R.id.onlineDot);
        }
    }
}
