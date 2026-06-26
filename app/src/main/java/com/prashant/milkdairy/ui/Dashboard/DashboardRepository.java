package com.prashant.milkdairy.ui.Dashboard;

import android.util.Log;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.prashant.milkdairy.Model.DashboardModel;
import com.prashant.milkdairy.Utils.FirebaseRefs;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class DashboardRepository {

    public interface DashboardCallback {
        void onDashboardUpdated(DashboardModel model);
        void onDashboardError(String source, Exception error);
    }

    private static final String TAG = "DashboardRepository";

    private final List<ListenerRegistration> listeners = new ArrayList<>();

    private final List<DocumentSnapshot> milkDocs = new ArrayList<>();
    private final List<DocumentSnapshot> inventoryDocs = new ArrayList<>();
    private final List<DocumentSnapshot> billDocs = new ArrayList<>();
    private final List<DocumentSnapshot> farmerDocs = new ArrayList<>();

    private DashboardCallback callback;

    public void startListening(DashboardCallback callback) {
        stopListening();
        this.callback = callback;

        listenTodayMilk();
        listenInventory();
        listenCurrentMonthBills();
        listenFarmers();
        listenDeductions();

        publishDashboard();
    }

    private void listenTodayMilk() {
        String today = new SimpleDateFormat(
                "dd MMM yyyy",
                Locale.US
        ).format(Calendar.getInstance().getTime());

        listeners.add(FirebaseRefs.milkCollection()
                .whereEqualTo("date", today)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        handleError("MilkCollection", error);
                        return;
                    }

                    milkDocs.clear();

                    if (snapshots != null) {
                        milkDocs.addAll(snapshots.getDocuments());
                    }

                    publishDashboard();
                }));
    }

    private void listenInventory() {
        listeners.add(FirebaseRefs.inventoryItems()
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        handleError("Inventory", error);
                        return;
                    }

                    inventoryDocs.clear();

                    if (snapshots != null) {
                        inventoryDocs.addAll(snapshots.getDocuments());
                    }

                    publishDashboard();
                }));
    }

    private void listenCurrentMonthBills() {
        Calendar start = Calendar.getInstance();
        start.set(Calendar.DAY_OF_MONTH, 1);
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);

        Calendar nextMonth = (Calendar) start.clone();
        nextMonth.add(Calendar.MONTH, 1);

        listeners.add(FirebaseRefs.bills()
                .whereGreaterThanOrEqualTo("generatedAt", new Timestamp(start.getTime()))
                .whereLessThan("generatedAt", new Timestamp(nextMonth.getTime()))
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        handleError("Bills", error);
                        return;
                    }

                    billDocs.clear();

                    if (snapshots != null) {
                        billDocs.addAll(snapshots.getDocuments());
                    }

                    publishDashboard();
                }));
    }

    private void listenFarmers() {
        listeners.add(FirebaseRefs.farmers()
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        handleError("Farmers", error);
                        return;
                    }

                    farmerDocs.clear();

                    if (snapshots != null) {
                        farmerDocs.addAll(snapshots.getDocuments());
                    }

                    publishDashboard();
                }));
    }

    private void listenDeductions() {
        listeners.add(FirebaseRefs.advanceFood()
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        handleError("Deductions", error);
                        return;
                    }

                    // Deduction changes do not have a dashboard card yet,
                    // but they still trigger an automatic refresh.
                    publishDashboard();
                }));
    }

    private void publishDashboard() {
        if (callback == null) return;

        DashboardModel model = new DashboardModel();

        for (DocumentSnapshot doc : milkDocs) {
            double liters = firstDouble(doc,
                    "quantityLiters", "liters", "quantity", "milkLiter");

            double rate = firstDouble(doc,
                    "ratePerLiter", "rate", "milkRate");

            double amount = liters * rate;

            if (amount <= 0) {
                amount = firstDouble(doc,
                        "amount", "totalAmount", "total", "grossAmount");
            }

            String shift = firstString(doc, "shift");
            String milkType = firstString(doc, "milkType", "type");

            model.addMilkRecord(shift, milkType, liters, amount);
        }

        for (DocumentSnapshot doc : inventoryDocs) {
            Boolean isActive = doc.getBoolean("isActive");

            if (isActive != null && !isActive) {
                continue;
            }

            double stock = firstDouble(doc, "currentStock", "stock");
            double purchaseRate = firstDouble(doc, "purchaseRate", "rate");

            model.addInventoryValue(stock * purchaseRate);
        }

        for (DocumentSnapshot doc : billDocs) {
            String status = firstString(doc, "status", "billStatus");

            if ("Cancelled".equalsIgnoreCase(status)) {
                continue;
            }

            double amount = firstDouble(doc,
                    "netPayable", "payableAmount", "totalPayable", "amount");

            model.addBill(amount);
        }

        for (DocumentSnapshot doc : farmerDocs) {
            String status = firstString(doc, "status");

            if (status.isEmpty()
                    || "Active".equalsIgnoreCase(status)
                    || "ACTIVE".equalsIgnoreCase(status)) {

                model.incrementActiveFarmers();
            }
        }

        callback.onDashboardUpdated(model);
    }

    private String firstString(DocumentSnapshot doc, String... keys) {
        for (String key : keys) {
            Object value = doc.get(key);

            if (value != null && !value.toString().trim().isEmpty()) {
                return value.toString().trim();
            }
        }

        return "";
    }

    private double firstDouble(DocumentSnapshot doc, String... keys) {
        for (String key : keys) {
            Object value = doc.get(key);

            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }

            if (value != null) {
                try {
                    return Double.parseDouble(
                            value.toString()
                                    .replace("\u20B9", "")
                                    .replace(",", "")
                                    .trim()
                    );
                } catch (Exception ignored) {}
            }
        }

        return 0;
    }

    private void handleError(String source, Exception error) {
        Log.e(TAG, source + " listener failed", error);

        if (callback != null) {
            callback.onDashboardError(source, error);
            publishDashboard();
        }
    }

    public void stopListening() {
        for (ListenerRegistration registration : listeners) {
            registration.remove();
        }

        listeners.clear();
        callback = null;
    }
}