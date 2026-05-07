package com.example.uniwingman.ui.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.uniwingman.R;
import java.util.ArrayList;
import java.util.List;

public class AdminUserAdapter extends RecyclerView.Adapter<AdminUserAdapter.UserViewHolder> {

    private List<AdminUserItem> userList = new ArrayList<>();

    // Μέθοδος για να ενημερώνουμε τη λίστα δυναμικά
    public void setUsers(List<AdminUserItem> users) {
        this.userList = users;
        notifyDataSetChanged(); // Λέει στο UI να ξαναζωγραφίσει τη λίστα
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Συνδέει τον Adapter με το item_admin_user.xml
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        // Παίρνει τον χρήστη στη συγκεκριμένη θέση και βάζει τα κείμενα
        AdminUserItem user = userList.get(position);
        holder.tvName.setText(user.getUsername());
        holder.tvEmail.setText(user.getEmail());
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    // Το ViewHolder κρατάει τα TextViews για να μην τα ψάχνουμε συνέχεια
    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvEmail;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvAdminUserName);
            tvEmail = itemView.findViewById(R.id.tvAdminUserEmail);
        }
    }
}