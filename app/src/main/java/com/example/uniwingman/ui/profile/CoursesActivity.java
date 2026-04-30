package com.example.uniwingman.ui.profile;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.uniwingman.R;

public class CoursesActivity extends AppCompatActivity {

    public static final String ARG_STATUS = "status";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_courses);

        String status = getIntent().getStringExtra(ARG_STATUS);
        if (status == null) status = "passed";

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.courses_fragment_container,
                            CoursesFragment.newInstance(status))
                    .commit();
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }
}