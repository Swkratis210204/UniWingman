package com.example.uniwingman.ui.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow; // ΝΕΟ import για το custom dropdown
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.uniwingman.R;
import java.util.ArrayList;
import java.util.List;

public class AdminUserAdapter extends RecyclerView.Adapter<AdminUserAdapter.UserViewHolder> {

    private List<AdminUserItem> userList = new ArrayList<>();

    public void setUsers(List<AdminUserItem> users) {
        this.userList = users;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        AdminUserItem user = userList.get(position);
        holder.tvName.setText(user.getUsername());
        holder.tvEmail.setText(user.getEmail());

        // --- ΛΟΓΙΚΗ ΓΙΑ ΤΟ ΑΡΧΙΚΟ ΓΡΑΜΜΑ (AVATAR) ---
        String username = user.getUsername();
        if (username != null && !username.trim().isEmpty()) {
            // Παίρνουμε το 1ο γράμμα και το κάνουμε κεφαλαίο
            String firstLetter = username.trim().substring(0, 1).toUpperCase();
            holder.tvAvatar.setText(firstLetter);
        } else {
            // Default τιμή αν δεν υπάρχει όνομα
            holder.tvAvatar.setText("?");
        }

        // --- ΝΕΟ: ΛΟΓΙΚΗ ΓΙΑ ΤΟ CUSTOM DROPDOWN ΟΤΑΝ ΠΑΤΑΣ ΤΟΝ ΧΡΗΣΤΗ ---
        holder.itemView.setOnClickListener(v -> {
            // 1. Φορτώνουμε το πανέμορφο layout που σχεδιάσαμε (layout_dropdown_actions.xml)
            View popupView = LayoutInflater.from(v.getContext()).inflate(R.layout.layout_dropdown_actions, null);

            // 2. Δημιουργούμε το PopupWindow
            PopupWindow popupWindow = new PopupWindow(popupView,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    true); // Το true σημαίνει ότι κλείνει αν πατήσεις οπουδήποτε αλλού (outside touch)

            // Βάζουμε μια ωραία σκιά (elevation) για να "πεταχτεί" προς τα έξω
            popupWindow.setElevation(15f);

            // 3. Βρίσκουμε τα κουμπιά (TextViews) μέσα στο custom μενού μας
            TextView btnStats = popupView.findViewById(R.id.btnStats);
            TextView btnPassword = popupView.findViewById(R.id.btnPassword);
            TextView btnBan = popupView.findViewById(R.id.btnBan);
            TextView btnDelete = popupView.findViewById(R.id.btnDelete);

            // 4. Ορίζουμε τι θα κάνει το κάθε κουμπί (Προς το παρόν Mock)
            btnStats.setOnClickListener(view -> {
                popupWindow.dismiss(); // Κλείνει το μενού μόλις πατήσεις
                Toast.makeText(v.getContext(), "Στατιστικά για: " + user.getUsername() + " (Mock)", Toast.LENGTH_SHORT).show();
            });

            btnPassword.setOnClickListener(view -> {
                popupWindow.dismiss();
                Toast.makeText(v.getContext(), "Αίτημα ανάκτησης... (Mock)", Toast.LENGTH_SHORT).show();
            });

            btnBan.setOnClickListener(view -> {
                popupWindow.dismiss();
                Toast.makeText(v.getContext(), "Ο χρήστης " + user.getUsername() + " έγινε Ban! (Mock)", Toast.LENGTH_SHORT).show();
            });

            btnDelete.setOnClickListener(view -> {
                popupWindow.dismiss();
                Toast.makeText(v.getContext(), "Διαγραφή του " + user.getUsername() + "... (Mock)", Toast.LENGTH_SHORT).show();
            });

            // 5. Το εμφανίζουμε!
            // Το showAsDropDown το βάζει να "κρέμεται" λίγο πιο όμορφα κάτω από τη γραμμή με offsets (x: 50, y: -20)
            popupWindow.showAsDropDown(v, 50, -20);
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvEmail, tvAvatar;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvAdminUserName);
            tvEmail = itemView.findViewById(R.id.tvAdminUserEmail);
            tvAvatar = itemView.findViewById(R.id.tvAvatar);
        }
    }
}