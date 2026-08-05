package com.seunome.zeneger;

import android.view.*;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.ViewHolder> {

    List<User> users;
    OnAddClick listener;

    public interface OnAddClick { void onAdd(User user); }

    public SearchAdapter(List<User> users, OnAddClick listener) {
        this.users    = users;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_search_user, parent, false);
        PremiumUi.styleDynamic(view);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = users.get(position);
        holder.name.setText(user.name);
        holder.email.setText(user.email);
        if (user.name != null && !user.name.isEmpty()) {
            holder.avatarLetter.setText(
                    String.valueOf(user.name.charAt(0)).toUpperCase());
        }
        holder.addBtn.setOnClickListener(v -> {
            listener.onAdd(user);
            holder.addBtn.setText("✓ Adicionado");
            holder.addBtn.setAlpha(0.5f);
            holder.addBtn.setEnabled(false);
        });
    }

    @Override
    public int getItemCount() { return users.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, email, avatarLetter, addBtn;
        ViewHolder(View v) {
            super(v);
            name         = v.findViewById(R.id.userName);
            email        = v.findViewById(R.id.userEmail);
            avatarLetter = v.findViewById(R.id.avatarLetter);
            addBtn       = v.findViewById(R.id.addBtn);
        }
    }
}
