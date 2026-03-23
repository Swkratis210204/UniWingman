package com.example.uniwingman;

import org.junit.Test;
import io.github.cdimascio.dotenv.Dotenv;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnectionTest {

    @Test
    public void testSupabaseConnection() {
        // 1. Φόρτωση του .env (το "../" λέει στην Java να βγει από τον φάκελο test και να πάει στο root)
        Dotenv dotenv = Dotenv.configure()
                .directory("../") // Οι δύο τελείες λένε: "βγες έξω από το app και πήγαινε στον κεντρικό φάκελο"
                .load();

        String url = dotenv.get("DB_URL");
        String user = dotenv.get("DB_USER");
        String password = dotenv.get("DB_PASSWORD");

        System.out.println("⏳ Ξεκινάει η δοκιμή σύνδεσης στο Supabase...");
        System.out.println("🔗 URL: " + url);

        // 2. Προσπάθεια σύνδεσης
        try (Connection connection = DriverManager.getConnection(url, user, password)) {
            if (connection != null && !connection.isClosed()) {
                System.out.println("✅ ΕΠΙΤΥΧΙΑ: Η σύνδεση με τη βάση δεδομένων έγινε κανονικά!");
            }
        } catch (SQLException e) {
            System.err.println("❌ ΑΠΟΤΥΧΙΑ: Δεν ήταν δυνατή η σύνδεση.");
            System.err.println("Σφάλμα: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
