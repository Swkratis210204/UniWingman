package com.example.uniwingman.ui.map;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.uniwingman.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private EditText etMapSearch;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        etMapSearch = view.findViewById(R.id.etMapSearch);

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        addOPABuildings();
        addTransport();
        addBookstores();

        // Κεντράρισμα στο Μαράσλειο
        LatLng opa = new LatLng(37.9927, 23.7347);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(opa, 15f));
    }

    private void addOPABuildings() {
        float color = BitmapDescriptorFactory.HUE_AZURE;

        addMarker(37.9927, 23.7347, "Μαράσλειο Μέγαρο", "Κεντρικό κτήριο ΟΠΑ · Πατησίων 76", color);
        addMarker(37.9940, 23.7340, "Κτήριο Τροίας", "Νέο κτήριο ΟΠΑ · Τροίας 2, Κιμώλου & Σπετσών", color);
        addMarker(37.9933, 23.7330, "Κτήριο Κοδριγκτώνος", "Γραφεία καθηγητών · Κοδριγκτώνος 12", color);
        addMarker(37.9945, 23.7355, "Κτήριο Ελπίδος", "Γραφείο Erasmus · Ελπίδος 13", color);
        addMarker(37.9985, 23.7420, "Κτήριο Ευελπίδων", "ΟΠΑ · Ευελπίδων 47Α & Λευκάδος 33", color);
    }

    private void addTransport() {
        float color = BitmapDescriptorFactory.HUE_GREEN;

        addMarker(37.9923, 23.7343,
                "Στάση: ΟΙΚΟΝΟΜΙΚΟ ΠΑΝΕΠΙΣΤΗΜΙΟ",
                "Λεωφ: 022, 054, 224, 500, 608, 622, Α8 · Τρόλ: 2, 3, 4, 5, 11, 14",
                color);
        addMarker(37.9938, 23.7318,
                "Στάση: Πανελλήνιος",
                "Λεωφ: 022, 224 · Τρόλ: 2, 4",
                color);
        addMarker(37.9908, 23.7298,
                "Μετρό: Βικτώρια",
                "Γραμμή 2 (Κόκκινη) · 5 λεπτά περπάτημα",
                color);
    }

    private void addBookstores() {
        float color = BitmapDescriptorFactory.HUE_ORANGE;

        // Κλειδάριθμος — Μάρνη 8
        addMarker(37.9895, 23.7310,
                "Κλειδάριθμος",
                "Βιβλιοπωλείο · Μάρνη 8 · Τηλ: 210 3300104",
                color);

        // Τζιόλα — Θεμιστοκλέους 73 (bookpoint)
        addMarker(37.9816, 23.7285,
                "Τζιόλα",
                "Επιστημονικό Βιβλιοπωλείο · Εύδοξος",
                color);

        // Πολιτεία — Ασκληπιού 1-3
        addMarker(37.9801, 23.7370,
                "Πολιτεία",
                "Βιβλιοπωλείο · Ασκληπιού 1-3",
                color);

        // Βιβλιοδιανομή ΟΠΑ — εντός campus
        addMarker(37.9925, 23.7350,
                "Βιβλιοδιανομή ΟΠΑ",
                "Σημείο παραλαβής Εύδοξος · Εντός Μαρασλείου",
                color);

        // Εκδόσεις Κρήτης — Σίνα 48
        addMarker(37.9790, 23.7380,
                "Εκδόσεις Κρήτης",
                "Βιβλιοπωλείο · Σίνα 48",
                color);

        // NewTech Pub — Σόλωνος 102
        addMarker(37.9810, 23.7350,
                "NewTech Pub",
                "Επιστημονικό Βιβλιοπωλείο · Σόλωνος 102",
                color);

        // Κλέψυδρα — Σόλωνος 52
        addMarker(37.9795, 23.7330,
                "Κλέψυδρα",
                "Βιβλιοπωλείο · Σόλωνος 52",
                color);
    }

    private void addMarker(double lat, double lng, String title, String snippet, float color) {
        mMap.addMarker(new MarkerOptions()
                .position(new LatLng(lat, lng))
                .title(title)
                .snippet(snippet)
                .icon(BitmapDescriptorFactory.defaultMarker(color)));
    }
}