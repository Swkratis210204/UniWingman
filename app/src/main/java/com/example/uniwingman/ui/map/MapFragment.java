package com.example.uniwingman.ui.map;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.util.ArrayList;
import java.util.List;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private EditText etMapSearch;
    private ImageView ivClearSearch;
    private Button btnDirections;

    private BottomSheetBehavior<View> bottomSheetBehavior;
    private TextView tvMarkerTitle, tvMarkerSnippet, tvMarkerWebsite;
    private ImageView ivMarkerIcon;

    private BitmapDescriptor iconOpa, iconTransport, iconBook;

    private static final String TAG_OPA       = "opa";
    private static final String TAG_TRANSPORT = "transport";
    private static final String TAG_BOOK      = "book";

    private final List<Marker> allMarkers = new ArrayList<>();
    private LatLng currentMarkerPosition = null;

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
        ivClearSearch    = view.findViewById(R.id.ivClearSearch);
        tvMarkerTitle    = view.findViewById(R.id.tvMarkerTitle);
        tvMarkerSnippet  = view.findViewById(R.id.tvMarkerSnippet);
        tvMarkerWebsite  = view.findViewById(R.id.tvMarkerWebsite);
        ivMarkerIcon     = view.findViewById(R.id.ivMarkerIcon);
        btnDirections    = view.findViewById(R.id.btnDirections);

        // Accessibility: Set clear button content description
        ivClearSearch.setContentDescription("Καθαρισμός αναζήτησης");

        iconOpa       = bitmapFromDrawable(requireContext(), R.drawable.marker_opa);
        iconTransport = bitmapFromDrawable(requireContext(), R.drawable.marker_transport);
        iconBook      = bitmapFromDrawable(requireContext(), R.drawable.marker_book);

        View bottomSheet = view.findViewById(R.id.bottomSheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        btnDirections.setOnClickListener(v -> {
            if (currentMarkerPosition == null) return;
            Uri uri = Uri.parse("google.navigation:q=" +
                    currentMarkerPosition.latitude + "," + currentMarkerPosition.longitude);
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, uri);
            mapIntent.setPackage("com.google.android.apps.maps");
            if (mapIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
                startActivity(mapIntent);
            } else {
                announceForAccessibility("Άνοιγμα οδηγιών στον περιηγητή.");
                Uri browserUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=" +
                        currentMarkerPosition.latitude + "," + currentMarkerPosition.longitude);
                startActivity(new Intent(Intent.ACTION_VIEW, browserUri));
            }
        });

        setupSearch();

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private void announceForAccessibility(String message) {
        if (getView() != null) {
            getView().announceForAccessibility(message);
        }
    }

    private void setupSearch() {
        etMapSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (ivClearSearch != null) {
                    ivClearSearch.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                }
            }
        });

        if (ivClearSearch != null) {
            ivClearSearch.setOnClickListener(v -> {
                etMapSearch.setText("");
                resetMarkers();
                hideKeyboard();
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                announceForAccessibility("Η αναζήτηση καθαρίστηκε. Εμφανίζονται όλα τα σημεία.");
            });
        }

        etMapSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                performSearch(etMapSearch.getText().toString().trim());
                hideKeyboard();
                return true;
            }
            return false;
        });
    }

    private void performSearch(String query) {
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        if (query.isEmpty()) {
            resetMarkers();
            return;
        }

        String q = normalizeGreek(query);
        String categoryTag = null;

        if (q.contains("βιβλ") || q.contains("εκδοσ")) {
            categoryTag = TAG_BOOK;
        } else if (q.contains("μετρο") || q.contains("λεωφ") || q.contains("τρολ") ||
                q.contains("σταση") || q.contains("συγκοιν")) {
            categoryTag = TAG_TRANSPORT;
        } else if (q.contains("κτηρ") || q.contains("οπα") || q.contains("πανεπιστημ")) {
            categoryTag = TAG_OPA;
        }

        if (categoryTag != null) {
            filterByCategory(categoryTag);
        } else {
            searchSpecificMarker(q);
        }
    }

    private void filterByCategory(String tag) {
        List<Marker> visible = new ArrayList<>();
        for (Marker marker : allMarkers) {
            MarkerInfo info = (MarkerInfo) marker.getTag();
            boolean show = info != null && info.tag.equals(tag);
            marker.setVisible(show);
            if (show) visible.add(marker);
        }

        if (!visible.isEmpty()) {
            announceForAccessibility("Βρέθηκαν " + visible.size() + " αποτελέσματα στην κατηγορία.");
            if (visible.size() == 1) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(visible.get(0).getPosition(), 16f));
            } else {
                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                for (Marker m : visible) builder.include(m.getPosition());
                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 150));
            }
        } else {
            announceForAccessibility("Δεν βρέθηκαν αποτελέσματα για αυτή την κατηγορία.");
        }
    }

    private void searchSpecificMarker(String q) {
        Marker bestMatch = null;
        int bestScore = 0;

        for (Marker marker : allMarkers) {
            String title = normalizeGreek(marker.getTitle());
            String snippet = normalizeGreek(marker.getSnippet());
            int score = 0;

            if (title.equals(q)) score = 100;
            else if (title.contains(q)) score = 80;
            else if (snippet.contains(q)) score = 50;

            if (score > bestScore) {
                bestScore = score;
                bestMatch = marker;
            }
        }

        if (bestMatch != null && bestScore > 0) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(bestMatch.getPosition(), 17f));
            MarkerInfo info = (MarkerInfo) bestMatch.getTag();
            if (info != null) {
                showBottomSheet(bestMatch.getTitle(), bestMatch.getSnippet(),
                        info.tag, info.website, bestMatch.getPosition());
            }
        } else {
            announceForAccessibility("Δεν βρέθηκαν αποτελέσματα για " + etMapSearch.getText().toString());
            Toast.makeText(getContext(), "Δεν βρέθηκαν αποτελέσματα.", Toast.LENGTH_SHORT).show();
        }
    }

    private void resetMarkers() {
        for (Marker marker : allMarkers) marker.setVisible(true);
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(37.9947, 23.7318), 15f));
    }

    private String normalizeGreek(String text) {
        if (text == null) return "";
        return text.toLowerCase()
                .replace("ά", "α").replace("έ", "ε").replace("ή", "η")
                .replace("ί", "ι").replace("ό", "ο").replace("ύ", "υ")
                .replace("ώ", "ω").replace("ϊ", "ι").replace("ϋ", "υ")
                .replace("ΐ", "ι").replace("ΰ", "υ");
    }

    private void hideKeyboard() {
        if (getActivity() != null) {
            InputMethodManager imm = (InputMethodManager) getActivity()
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null && etMapSearch != null) {
                imm.hideSoftInputFromWindow(etMapSearch.getWindowToken(), 0);
            }
        }
    }

    private BitmapDescriptor bitmapFromDrawable(Context context, int drawableId) {
        var drawable = ContextCompat.getDrawable(context, drawableId);
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

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(37.9947, 23.7318), 15f));

        mMap.setOnMarkerClickListener(marker -> {
            MarkerInfo info = (MarkerInfo) marker.getTag();
            if (info != null) {
                showBottomSheet(marker.getTitle(), marker.getSnippet(),
                        info.tag, info.website, marker.getPosition());
            }
            return true;
        });

        mMap.setOnMapClickListener(latLng ->
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN));
    }

    private void showBottomSheet(String title, String snippet, String tag,
                                 String website, LatLng position) {
        currentMarkerPosition = position;
        tvMarkerTitle.setText(title);
        tvMarkerSnippet.setText(snippet != null ? snippet.replace(" · ", "\n") : "");

        String typeDescription = "Σημείο ενδιαφέροντος";
        if (TAG_TRANSPORT.equals(tag)) {
            ivMarkerIcon.setImageResource(R.drawable.marker_transport);
            typeDescription = "Στάση συγκοινωνίας";
        } else if (TAG_BOOK.equals(tag)) {
            ivMarkerIcon.setImageResource(R.drawable.marker_book);
            typeDescription = "Βιβλιοπωλείο";
        } else {
            ivMarkerIcon.setImageResource(R.drawable.marker_opa);
            typeDescription = "Κτήριο ΟΠΑ";
        }

        ivMarkerIcon.setContentDescription(typeDescription);

        if (website != null) {
            tvMarkerWebsite.setVisibility(View.VISIBLE);
            tvMarkerWebsite.setText("🌐 " + website.replace("https://www.", ""));
            tvMarkerWebsite.setContentDescription("Ιστοσελίδα: " + website);
            tvMarkerWebsite.setOnClickListener(v ->
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(website))));
        } else {
            tvMarkerWebsite.setVisibility(View.GONE);
        }

        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        announceForAccessibility("Εμφάνιση λεπτομερειών για: " + title + ". " + typeDescription);
    }

    private void addOPABuildings() {
        addMarker(37.99468, 23.73185, "Μαράσλειο Μέγαρο", "Κεντρικό κτήριο ΟΠΑ · Πατησίων 76", null, iconOpa, TAG_OPA);
        addMarker(37.99590, 23.73612, "Κτήριο Τροίας", "Νέο κτήριο ΟΠΑ · Τροίας 2", null, iconOpa, TAG_OPA);
        addMarker(37.99567, 23.73310, "Κτήριο Κοδριγκτώνος", "Γραφεία καθηγητών · Κοδριγκτώνος 12", null, iconOpa, TAG_OPA);
    }

    private void addTransport() {
        addMarker(37.99339, 23.73183, "Στάση: ΟΙΚΟΝΟΜΙΚΟ ΠΑΝΕΠΙΣΤΗΜΙΟ", "Λεωφορεία και Τρόλεϊ", null, iconTransport, TAG_TRANSPORT);
        addMarker(37.99302, 23.73049, "Μετρό: Βικτώρια", "Γραμμή 1 Πράσινη", null, iconTransport, TAG_TRANSPORT);
    }

    private void addBookstores() {
        addMarker(37.98936, 23.72993, "Κλειδάριθμος", "Βιβλιοπωλείο · Μάρνη 8", "https://www.klidarithmos.gr", iconBook, TAG_BOOK);
        addMarker(37.99354, 23.73232, "Βιβλιοδιανομή ΟΠΑ", "Διανομή Βιβλίων · Αντωνιάδου 2", null, iconBook, TAG_BOOK);
    }

    private void addMarker(double lat, double lng, String title, String snippet,
                           String website, BitmapDescriptor icon, String tag) {
        Marker marker = mMap.addMarker(new MarkerOptions()
                .position(new LatLng(lat, lng))
                .title(title)
                .snippet(snippet)
                .icon(icon));
        if (marker != null) {
            marker.setTag(new MarkerInfo(tag, website));
            allMarkers.add(marker);
        }
    }
}