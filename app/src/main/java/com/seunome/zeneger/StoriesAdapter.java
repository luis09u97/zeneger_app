package com.seunome.zeneger;

import android.view.*;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import de.hdodenhof.circleimageview.CircleImageView;
import java.util.List;

public class StoriesAdapter extends RecyclerView.Adapter<StoriesAdapter.ViewHolder> {

    List<Story> stories;
    OnStoryClick listener;

    public interface OnStoryClick { void onClick(Story story); }

    public StoriesAdapter(List<Story> stories, OnStoryClick listener) {
        this.stories  = stories;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_story, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Story story = stories.get(position);
        holder.name.setText(story.userName);
        holder.time.setText(story.getTimeAgo());

        if (story.userName != null && !story.userName.isEmpty()) {
            holder.avatarLetter.setText(
                    String.valueOf(story.userName.charAt(0)).toUpperCase());
        }

        if (story.imageUrl != null && !story.imageUrl.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(story.imageUrl)
                    .circleCrop()
                    .into(holder.avatar);
            holder.avatarLetter.setText("");
        }

        holder.itemView.setOnClickListener(v -> listener.onClick(story));
    }

    @Override
    public int getItemCount() { return stories.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, time, avatarLetter;
        CircleImageView avatar;
        View ring;

        ViewHolder(View v) {
            super(v);
            name         = v.findViewById(R.id.storyUserName);
            time         = v.findViewById(R.id.storyTime);
            avatarLetter = v.findViewById(R.id.storyAvatarLetter);
            avatar       = v.findViewById(R.id.storyAvatar);
            ring         = v.findViewById(R.id.storyRing);
        }
    }
}