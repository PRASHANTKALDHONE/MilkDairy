package com.prashant.milkdairy.ui.Inventory;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.prashant.milkdairy.Adapter.InventoryAdapter;
import com.prashant.milkdairy.Model.Farmer;
import com.prashant.milkdairy.Model.InventoryModel;
import com.prashant.milkdairy.R;
import com.prashant.milkdairy.Utils.FirebaseRefs;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class InventoryFragment extends Fragment {

    private FloatingActionButton fabAddInventory;
    private RecyclerView recyclerInventory;
    private Spinner spCategory;
    private EditText etSearchInventory;

    private LinearLayout layoutEmptyInventory;
    private TextView tvEmptyInventory;

    private TextView tvStatTotal, tvStatLowStock, tvStatOutStock, tvStatStockValue;

    private final List<InventoryModel> inventoryList = new ArrayList<>();
    private final List<InventoryModel> filteredList = new ArrayList<>();

    private InventoryAdapter adapter;
    private ListenerRegistration inventoryListener;
    private final List<Farmer> farmers = new ArrayList<>();
    private ListenerRegistration farmerListener;


    private final String[] categories = {
            "All Categories",
            "Feed",
            "Medicine",
            "Chemical",
            "Equipment",
            "Testing",
            "Packaging",
            "Cleaning",
            "Consumable",
            "Other"
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_inventory, container, false);

        initViews(view);
        setupCategorySpinner();
        setupRecycler();
        setupSearch();
        listenInventory();
        listenFarmers();


        fabAddInventory.setOnClickListener(v ->
                Navigation.findNavController(v)
                        .navigate(R.id.action_nav_inventory_to_add_inventory_item)
        );

        return view;
    }

    private void initViews(View view) {
        recyclerInventory = view.findViewById(R.id.rvDeductionHistory);
        spCategory = view.findViewById(R.id.spCategory);
        etSearchInventory = view.findViewById(R.id.etSearchInventory);
        fabAddInventory = view.findViewById(R.id.fabAddInventory);

        layoutEmptyInventory = view.findViewById(R.id.layoutEmptyInventory);
        tvEmptyInventory = view.findViewById(R.id.tvEmptyInventory);

        tvStatTotal = view.findViewById(R.id.statTotal).findViewById(R.id.tvDashnum);
        tvStatLowStock = view.findViewById(R.id.statlowstock).findViewById(R.id.tvDashnum);
        tvStatOutStock = view.findViewById(R.id.statoutstock).findViewById(R.id.tvDashnum);
        tvStatStockValue = view.findViewById(R.id.statstockvalue).findViewById(R.id.tvDashnum);

        TextView labelTotal = view.findViewById(R.id.statTotal).findViewById(R.id.tvdashtext);
        TextView labelLow = view.findViewById(R.id.statlowstock).findViewById(R.id.tvdashtext);
        TextView labelOut = view.findViewById(R.id.statoutstock).findViewById(R.id.tvdashtext);
        TextView labelValue = view.findViewById(R.id.statstockvalue).findViewById(R.id.tvdashtext);


        labelTotal.setText("TOTAL");
        labelLow.setText("LOW STOCK");
        labelOut.setText("OUT STOCK");
        labelValue.setText("VALUE");

    }

    private void setupRecycler() {
        recyclerInventory.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new InventoryAdapter(filteredList, new InventoryAdapter.InventoryActionListener() {
            @Override
            public void onStockIn(InventoryModel item) {
                showStockDialog(item, "STOCK_IN");
            }

            @Override
            public void onStockOut(InventoryModel item) {
                showStockDialog(item, "STOCK_OUT");
            }

            @Override
            public void onSell(InventoryModel item) {
                showStockDialog(item, "FARMER_FEED");
            }

            @Override
            public void onEdit(InventoryModel item) {
                showEditDialog(item);
            }

            @Override
            public void onDelete(InventoryModel item) {
                confirmDelete(item);
            }
        });

        recyclerInventory.setAdapter(adapter);
    }

    private void setupCategorySpinner() {

        ArrayAdapter<String> spinnerAdapter =
                new ArrayAdapter<>(
                        requireContext(),
                        R.layout.spinner_inventory_selected,
                        categories
                );

        spinnerAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spCategory.setAdapter(spinnerAdapter);

        spCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent,
                                       View view,
                                       int position,
                                       long id) {

                filterData();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void setupSearch() {
        etSearchInventory.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterData();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void listenInventory() {
        inventoryListener = FirebaseRefs.inventoryItems()
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Toast.makeText(requireContext(), error.getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }

                    inventoryList.clear();

                    if (snapshots != null) {
                        for (QueryDocumentSnapshot doc : snapshots) {
                            InventoryModel item = InventoryModel.fromDoc(doc);

                            if (item.isActive()) {
                                inventoryList.add(item);
                            }
                        }
                    }

                    updateDashboard();
                    filterData();
                });
    }

    private void updateDashboard() {
        int total = inventoryList.size();
        int low = 0;
        int out = 0;
        double value = 0;

        for (InventoryModel item : inventoryList) {
            double stock = item.getCurrentStockValue();
            double min = item.getMinimumStockValue();

            if (stock <= 0) {
                out++;
            } else if (stock <= min) {
                low++;
            }

            value += item.getStockValueAmount();
        }

        tvStatTotal.setText(String.valueOf(total));
        tvStatLowStock.setText(String.valueOf(low));
        tvStatOutStock.setText(String.valueOf(out));
        tvStatStockValue.setText("₹" + String.format("%.0f", value));
    }

    private void filterData() {
        filteredList.clear();

        String selectedCategory = spCategory.getSelectedItem() == null
                ? "All Categories"
                : spCategory.getSelectedItem().toString();

        String searchText = etSearchInventory.getText().toString().trim().toLowerCase(Locale.US);

        for (InventoryModel item : inventoryList) {
            boolean matchesCategory = selectedCategory.equals("All Categories")
                    || item.getCategory().equalsIgnoreCase(selectedCategory);

            boolean matchesSearch =
                    item.getItemName().toLowerCase(Locale.US).contains(searchText)
                            || item.getItemCode().toLowerCase(Locale.US).contains(searchText)
                            || item.getCategory().toLowerCase(Locale.US).contains(searchText);

            if (matchesCategory && matchesSearch) {
                filteredList.add(item);
            }
        }

        adapter.notifyDataSetChanged();
        updateEmptyState();
    }
    private void updateEmptyState() {
        boolean isEmpty = filteredList.isEmpty();

        recyclerInventory.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        layoutEmptyInventory.setVisibility(isEmpty ? View.VISIBLE : View.GONE);

        if (isEmpty) {
            String searchText = etSearchInventory.getText().toString().trim();
            String category = spCategory.getSelectedItem() == null
                    ? "All Categories"
                    : spCategory.getSelectedItem().toString();

            if (!searchText.isEmpty() || !"All Categories".equals(category)) {
                tvEmptyInventory.setText("No inventory item found for selected filter");
            } else {
                tvEmptyInventory.setText("No inventory item found");
            }
        }
    }

    private void showStockDialog(InventoryModel item, String type) {
        boolean stockIn = "STOCK_IN".equals(type);

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());

        builder.setTitle(stockIn
                ? "Stock In - " + item.getItemName()
                : "Stock Out - " + item.getItemName());

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 10);

        final Farmer[] selectedFarmer = {null};

        AutoCompleteTextView etFarmer = new AutoCompleteTextView(requireContext());
        etFarmer.setHint("Select Farmer *");
        etFarmer.setThreshold(1);
        etFarmer.setSingleLine(true);

        List<String> farmerNames = new ArrayList<>();
        for (Farmer farmer : farmers) {
            farmerNames.add(farmer.getCode() + " - " + farmer.getName());
        }

        ArrayAdapter<String> farmerAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                farmerNames
        );

        etFarmer.setAdapter(farmerAdapter);

        etFarmer.setOnItemClickListener((parent, view, position, id) -> {
            String selectedText = parent.getItemAtPosition(position).toString();
            selectedFarmer[0] = findFarmerByDisplayText(selectedText);
        });

        TextView tvAvailableStock = new TextView(requireContext());
        tvAvailableStock.setText("Available Stock: " +
                String.format(Locale.US, "%.2f", item.getCurrentStockValue()) +
                " " + item.getUnit());

        EditText etQuantity = createNumberInput("Quantity *");

        EditText etRate = createNumberInput(stockIn
                ? "Purchase Rate Per Unit *"
                : "Selling Rate Per Unit *");

        etRate.setText(String.format(Locale.US, "%.2f",
                stockIn ? item.getPurchaseRateValue() : item.getSellingRateValue()));

        TextView tvTotalAmount = new TextView(requireContext());
        tvTotalAmount.setText("Total Amount: ₹0.00");
        tvTotalAmount.setTextSize(16);

        Spinner spType = new Spinner(requireContext());

        String[] outTypes = {
                "FARMER_FEED",
                "MEDICINE_USAGE",
                "CHEMICAL_USAGE",
                "DAMAGE",
                "MANUAL_ADJUSTMENT"
        };

        spType.setAdapter(new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                outTypes
        ));

        EditText etRemarks = new EditText(requireContext());
        etRemarks.setHint("Remarks");
        etRemarks.setMinLines(2);

        if (!stockIn) {
            layout.addView(etFarmer);
            layout.addView(tvAvailableStock);
        }

        layout.addView(etQuantity);
        layout.addView(etRate);

        if (!stockIn) {
            layout.addView(spType);
            spType.setSelection(0);
        }

        layout.addView(tvTotalAmount);
        layout.addView(etRemarks);

        TextWatcher amountWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                double qty = parseDouble(etQuantity.getText().toString());
                double rate = parseDouble(etRate.getText().toString());
                tvTotalAmount.setText("Total Amount: ₹" + String.format(Locale.US, "%.2f", qty * rate));
            }

            @Override public void afterTextChanged(Editable s) {}
        };

        etQuantity.addTextChangedListener(amountWatcher);
        etRate.addTextChangedListener(amountWatcher);

        builder.setView(layout);
        builder.setPositiveButton("Save", null);
        builder.setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(d -> {
            Button btnSave = dialog.getButton(AlertDialog.BUTTON_POSITIVE);

            btnSave.setOnClickListener(v -> {
                double qty = parseDouble(etQuantity.getText().toString());
                double rate = parseDouble(etRate.getText().toString());

                if (!stockIn) {
                    if (selectedFarmer[0] == null) {
                        selectedFarmer[0] = findFarmerByDisplayText(etFarmer.getText().toString().trim());
                    }

                    if (selectedFarmer[0] == null) {
                        etFarmer.setError("Select farmer");
                        return;
                    }
                }

                if (qty <= 0) {
                    etQuantity.setError("Enter valid quantity");
                    return;
                }

                if (!stockIn && qty > item.getCurrentStockValue()) {
                    etQuantity.setError("Insufficient stock");
                    return;
                }

                if (rate < 0) {
                    etRate.setError("Invalid rate");
                    return;
                }

                String finalType = stockIn
                        ? "STOCK_IN"
                        : spType.getSelectedItem().toString();

                saveStockMovement(
                        item,
                        finalType,
                        qty,
                        rate,
                        etRemarks.getText().toString().trim(),
                        selectedFarmer[0],
                        dialog
                );
            });
        });

        dialog.show();
    }
    private Farmer findFarmerByDisplayText(String text) {
        for (Farmer farmer : farmers) {
            String display = farmer.getCode() + " - " + farmer.getName();

            if (display.equalsIgnoreCase(text)
                    || farmer.getCode().equalsIgnoreCase(text)
                    || farmer.getName().equalsIgnoreCase(text)) {
                return farmer;
            }
        }

        return null;
    }



    private void listenFarmers() {
        farmerListener = FirebaseRefs.farmers()
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Toast.makeText(requireContext(), error.getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }

                    farmers.clear();

                    if (snapshots != null) {
                        for (QueryDocumentSnapshot doc : snapshots) {
                            Farmer farmer = new Farmer();
                            farmer.setId(doc.getId());
                            farmer.setCode(doc.getString("code"));
                            farmer.setName(doc.getString("name"));
                            farmer.setMobile(doc.getString("mobile"));
                            farmer.setStatus(doc.getString("status"));
                            farmer.setMilkType(doc.getString("milkType"));

                            if ("Active".equalsIgnoreCase(farmer.getStatus())) {
                                farmers.add(farmer);
                            }
                        }
                    }
                });
    }


    private EditText createNumberInput(String hint) {
        EditText editText = new EditText(requireContext());
        editText.setHint(hint);
        editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        return editText;
    }

    private void saveStockMovement(InventoryModel item,
                                   String type,
                                   double quantity,
                                   double rate,
                                   String remarks,
                                   Farmer farmer,
                                   Dialog dialog) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        DocumentReference itemRef = FirebaseRefs.inventoryItems().document(item.getItemId());
        DocumentReference txnRef = FirebaseRefs.inventoryTransactions().document();

        boolean stockIn = "STOCK_IN".equals(type);

        DocumentReference deductionRef = stockIn
                ? null
                : FirebaseRefs.advanceFood().document();

        db.runTransaction(transaction -> {
            com.google.firebase.firestore.DocumentSnapshot itemSnap = transaction.get(itemRef);

            double previousStock = getDouble(itemSnap, "currentStock");
            double oldPurchaseRate = getDouble(itemSnap, "purchaseRate");
            double oldSellingRate = getDouble(itemSnap, "sellingRate");

            double newStock;
            double finalPurchaseRate;
            double finalSellingRate;
            double finalStockValue;

            if (stockIn) {
                double oldStockValue = previousStock * oldPurchaseRate;
                double newPurchaseValue = quantity * rate;

                newStock = previousStock + quantity;

                if (newStock <= 0) {
                    finalPurchaseRate = 0;
                    finalStockValue = 0;
                } else {
                    finalStockValue = oldStockValue + newPurchaseValue;
                    finalPurchaseRate = finalStockValue / newStock;
                }

                finalSellingRate = oldSellingRate;

            } else {
                if (farmer == null) {
                    throw new FirebaseFirestoreException(
                            "Farmer is required",
                            FirebaseFirestoreException.Code.ABORTED
                    );
                }

                if (quantity > previousStock) {
                    throw new FirebaseFirestoreException(
                            "Insufficient stock",
                            FirebaseFirestoreException.Code.ABORTED
                    );
                }

                newStock = previousStock - quantity;
                finalPurchaseRate = oldPurchaseRate;
                finalSellingRate = rate;
                finalStockValue = newStock * finalPurchaseRate;
            }

            finalPurchaseRate = roundTwo(finalPurchaseRate);
            finalSellingRate = roundTwo(finalSellingRate);
            finalStockValue = roundTwo(finalStockValue);

            double totalAmount = roundTwo(quantity * rate);

            Map<String, Object> updates = new HashMap<>();
            updates.put("currentStock", roundTwo(newStock));
            updates.put("purchaseRate", finalPurchaseRate);
            updates.put("sellingRate", finalSellingRate);
            updates.put("stockValue", finalStockValue);
            updates.put("updatedAt", FieldValue.serverTimestamp());

            transaction.update(itemRef, updates);

            Map<String, Object> txn = new HashMap<>();
            txn.put("transactionId", txnRef.getId());
            txn.put("itemId", item.getItemId());
            txn.put("itemCode", item.getItemCode());
            txn.put("itemName", item.getItemName());
            txn.put("category", item.getCategory());
            txn.put("unit", item.getUnit());

            txn.put("type", stockIn ? "STOCK_IN" : "STOCK_OUT");
            txn.put("transactionType", stockIn ? "STOCK_IN" : "STOCK_OUT");
            txn.put("stockOutType", stockIn ? "" : type);

            txn.put("quantity", quantity);
            txn.put("rate", rate);
            txn.put("sellingRate", stockIn ? 0 : rate);
            txn.put("totalAmount", totalAmount);

            txn.put("previousStock", previousStock);
            txn.put("newStock", roundTwo(newStock));
            txn.put("purchaseRateBefore", oldPurchaseRate);
            txn.put("purchaseRateAfter", finalPurchaseRate);
            txn.put("stockValueAfter", finalStockValue);

            if (!stockIn) {
                txn.put("farmerId", farmer.getId());
                txn.put("farmerCode", farmer.getCode());
                txn.put("farmerName", farmer.getName());
            }

            txn.put("remarks", remarks);
            txn.put("createdBy", FirebaseRefs.currentUserId());
            txn.put("createdAt", FieldValue.serverTimestamp());

            transaction.set(txnRef, txn);

            if (!stockIn && deductionRef != null) {
                String today = new java.text.SimpleDateFormat("dd-MM-yyyy", Locale.US)
                        .format(new java.util.Date());

                long todayMillis = System.currentTimeMillis();

                Map<String, Object> deduction = new HashMap<>();
                deduction.put("id", deductionRef.getId());
                deduction.put("deductionId", deductionRef.getId());

                deduction.put("farmerId", farmer.getId());
                deduction.put("farmerCode", farmer.getCode());
                deduction.put("farmerName", farmer.getName());

                deduction.put("title", item.getItemName());
                deduction.put("category", "Inventory");
                deduction.put("type", "Inventory");
                deduction.put("description", item.getItemName() + " - " + quantity + " " + item.getUnit());

                deduction.put("amount", totalAmount);
                deduction.put("remaining", totalAmount);
                deduction.put("quantity", quantity);
                deduction.put("unit", item.getUnit());
                deduction.put("rate", rate);

                deduction.put("source", "INVENTORY");
                deduction.put("inventoryItemId", item.getItemId());
                deduction.put("inventoryItemCode", item.getItemCode());
                deduction.put("inventoryTransactionId", txnRef.getId());

                deduction.put("date", today);
                deduction.put("dateMillis", todayMillis);
                deduction.put("status", "UNPAID");

                deduction.put("createdAt", FieldValue.serverTimestamp());
                deduction.put("updatedAt", FieldValue.serverTimestamp());

                transaction.set(deductionRef, deduction);
            }

            return null;
        }).addOnSuccessListener(unused -> {
            dialog.dismiss();

            if (stockIn) {
                Toast.makeText(requireContext(), "Stock added successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Stock issued and deduction created", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e ->
                Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private double roundTwo(double value) {
        return Math.round(value * 100.0) / 100.0;
    }



    private void showEditDialog(InventoryModel item) {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_edit_inventory);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        TextView txtTitle = dialog.findViewById(R.id.txtDialogTitle);
        ImageView btnClose = dialog.findViewById(R.id.btnCloseDialog);

        EditText etName = dialog.findViewById(R.id.etEditItemName);
        EditText etUnit = dialog.findViewById(R.id.etEditUnit);
        EditText etMinStock = dialog.findViewById(R.id.etEditMinStock);
        EditText etPurchase = dialog.findViewById(R.id.etEditPurchaseRate);
        EditText etSelling = dialog.findViewById(R.id.etEditSellingRate);
        Spinner spEditCategory = dialog.findViewById(R.id.spEditCategory);

        txtTitle.setText("Edit Item - " + item.getItemCode());

        etName.setText(item.getItemName());
        etUnit.setText(item.getUnit());
        etMinStock.setText(String.format(Locale.US, "%.2f", item.getMinimumStockValue()));
        etPurchase.setText(String.format(Locale.US, "%.2f", item.getPurchaseRateValue()));
        etSelling.setText(String.format(Locale.US, "%.2f", item.getSellingRateValue()));

        List<String> editCategories = new ArrayList<>();

        for (String c : categories) {
            if (!"All Categories".equals(c)) {
                editCategories.add(c);
            }
        }

        if (!editCategories.contains(item.getCategory())) {
            editCategories.add(item.getCategory());
        }

        spEditCategory.setAdapter(new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                editCategories
        ));

        spEditCategory.setSelection(editCategories.indexOf(item.getCategory()));

        Button btnSave = dialog.findViewById(R.id.btnSaveEdit);
        Button btnCancel = dialog.findViewById(R.id.btnCancelEdit);

        btnClose.setOnClickListener(v -> dialog.dismiss());
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String category = spEditCategory.getSelectedItem().toString();
            String unit = etUnit.getText().toString().trim();

            double minStock = parseDouble(etMinStock.getText().toString());
            double purchase = parseDouble(etPurchase.getText().toString());
            double selling = parseDouble(etSelling.getText().toString());

            if (name.isEmpty()) {
                etName.setError("Required");
                return;
            }

            if (unit.isEmpty()) {
                etUnit.setError("Required");
                return;
            }

            if (minStock < 0 || purchase < 0 || selling < 0) {
                Toast.makeText(requireContext(), "Values cannot be negative", Toast.LENGTH_SHORT).show();
                return;
            }

            if (isDuplicateItem(name, category, item.getItemId())) {
                Toast.makeText(requireContext(), "Same item already exists in this category", Toast.LENGTH_SHORT).show();
                return;
            }

            double stockValue = item.getCurrentStockValue() * purchase;

            Map<String, Object> updates = new HashMap<>();
            updates.put("itemName", name);
            updates.put("category", category);
            updates.put("unit", unit);
            updates.put("minimumStock", minStock);
            updates.put("purchaseRate", purchase);
            updates.put("sellingRate", selling);
            updates.put("stockValue", stockValue);
            updates.put("updatedAt", FieldValue.serverTimestamp());

            FirebaseRefs.inventoryItems().document(item.getItemId())
                    .update(updates)
                    .addOnSuccessListener(unused -> {
                        dialog.dismiss();
                        Toast.makeText(requireContext(), "Item updated", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_LONG).show());
        });

        dialog.show();
    }

    private void confirmDelete(InventoryModel item) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Item")
                .setMessage("This will hide the item but keep transaction history safe.")
                .setPositiveButton("Delete", (dialog, which) -> FirebaseRefs.inventoryItems()
                        .document(item.getItemId())
                        .update(
                                "isActive", false,
                                "updatedAt", FieldValue.serverTimestamp()
                        )
                        .addOnSuccessListener(unused ->
                                Toast.makeText(requireContext(), "Item deleted", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e ->
                                Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_LONG).show()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private boolean isDuplicateItem(String name, String category, String currentItemId) {
        for (InventoryModel item : inventoryList) {
            if (item.getItemId().equals(currentItemId)) continue;

            if (item.getItemName().equalsIgnoreCase(name)
                    && item.getCategory().equalsIgnoreCase(category)) {
                return true;
            }
        }

        return false;
    }

    private double parseDouble(String value) {
        try {
            return Double.parseDouble(value.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private double getDouble(com.google.firebase.firestore.DocumentSnapshot doc, String key) {
        Double value = doc.getDouble(key);
        return value == null ? 0 : value;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (inventoryListener != null) {
            inventoryListener.remove();
        }

        if (farmerListener != null) {
            farmerListener.remove();
        }
    }

}
