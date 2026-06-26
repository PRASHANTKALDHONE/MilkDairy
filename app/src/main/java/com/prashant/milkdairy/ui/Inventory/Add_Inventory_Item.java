package com.prashant.milkdairy.ui.Inventory;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.prashant.milkdairy.R;
import com.prashant.milkdairy.Utils.FirebaseRefs;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Add_Inventory_Item extends Fragment {

    private EditText etItemName, etCategory, etUnit, etMinimumStock;
    private EditText etOpeningStock, etPurchaseRate, etSellingRate;
    private TextView txtItemCode;

    private boolean saving = false;

    public Add_Inventory_Item() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_add__inventory__item, container, false);

        initViews(view);
        loadNextItemCode();

        view.findViewById(R.id.btnBack).setOnClickListener(v ->
                Navigation.findNavController(v).popBackStack()
        );

        view.findViewById(R.id.btnCancel).setOnClickListener(v ->
                Navigation.findNavController(v).popBackStack()
        );

        view.findViewById(R.id.btnReset).setOnClickListener(v -> clearAllFields());

        view.findViewById(R.id.btnSave).setOnClickListener(v -> validateAndSave(view));

        return view;
    }

    private void initViews(View view) {
        txtItemCode = view.findViewById(R.id.txtItemCode);

        etItemName = view.findViewById(R.id.etItemName);
        etCategory = view.findViewById(R.id.etCategory);
        etUnit = view.findViewById(R.id.etUnit);
        etMinimumStock = view.findViewById(R.id.etMinimumStock);
        etOpeningStock = view.findViewById(R.id.etOpeningStock);
        etPurchaseRate = view.findViewById(R.id.etPurchaseRate);
        etSellingRate = view.findViewById(R.id.etSellingRate);
    }

    private void loadNextItemCode() {
        FirebaseRefs.currentUserDoc().get()
                .addOnSuccessListener(doc -> {
                    Long counter = doc.getLong("inventoryCounter");
                    int next = counter == null ? 1 : counter.intValue() + 1;
                    txtItemCode.setText(formatItemCode(next));
                })
                .addOnFailureListener(e -> txtItemCode.setText("I001"));
    }

    private String formatItemCode(int number) {
        return "I" + String.format(Locale.US, "%03d", number);
    }

    private void clearAllFields() {
        etItemName.setText("");
        etCategory.setText("");
        etUnit.setText("");
        etMinimumStock.setText("");
        etOpeningStock.setText("");
        etPurchaseRate.setText("");
        etSellingRate.setText("");

        Toast.makeText(getContext(), "All fields reset", Toast.LENGTH_SHORT).show();
    }

    private void validateAndSave(View view) {
        if (saving) return;

        String itemName = etItemName.getText().toString().trim();
        String category = etCategory.getText().toString().trim();
        String unit = etUnit.getText().toString().trim();

        double minimumStock = parseDouble(etMinimumStock.getText().toString());
        double openingStock = parseDouble(etOpeningStock.getText().toString());
        double purchaseRate = parseDouble(etPurchaseRate.getText().toString());
        double sellingRate = parseDouble(etSellingRate.getText().toString());

        if (TextUtils.isEmpty(itemName)) {
            etItemName.setError("Required");
            return;
        }

        if (TextUtils.isEmpty(category)) {
            etCategory.setError("Required");
            return;
        }

        if (TextUtils.isEmpty(unit)) {
            etUnit.setError("Required");
            return;
        }

        if (minimumStock < 0 || openingStock < 0 || purchaseRate < 0 || sellingRate < 0) {
            Toast.makeText(getContext(), "Values cannot be negative", Toast.LENGTH_SHORT).show();
            return;
        }

        saving = true;

        FirebaseRefs.inventoryItems().get()
                .addOnSuccessListener(snapshots -> {
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : snapshots) {
                        Boolean active = doc.getBoolean("isActive");
                        boolean isActive = active == null || active;

                        String oldName = doc.getString("itemName");
                        String oldCategory = doc.getString("category");

                        if (isActive
                                && itemName.equalsIgnoreCase(oldName)
                                && category.equalsIgnoreCase(oldCategory)) {
                            saving = false;
                            Toast.makeText(getContext(), "Same item already exists in this category", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }

                    saveInventoryItem(view, itemName, category, unit, minimumStock, openingStock, purchaseRate, sellingRate);
                })
                .addOnFailureListener(e -> {
                    saving = false;
                    Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void saveInventoryItem(View view,
                                   String itemName,
                                   String category,
                                   String unit,
                                   double minimumStock,
                                   double openingStock,
                                   double purchaseRate,
                                   double sellingRate) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        DocumentReference userRef = FirebaseRefs.currentUserDoc();
        DocumentReference txnRef = FirebaseRefs.inventoryTransactions().document();

        db.runTransaction(transaction -> {
            com.google.firebase.firestore.DocumentSnapshot userSnap = transaction.get(userRef);

            Long counterValue = userSnap.getLong("inventoryCounter");
            int nextCounter = counterValue == null ? 1 : counterValue.intValue() + 1;

            String itemCode = formatItemCode(nextCounter);
            DocumentReference itemRef = FirebaseRefs.inventoryItems().document(itemCode);

            double stockValue = openingStock * purchaseRate;

            Map<String, Object> item = new HashMap<>();
            item.put("itemId", itemCode);
            item.put("itemCode", itemCode);
            item.put("itemName", itemName);
            item.put("category", category);
            item.put("unit", unit);
            item.put("currentStock", openingStock);
            item.put("minimumStock", minimumStock);
            item.put("purchaseRate", purchaseRate);
            item.put("sellingRate", sellingRate);
            item.put("stockValue", stockValue);
            item.put("isActive", true);
            item.put("createdAt", FieldValue.serverTimestamp());
            item.put("updatedAt", FieldValue.serverTimestamp());

            Map<String, Object> txn = new HashMap<>();
            txn.put("transactionId", txnRef.getId());
            txn.put("itemId", itemCode);
            txn.put("itemCode", itemCode);
            txn.put("itemName", itemName);
            txn.put("category", category);
            txn.put("unit", unit);
            txn.put("type", "OPENING_STOCK");
            txn.put("quantity", openingStock);
            txn.put("rate", purchaseRate);
            txn.put("totalAmount", stockValue);
            txn.put("previousStock", 0);
            txn.put("newStock", openingStock);
            txn.put("remarks", "Opening stock");
            txn.put("createdBy", FirebaseRefs.currentUserId());
            txn.put("createdAt", FieldValue.serverTimestamp());

            transaction.set(itemRef, item);
            transaction.set(txnRef, txn);
            transaction.update(userRef, "inventoryCounter", nextCounter);

            return null;
        }).addOnSuccessListener(unused -> {
            saving = false;
            Toast.makeText(getContext(), "Inventory saved successfully", Toast.LENGTH_SHORT).show();
            Navigation.findNavController(view).popBackStack();
        }).addOnFailureListener(e -> {
            saving = false;
            Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }

    private double parseDouble(String value) {
        try {
            if (value == null || value.trim().isEmpty()) return 0;
            return Double.parseDouble(value.trim());
        } catch (Exception e) {
            return 0;
        }
    }
}
