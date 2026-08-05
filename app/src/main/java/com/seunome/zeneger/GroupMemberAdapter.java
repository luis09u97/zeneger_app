package com.seunome.zeneger;

import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.*;

public class GroupMemberAdapter extends RecyclerView.Adapter<GroupMemberAdapter.ViewHolder> {

    List<User> users;
    Set<String> selectedIds;

    public GroupMemberAdapter(List<User> users, Set<String> selectedIds) {
        this.users = users;
        this.selectedIds = selectedIds;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_group_member, parent, false);
        PremiumUi.styleDynamic(v);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = users.get(position);
        holder.name.setText(user.name);
        if (user.name != null && !user.name.isEmpty()) {
            holder.avatarLetter.setText(String.valueOf(user.name.charAt(0)).toUpperCase());
        }
        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(selectedIds.contains(user.uid));
        holder.checkBox.setOnCheckedChangeListener((btn, checked) -> {
            if (checked) selectedIds.add(user.uid);
            else selectedIds.remove(user.uid);
        });

        holder.itemView.setOnClickListener(v -> holder.checkBox.toggle());
    }

    @Override
    public int getItemCount() { return users.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, avatarLetter;
        CheckBox checkBox;
        ViewHolder(View v) {
            super(v);
            name         = v.findViewById(R.id.memberName);
            avatarLetter = v.findViewById(R.id.memberAvatarLetter);
            checkBox     = v.findViewById(R.id.memberCheckbox);
        }
    }
}
