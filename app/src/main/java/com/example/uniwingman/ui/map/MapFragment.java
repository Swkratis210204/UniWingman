package com.example.uniwingman.ui.map;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.uniwingman.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private EditText etMapSearch;

    private BottomSheetBehavior<View> bottomSheetBehavior;
    private TextView tvMarkerTitle, tvMarkerSnippet, tvMarkerWebsite;
    private ImageView ivMarkerIcon;

    private BitmapDescriptor iconOpa, iconTransport, iconBook;

    private static final String TAG_OPA       = "opa";
    private static final String TAG_TRANSPORT = "transport";
    private static final String TAG_BOOK      = "book";

    // Data class για marker info
    private static class MarkerInfo {
        String tag;
        String website;
        MarkerInfo(String tag, String website) {
            this.tag = tag;
            this.website = website;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etMapSearch      = view.findViewById(R.id.etMapSearch);
        tvMarkerTitle    = view.findViewById(R.id.tvMarkerTitle);
        tvMarkerSnippet  = view.findViewById(R.id.tvMarkerSnippet);
        tvMarkerWebsite  = view.findViewById(R.id.tvMarkerWebsite);
        ivMarkerIcon     = view.findViewById(R.id.ivMarkerIcon);

        iconOpa       = bitmapFromDrawable(requireContext(), R.drawable.marker_opa);
        iconTransport = bitmapFromDrawable(requireContext(), R.drawable.marker_transport);
        iconBook      = bitmapFromDrawable(requireContext(), R.drawable.marker_book);

        View bottomSheet = view.findViewById(R.id.bottomSheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private BitmapDescriptor bitmapFromDrawable(Context context, int drawableId) {
        var drawable = ContextCompat.getDrawable(context, drawableId);
        assert drawable != null;
        int size = 80;
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, size, size);
        drawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override public View getInfoWindow(@NonNull Marker marker) { return new View(requireContext()); }
            @Override public View getInfoContents(@NonNull Marker marker) { return null; }
        });

        addOPABuildings();
        addTransport();
        addBookstores();

        LatLng opa = new LatLng(37.9947, 23.7318);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(opa, 15f));

        mMap.setOnMarkerClickListener(marker -> {
            MarkerInfo info = (MarkerInfo) marker.getTag();
            if (info != null) {
                showBottomSheet(marker.getTitle(), marker.getSnippet(), info.tag, info.website);
            }
            return true;
        });

        mMap.setOnMapClickListener(latLng ->
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN));
    }

    private void showBottomSheet(String title, String snippet, String tag, String website) {
        tvMarkerTitle.setText(title);
        tvMarkerSnippet.setText(snippet != null ? snippet.replace(" · ", "\n") : "");

        if (TAG_TRANSPORT.equals(tag)) {
            ivMarkerIcon.setImageResource(R.drawable.marker_transport);
        } else if (TAG_BOOK.equals(tag)) {
            ivMarkerIcon.setImageResource(R.drawable.marker_book);
        } else {
            ivMarkerIcon.setImageResource(R.drawable.marker_opa);
        }

        // Website link
        if (website != null) {
            tvMarkerWebsite.setVisibility(View.VISIBLE);
            tvMarkerWebsite.setText("🌐 " + website.replace("https://www.", ""));
            tvMarkerWebsite.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(website));
                startActivity(intent);
            });
        } else {
            tvMarkerWebsite.setVisibility(View.GONE);
        }

        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    private void addOPABuildings() {
        addMarker(37.99468, 23.73185, "Μαράσλειο Μέγαρο",    "Κεντρικό κτήριο ΟΠΑ · Πατησίων 76",             null, iconOpa, TAG_OPA);
        addMarker(37.99590, 23.73612, "Κτήριο Τροίας",        "Νέο κτήριο ΟΠΑ · Τροίας 2, Κιμώλου & Σπετσών", null, iconOpa, TAG_OPA);
        addMarker(37.99567, 23.73310, "Κτήριο Κοδριγκτώνος", "Γραφεία καθηγητών · Κοδριγκτώνος 12",           null, iconOpa, TAG_OPA);
        addMarker(37.99619, 23.73947, "Κτήριο Ευελπίδων 47Α","ΟΠΑ · Ευελπίδων 47Α & Λευκάδος 33",             null, iconOpa, TAG_OPA);
        addMarker(37.99547, 23.73692, "Κτήριο Ευελπίδων 29", "ΟΠΑ · Ευελπίδων 29",                            null, iconOpa, TAG_OPA);
    }

    private void addTransport() {
        addMarker(37.99339083367353, 23.731834898693943, "Στάση: ΟΙΚΟΝΟΜΙΚΟ ΠΑΝΕΠΙΣΤΗΜΙΟ",
                "Λεωφ: 022, 054, 224, 500, 608, 622, Α8 · Τρόλ: 2, 3, 4, 5, 11, 14",
                null, iconTransport, TAG_TRANSPORT);
        addMarker(37.99430398957923, 23.733090172488083, "Στάση: Πανελλήνιος",
                "Λεωφ: 022, 224 · Τρόλ: 2, 4",
                null, iconTransport, TAG_TRANSPORT);
        addMarker(37.99302725917662, 23.73049379421302, "Μετρό: Βικτώρια",
                "Γραμμή 1 (Πράσινη) · 5 λεπτά περπάτημα",
                null, iconTransport, TAG_TRANSPORT);
    }

    private void addBookstores() {
        addMarker(37.98936, 23.72993, "Κλειδάριθμος",
                "Βιβλιοπωλείο · Μάρνη 8 · Τηλ: 210 3300104 · sales@klidarithmos.gr · Δευτ-Παρ: 08:00-16:00 · Σαβ-Κυρ: Κλειστό",
                "https://www.klidarithmos.gr", iconBook, TAG_BOOK);

        addMarker(37.98876, 23.72905, "Τζιόλα",
                "Επιστημονικό Βιβλιοπωλείο · 3ης Σεπτεμβρίου 41Α · Τηλ: 210 3632600 · info@tziola.gr · Δευτ-Παρ: 09:00-17:00 · Σαβ: Κλειστό",
                "https://www.tziola.gr", iconBook, TAG_BOOK);

        addMarker(37.98183, 23.73457, "Πολιτεία",
                "Βιβλιοπωλείο · Ασκληπιού 1-3 · Τηλ: 210 3600235 · politeia@otenet.gr · Δευτ-Παρ: 09:00-21:00 · Σαβ: 09:00-18:00 · Κυρ: Κλειστό",
                "https://www.politeianet.gr", iconBook, TAG_BOOK);

        addMarker(37.99354, 23.73232, "Βιβλιοδιανομή ΟΠΑ",
                "Διανομή Βιβλίων · Αντωνιάδου 2 · Τηλ: 210 8203745 · Δευτ-Παρ: 09:00-13:00 · Σαβ-Κυρ: Κλειστό",
                null, iconBook, TAG_BOOK);

        addMarker(37.98233, 23.73483, "Εκδόσεις Κρήτης",
                "Βιβλιοπωλείο · Ιπποκράτους 10-12 · Τηλ: 210 2207940 · Δευτ-Παρ: 09:00-17:00 · Σαβ-Κυρ: Κλειστό",
                null, iconBook, TAG_BOOK);

        addMarker(37.98685, 23.73300, "NewTech Pub",
                "Βιβλιοπωλείο · Σολωμού 24 · Τηλ: 210 3845594 · contact@newtech-pub.com · Δευτ-Παρ: 09:00-18:30 · Σαβ-Κυρ: Κλειστό",
                "https://www.newtech-pub.com", iconBook, TAG_BOOK);

        addMarker(37.98745, 23.73044, "Εκδόσεις Παπασωτηρίου",
                "Βιβλιοπωλείο · Στουρνάρη 49Α · Τηλ: 210 3800008 · publish@papasotiriou.gr · Δευτ-Παρ: 09:00-17:00 · Σαβ-Κυρ: Κλειστό",
                "https://www.ekdoseis-papasotiriou.gr", iconBook, TAG_BOOK);
    }

    private void addMarker(double lat, double lng, String title, String snippet,
                           String website, BitmapDescriptor icon, String tag) {
        Marker marker = mMap.addMarker(new MarkerOptions()
                .position(new LatLng(lat, lng))
                .title(title)
                .snippet(snippet)
                .icon(icon));
        if (marker != null) marker.setTag(new MarkerInfo(tag, website));
    }
}