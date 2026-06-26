package com.prashant.milkdairy.ui.Bonus_Management;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.prashant.milkdairy.Model.BonusResultModel;
import com.prashant.milkdairy.Model.BonusRuleModel;
import com.prashant.milkdairy.Model.Farmer;
import com.prashant.milkdairy.R;
import com.prashant.milkdairy.Utils.FirebaseRefs;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class BonusManagementFragment extends Fragment {

    private TextView tvBonusRuleCount, tvActiveRuleCount, tvBonusPaid, tvActiveFarmer;
    private TextView tvRuleName, tvRuleRate, tvBonusPreview;

    private MaterialButton btnThisMonth, btnCustom, btnCalculate;
    private EditText etFromDate, etToDate;
    private AutoCompleteTextView actBonusRule;
    private LinearLayout layoutBonusLoader;
    private ImageView ivMore;
    private ImageView ivRuleStatus;
    private TextView tvRuleStatus;
    private LinearLayout layoutRuleStatus;
    private ExtendedFloatingActionButton fabAddBonus;

    private ArrayAdapter<String> ruleAdapter;

    private final ArrayList<BonusRuleModel> allRules = new ArrayList<>();
    private final ArrayList<BonusRuleModel> activeRules = new ArrayList<>();
    private final ArrayList<String> bonusRuleNames = new ArrayList<>();
    private final ArrayList<BonusResultModel> bonusResultList = new ArrayList<>();

    private ListenerRegistration ruleListener, distributionListener, farmerListener;
    private MaterialButton btnPercentage, btnFixed, btnPerAmount, btnPerLiter;

    private BonusRuleModel selectedActiveRule;
    private BonusRuleModel selectedCalculatedRule;
    private boolean bonusReadyToSave = false;

    private final SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.US);

    public BonusManagementFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bonus_management, container, false);

        initViews(view);
        setupRuleDropdown();
        setupDates();
        setupClicks();
        listenRules();
        listenDashboard();
        listenActiveFarmers();

        return view;
    }

    private void initViews(View view) {
        tvBonusRuleCount = view.findViewById(R.id.statTotalbonus).findViewById(R.id.tvDashnum);
        tvActiveRuleCount = view.findViewById(R.id.statactiverule).findViewById(R.id.tvDashnum);
        tvBonusPaid = view.findViewById(R.id.statbonuspaid).findViewById(R.id.tvDashnum);
        tvActiveFarmer = view.findViewById(R.id.statactivefarmer).findViewById(R.id.tvDashnum);

        TextView labelTotal = view.findViewById(R.id.statTotalbonus).findViewById(R.id.tvdashtext);
        TextView labelActive = view.findViewById(R.id.statactiverule).findViewById(R.id.tvdashtext);
        TextView labelPaid = view.findViewById(R.id.statbonuspaid).findViewById(R.id.tvdashtext);
        TextView labelFarmer = view.findViewById(R.id.statactivefarmer).findViewById(R.id.tvdashtext);

        labelTotal.setText("RULES");
        labelActive.setText("ACTIVE");
        labelPaid.setText("PAID");
        labelFarmer.setText("FARMERS");


        tvRuleName = view.findViewById(R.id.tvRuleName);
        tvRuleRate = view.findViewById(R.id.tvRuleRate);
        ivMore = view.findViewById(R.id.ivMore);

        ivRuleStatus = view.findViewById(R.id.ivRuleStatus);
        tvRuleStatus = view.findViewById(R.id.tvRuleStatus);
        layoutRuleStatus = view.findViewById(R.id.layoutRuleStatus);

        btnThisMonth = view.findViewById(R.id.btnThisMonth);
        btnCustom = view.findViewById(R.id.btncustom);
        btnCalculate = view.findViewById(R.id.btnCalculate);

        etFromDate = view.findViewById(R.id.etFromDate);
        etToDate = view.findViewById(R.id.etToDate);
        actBonusRule = view.findViewById(R.id.actBonusRule);

        tvBonusPreview = view.findViewById(R.id.tvBonusPreview);
        layoutBonusLoader = view.findViewById(R.id.layoutBonusLoader);
        fabAddBonus = view.findViewById(R.id.fabAddbonus);
    }

    private void setupRuleDropdown() {
        bonusRuleNames.clear();
        bonusRuleNames.add("Select Rule");

        ruleAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                bonusRuleNames
        );

        actBonusRule.setAdapter(ruleAdapter);
        actBonusRule.setThreshold(0);
        actBonusRule.setOnClickListener(v -> actBonusRule.showDropDown());

        actBonusRule.setOnItemClickListener((parent, view, position, id) -> {
            selectedActiveRule = getSelectedRule();
            updateActiveRuleCard(selectedActiveRule);
            clearResult();
        });
    }

    private void setupDates() {
        setThisMonthDates();

        etFromDate.setOnClickListener(v -> showDatePicker(etFromDate));
        etToDate.setOnClickListener(v -> showDatePicker(etToDate));
    }

    private void setupClicks() {
        fabAddBonus.setText("Add Rule");
        fabAddBonus.setOnClickListener(v -> showAddBonusRuleDialog(null));

        btnThisMonth.setOnClickListener(v -> {
            setThisMonthDates();
            clearResult();
        });

        btnCustom.setOnClickListener(v -> {
            selectPeriodButton(btnCustom);
            clearResult();
        });

        btnCalculate.setOnClickListener(v -> {
            if (bonusReadyToSave) {
                saveBonusDistribution();
            } else {
                calculateBonus();
            }
        });

        ivMore.setOnClickListener(this::showRuleMenu);
    }


    private void listenRules() {
        ruleListener = FirebaseRefs.bonusRules().addSnapshotListener((snapshots, error) -> {
            if (error != null) {
                Toast.makeText(requireContext(), error.getMessage(), Toast.LENGTH_LONG).show();
                return;
            }

            allRules.clear();
            activeRules.clear();
            bonusRuleNames.clear();
            bonusRuleNames.add("Select Rule");

            if (snapshots != null) {
                for (QueryDocumentSnapshot doc : snapshots) {
                    BonusRuleModel rule = new BonusRuleModel();
                    rule.setRuleId(doc.getId());
                    rule.setRuleName(safe(doc.getString("ruleName")));
                    rule.setBonusType(safe(doc.getString("bonusType")));
                    rule.setValue(getDouble(doc, "value"));
                    rule.setPerAmount(getDouble(doc, "perAmount"));
                    rule.setMinLiter(getDouble(doc, "minLiter"));
                    rule.setMilkType(safe(doc.getString("milkType")).isEmpty() ? "All" : doc.getString("milkType"));
                    rule.setDescription(safe(doc.getString("description")));

                    Boolean active = doc.getBoolean("active");
                    rule.setActive(active != null && active);

                    allRules.add(rule);

                    if (rule.isActive()) {
                        activeRules.add(rule);
                        bonusRuleNames.add(rule.getRuleName());
                    }
                }
            }

            ruleAdapter.notifyDataSetChanged();

            tvBonusRuleCount.setText(String.valueOf(allRules.size()));
            tvActiveRuleCount.setText(String.valueOf(activeRules.size()));
            btnCalculate.setEnabled(!activeRules.isEmpty());

            if (activeRules.isEmpty()) {

                if (selectedActiveRule != null) {

                    selectedActiveRule.setActive(false);

                    updateActiveRuleCard(selectedActiveRule);

                } else {

                    actBonusRule.setText("", false);

                    updateActiveRuleCard(null);
                }
            } else if (selectedActiveRule == null || !containsRule(selectedActiveRule.getRuleId())) {
                selectedActiveRule = activeRules.get(0);
                actBonusRule.setText(selectedActiveRule.getRuleName(), false);
                updateActiveRuleCard(selectedActiveRule);
            }
        });
    }

    private void listenDashboard() {
        distributionListener = FirebaseRefs.bonusDistributions().addSnapshotListener((snapshots, error) -> {
            if (error != null || snapshots == null) {
                tvBonusPaid.setText("\u20B9 0.00");
                return;
            }

            Calendar now = Calendar.getInstance();
            double thisMonthBonus = 0;

            for (QueryDocumentSnapshot doc : snapshots) {
                String periodTo = doc.getString("periodTo");
                String status = doc.getString("status");

                if ("CANCELLED".equalsIgnoreCase(status)) continue;

                try {
                    Calendar c = Calendar.getInstance();
                    c.setTime(sdf.parse(periodTo));

                    if (c.get(Calendar.MONTH) == now.get(Calendar.MONTH)
                            && c.get(Calendar.YEAR) == now.get(Calendar.YEAR)) {
                        thisMonthBonus += getDouble(doc, "bonusAmount");
                    }
                } catch (Exception ignored) {}
            }

            tvBonusPaid.setText(String.format(Locale.US, "₹ %.0f", thisMonthBonus));
        });
    }

    private void listenActiveFarmers() {
        farmerListener = FirebaseRefs.farmers().addSnapshotListener((snapshots, error) -> {
            if (error != null || snapshots == null) {
                tvActiveFarmer.setText("0");
                return;
            }

            int activeCount = 0;

            for (QueryDocumentSnapshot doc : snapshots) {
                if ("Active".equalsIgnoreCase(safe(doc.getString("status")))) {
                    activeCount++;
                }
            }

            tvActiveFarmer.setText(String.valueOf(activeCount));
        });
    }

    private void calculateBonus() {
        if (!validateDates()) return;

        BonusRuleModel rule = getSelectedRule();

        if (rule == null) {
            Toast.makeText(requireContext(), "Please select active bonus rule", Toast.LENGTH_SHORT).show();
            return;
        }

        selectedCalculatedRule = rule;
        bonusReadyToSave = false;

        layoutBonusLoader.setVisibility(View.VISIBLE);
        tvBonusPreview.setVisibility(View.GONE);
        btnCalculate.setEnabled(false);

        String from = etFromDate.getText().toString();
        String to = etToDate.getText().toString();

        FirebaseRefs.farmers().get().addOnSuccessListener(farmerSnapshots -> {
            Map<String, Farmer> farmerMap = new HashMap<>();

            for (QueryDocumentSnapshot doc : farmerSnapshots) {
                Farmer farmer = new Farmer();
                farmer.setId(doc.getId());
                farmer.setCode(doc.getString("code"));
                farmer.setName(doc.getString("name"));
                farmer.setMilkType(doc.getString("milkType"));
                farmer.setStatus(doc.getString("status"));
                farmerMap.put(farmer.getId(), farmer);
            }

            FirebaseRefs.bonusDistributions()
                    .whereEqualTo("bonusRuleId", rule.getRuleId())
                    .get()
                    .addOnSuccessListener(distributionSnapshots -> {
                        Set<String> duplicates = new HashSet<>();

                        for (QueryDocumentSnapshot doc : distributionSnapshots) {
                            String status = safe(doc.getString("status"));

                            if (!"CANCELLED".equalsIgnoreCase(status)
                                    && from.equals(doc.getString("periodFrom"))
                                    && to.equals(doc.getString("periodTo"))) {
                                duplicates.add(doc.getString("farmerId"));
                            }
                        }

                        FirebaseRefs.milkCollection().get()
                                .addOnSuccessListener(milkSnapshots -> calculateFromMilkSnapshots(
                                        milkSnapshots,
                                        farmerMap,
                                        duplicates,
                                        rule,
                                        from,
                                        to
                                ))
                                .addOnFailureListener(e -> showCalcError(e.getMessage()));
                    })
                    .addOnFailureListener(e -> showCalcError(e.getMessage()));
        }).addOnFailureListener(e -> showCalcError(e.getMessage()));
    }

    private void calculateFromMilkSnapshots(Iterable<QueryDocumentSnapshot> milkSnapshots,
                                            Map<String, Farmer> farmerMap,
                                            Set<String> duplicates,
                                            BonusRuleModel rule,
                                            String from,
                                            String to) {
        Map<String, FarmerBonusGroup> groups = new HashMap<>();

        for (QueryDocumentSnapshot doc : milkSnapshots) {
            String farmerId = doc.getString("farmerId");
            String date = doc.getString("date");
            String milkType = safe(doc.getString("milkType"));

            if (farmerId == null || !isDateInRange(date, from, to)) continue;

            String ruleMilkType = safe(rule.getMilkType());
            if (ruleMilkType.isEmpty()) ruleMilkType = "All";

            if (!"All".equalsIgnoreCase(ruleMilkType)
                    && !ruleMilkType.equalsIgnoreCase(milkType)) {
                continue;
            }

            Farmer farmer = farmerMap.get(farmerId);
            if (farmer == null || !"Active".equalsIgnoreCase(safe(farmer.getStatus()))) continue;
            if (duplicates.contains(farmerId)) continue;

            double liters = firstDouble(doc, "quantityLiters", "liters", "quantity", "milkLiter");
            double milkAmount = getMilkAmount(doc);
            double fat = firstDouble(doc, "fat");
            double snf = firstDouble(doc, "snf");

            if (liters <= 0) continue;

            FarmerBonusGroup group = groups.get(farmerId);

            if (group == null) {
                group = new FarmerBonusGroup(farmer);
                groups.put(farmerId, group);
            }

            group.totalLiters += liters;
            group.milkAmount += milkAmount;
            group.fatTotal += fat * liters;
            group.snfTotal += snf * liters;
        }

        bonusResultList.clear();

        double totalBonus = 0;
        double totalLiters = 0;

        for (FarmerBonusGroup group : groups.values()) {
            if (group.totalLiters < rule.getMinLiter()) continue;

            double avgFat = group.totalLiters == 0 ? 0 : group.fatTotal / group.totalLiters;
            double bonus = calculateRuleBonus(rule, group.totalLiters, group.milkAmount, avgFat);

            if (bonus <= 0) continue;

            bonusResultList.add(new BonusResultModel(
                    group.farmer.getId(),
                    group.farmer.getCode(),
                    group.farmer.getName(),
                    group.totalLiters,
                    group.milkAmount,
                    bonus
            ));

            totalBonus += bonus;
            totalLiters += group.totalLiters;
        }

        layoutBonusLoader.setVisibility(View.GONE);
        tvBonusPreview.setVisibility(View.VISIBLE);
        btnCalculate.setEnabled(true);

        if (bonusResultList.isEmpty()) {
            bonusReadyToSave = false;
            btnCalculate.setText("Calculate Bonus");
            tvBonusPreview.setText("No eligible farmers found. Check date range, active rule, milk collection, or duplicate bonus records.");
            return;
        }

        bonusReadyToSave = true;
        btnCalculate.setText("Save Bonus Distribution");

        tvBonusPreview.setText(String.format(
                Locale.US,
                "Calculated for %d farmers\nTotal Liters: %.2f L\nTotal Bonus: \u20B9 %.2f",
                bonusResultList.size(),
                totalLiters,
                totalBonus
        ));
    }

    private void saveBonusDistribution() {
        if (selectedCalculatedRule == null || bonusResultList.isEmpty()) {
            Toast.makeText(requireContext(), "Calculate bonus first", Toast.LENGTH_SHORT).show();
            return;
        }

        String from = etFromDate.getText().toString();
        String to = etToDate.getText().toString();

        btnCalculate.setEnabled(false);
        btnCalculate.setText("Saving...");

        FirebaseRefs.bonusDistributions()
                .whereEqualTo("bonusRuleId", selectedCalculatedRule.getRuleId())
                .get()
                .addOnSuccessListener(existingSnapshots -> {
                    Set<String> existing = new HashSet<>();

                    for (QueryDocumentSnapshot doc : existingSnapshots) {
                        String status = safe(doc.getString("status"));

                        if (!"CANCELLED".equalsIgnoreCase(status)
                                && from.equals(doc.getString("periodFrom"))
                                && to.equals(doc.getString("periodTo"))) {
                            existing.add(doc.getString("farmerId"));
                        }
                    }

                    WriteBatch batch = com.google.firebase.firestore.FirebaseFirestore.getInstance().batch();
                    int saveCount = 0;

                    for (BonusResultModel result : bonusResultList) {
                        if (existing.contains(result.getFarmerId())) continue;

                        String id = FirebaseRefs.bonusDistributions().document().getId();

                        Map<String, Object> map = new HashMap<>();
                        map.put("distributionId", id);
                        map.put("farmerId", result.getFarmerId());
                        map.put("farmerCode", result.getFarmerCode());
                        map.put("farmerName", result.getFarmerName());
                        map.put("totalLiters", result.getTotalLiters());
                        map.put("milkAmount", result.getMilkAmount());
                        map.put("bonusAmount", result.getBonusAmount());
                        map.put("bonusRuleId", selectedCalculatedRule.getRuleId());
                        map.put("bonusRuleName", selectedCalculatedRule.getRuleName());
                        map.put("bonusType", selectedCalculatedRule.getBonusType());
                        map.put("ruleValue", selectedCalculatedRule.getValue());
                        map.put("perAmount", selectedCalculatedRule.getPerAmount());
                        map.put("periodFrom", from);
                        map.put("periodTo", to);
                        map.put("status", "SAVED");
                        map.put("createdAt", FieldValue.serverTimestamp());

                        batch.set(FirebaseRefs.bonusDistributions().document(id), map);
                        saveCount++;
                    }

                    if (saveCount == 0) {
                        btnCalculate.setEnabled(true);
                        btnCalculate.setText("Calculate Bonus");
                        bonusReadyToSave = false;
                        Toast.makeText(requireContext(), "Bonus already saved for this period and rule", Toast.LENGTH_LONG).show();
                        return;
                    }

                    batch.commit()
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(requireContext(), "Bonus distribution saved", Toast.LENGTH_SHORT).show();
                                clearResult();
                            })
                            .addOnFailureListener(e -> {
                                btnCalculate.setEnabled(true);
                                btnCalculate.setText("Save Bonus Distribution");
                                Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    btnCalculate.setEnabled(true);
                    btnCalculate.setText("Save Bonus Distribution");
                    Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void showAddBonusRuleDialog(BonusRuleModel editingRule) {
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_add_bonus_rule);

        Window window = dialog.getWindow();

        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        ImageView btnClose = dialog.findViewById(R.id.btnClose);
        Button btnCancel = dialog.findViewById(R.id.btnCancel);
        Button btnAddRule = dialog.findViewById(R.id.btnAddRule);

        EditText etRuleName = dialog.findViewById(R.id.etRuleName);
        EditText etValue = dialog.findViewById(R.id.etValue);
        EditText etPerAmount = dialog.findViewById(R.id.etPerAmount);
        EditText etDescription = dialog.findViewById(R.id.etDescription);

        TextView tvValueLabel = dialog.findViewById(R.id.tvValueLabel);
        TextView tvPerAmountLabel = dialog.findViewById(R.id.tvPerAmountLabel);

        MaterialButton btnPercentage = dialog.findViewById(R.id.btnPercentage);
        MaterialButton btnFixed = dialog.findViewById(R.id.btnFixed);
        MaterialButton btnPerAmount = dialog.findViewById(R.id.btnPerAmount);
        MaterialButton btnPerLiter = dialog.findViewById(R.id.btnPerLiter);

        CheckBox cbActive = dialog.findViewById(R.id.cbActive);

        final String[] selectedType = {"PERCENTAGE"};

        if (editingRule != null) {
            etRuleName.setText(safe(editingRule.getRuleName()));
            etValue.setText(String.valueOf(editingRule.getValue()));
            etPerAmount.setText(String.valueOf(editingRule.getPerAmount()));
            etDescription.setText(safe(editingRule.getDescription()));
            cbActive.setChecked(editingRule.isActive());
            selectedType[0] = safe(editingRule.getBonusType()).isEmpty()
                    ? "PERCENTAGE"
                    : editingRule.getBonusType();
            btnAddRule.setText("Save Changes");
        }

        View.OnClickListener applySelection = v -> {

            if ("PERCENTAGE".equals(selectedType[0])) {

                selectBonusType(
                        btnPercentage,
                        btnPercentage,
                        btnFixed,
                        btnPerAmount,
                        btnPerLiter);

                tvPerAmountLabel.setVisibility(View.GONE);
                etPerAmount.setVisibility(View.GONE);

                tvValueLabel.setText("Value (%) *");

            } else if ("FIXED".equals(selectedType[0])) {

                selectBonusType(
                        btnFixed,
                        btnPercentage,
                        btnFixed,
                        btnPerAmount,
                        btnPerLiter);

                tvPerAmountLabel.setVisibility(View.GONE);
                etPerAmount.setVisibility(View.GONE);

                tvValueLabel.setText("Value (₹ fixed) *");

            } else if ("PER_AMOUNT".equals(selectedType[0])) {

                selectBonusType(
                        btnPerAmount,
                        btnPercentage,
                        btnFixed,
                        btnPerAmount,
                        btnPerLiter);

                tvPerAmountLabel.setVisibility(View.VISIBLE);
                etPerAmount.setVisibility(View.VISIBLE);

                tvValueLabel.setText("Bonus Value (₹) *");

            } else {

                selectBonusType(
                        btnPerLiter,
                        btnPercentage,
                        btnFixed,
                        btnPerAmount,
                        btnPerLiter);

                tvPerAmountLabel.setVisibility(View.GONE);
                etPerAmount.setVisibility(View.GONE);

                tvValueLabel.setText("Value (₹ per liter) *");
            }
        };

        btnPercentage.setOnClickListener(v -> {
            selectedType[0] = "PERCENTAGE";
            applySelection.onClick(v);
        });

        btnFixed.setOnClickListener(v -> {
            selectedType[0] = "FIXED";
            applySelection.onClick(v);
        });

        btnPerAmount.setOnClickListener(v -> {
            selectedType[0] = "PER_AMOUNT";
            applySelection.onClick(v);
        });

        btnPerLiter.setOnClickListener(v -> {
            selectedType[0] = "PER_LITER";
            applySelection.onClick(v);
        });

        applySelection.onClick(null);

        btnClose.setOnClickListener(v -> dialog.dismiss());
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnAddRule.setOnClickListener(v -> {
            String name = etRuleName.getText().toString().trim();
            String valueText = etValue.getText().toString().trim();
            String perAmountText = etPerAmount.getText().toString().trim();

            if (name.isEmpty()) {
                etRuleName.setError("Rule name required");
                return;
            }

            if (valueText.isEmpty()) {
                etValue.setError("Value required");
                return;
            }

            double value = parseDoubleSafe(valueText);

            if (value <= 0) {
                etValue.setError("Value must be greater than 0");
                return;
            }

            double perAmount = 0;

            if ("PER_AMOUNT".equals(selectedType[0])) {
                if (perAmountText.isEmpty()) {
                    etPerAmount.setError("Per amount required");
                    return;
                }

                perAmount = parseDoubleSafe(perAmountText);

                if (perAmount <= 0) {
                    etPerAmount.setError("Must be greater than 0");
                    return;
                }
            }

            if (isDuplicateRuleName(name, editingRule)) {
                etRuleName.setError("Rule name already exists");
                return;
            }

            String ruleId = editingRule == null
                    ? FirebaseRefs.bonusRules().document().getId()
                    : editingRule.getRuleId();

            Map<String, Object> map = new HashMap<>();
            map.put("ruleId", ruleId);
            map.put("ruleName", name);
            map.put("bonusType", selectedType[0]);
            map.put("value", value);
            map.put("perAmount", perAmount);
            map.put("minLiter", editingRule == null ? 0 : editingRule.getMinLiter());
            map.put("milkType", editingRule == null ? "All" : editingRule.getMilkType());
            map.put("active", cbActive.isChecked());
            map.put("description", etDescription.getText().toString().trim());
            map.put("updatedAt", FieldValue.serverTimestamp());

            if (editingRule == null) {
                map.put("createdAt", FieldValue.serverTimestamp());
            }

            final double finalPerAmount = perAmount;
            final double finalValue = value;
            final String finalName = name;

            FirebaseRefs.bonusRules().document(ruleId).set(map)
                    .addOnSuccessListener(unused -> {

                        dialog.dismiss();

                        Toast.makeText(
                                requireContext(),
                                "Rule saved",
                                Toast.LENGTH_SHORT
                        ).show();

                        if (editingRule != null) {

                            editingRule.setRuleName(finalName);
                            editingRule.setBonusType(selectedType[0]);
                            editingRule.setValue(finalValue);
                            editingRule.setPerAmount(finalPerAmount);
                            editingRule.setDescription(
                                    etDescription.getText().toString().trim()
                            );
                            editingRule.setActive(cbActive.isChecked());

                            selectedActiveRule = editingRule;

                            updateActiveRuleCard(selectedActiveRule);
                        }
                    });
        });

        dialog.show();
    }

    private void showRuleMenu(View anchor) {
        PopupMenu menu = new PopupMenu(requireContext(), anchor);

        if (selectedActiveRule == null) {

            menu.getMenu().add("Add Rule");

        } else {

            menu.getMenu().add("Edit Rule");

            if (selectedActiveRule.isActive()) {
                menu.getMenu().add("Deactivate Rule");
            } else {
                menu.getMenu().add("Activate Rule");
            }

            menu.getMenu().add("Delete Rule");
        }

        if (bonusReadyToSave) {
            menu.getMenu().add("Save Calculated Bonus");
        }

        menu.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();

            if ("Add Rule".equals(title)) {
                showAddBonusRuleDialog(null);
                return true;
            }

            if ("Edit Rule".equals(title)) {
                showAddBonusRuleDialog(selectedActiveRule);
                return true;
            }

            if ("Deactivate Rule".equals(title)
                    || "Activate Rule".equals(title)) {

                toggleRule(selectedActiveRule);
                return true;
            }

            if ("Delete Rule".equals(title)) {
                deleteRule(selectedActiveRule);
                return true;
            }

            if ("Save Calculated Bonus".equals(title)) {
                saveBonusDistribution();
                return true;
            }

            return false;
        });

        menu.show();
    }

    private void toggleRule(BonusRuleModel rule) {

        if (rule == null) return;

        boolean newStatus = !rule.isActive();

        FirebaseRefs.bonusRules()
                .document(rule.getRuleId())
                .update(
                        "active", newStatus,
                        "updatedAt", FieldValue.serverTimestamp()
                )
                .addOnSuccessListener(unused -> {

                    rule.setActive(newStatus);

                    selectedActiveRule = rule;

                    updateActiveRuleCard(rule);

                    Toast.makeText(
                            requireContext(),
                            newStatus
                                    ? "Rule activated"
                                    : "Rule deactivated",
                            Toast.LENGTH_SHORT
                    ).show();

                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                requireContext(),
                                e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show());
    }

    private void deleteRule(BonusRuleModel rule) {
        if (rule == null) return;

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Delete Rule")
                .setMessage("Are you sure you want to delete this bonus rule?")
                .setPositiveButton("Delete", (dialog, which) -> FirebaseRefs.bonusRules()
                        .document(rule.getRuleId())
                        .delete()
                        .addOnSuccessListener(unused -> {

                            Toast.makeText(
                                    requireContext(),
                                    "Rule deleted successfully",
                                    Toast.LENGTH_SHORT
                            ).show();

                            updateActiveRuleCard(null);

                            actBonusRule.setText("", false);

                            clearResult();

                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(
                                        requireContext(),
                                        e.getMessage(),
                                        Toast.LENGTH_LONG
                                ).show()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private boolean containsRule(String ruleId) {
        for (BonusRuleModel rule : activeRules) {
            if (safe(rule.getRuleId()).equals(ruleId)) {
                return true;
            }
        }

        return false;
    }

    private void updateActiveRuleCard(BonusRuleModel rule) {

        if (rule == null) {

            tvRuleName.setText("No Active Bonus Rule");

            tvRuleRate.setText(
                    "Create or enable a rule to calculate bonus"
            );

            ivRuleStatus.setImageResource(R.drawable.plus);

            tvRuleStatus.setText("No Rule");

            tvRuleStatus.setTextColor(
                    Color.parseColor("#9CA3AF")
            );

            btnCalculate.setEnabled(false);

            return;
        }

        tvRuleName.setText(rule.getRuleName());

        if (rule.isActive()) {

            ivRuleStatus.setImageResource(
                    android.R.drawable.checkbox_on_background
            );

            tvRuleStatus.setText("Active");

            tvRuleStatus.setTextColor(
                    Color.parseColor("#27AE60")
            );

            tvRuleRate.setText(
                    getRuleText(rule)
            );

            btnCalculate.setEnabled(true);

        } else {

            ivRuleStatus.setImageResource(
                    android.R.drawable.ic_media_pause
            );

            tvRuleStatus.setText("Inactive");

            tvRuleStatus.setTextColor(
                    Color.parseColor("#F59E0B")
            );

            tvRuleRate.setText(
                    "Rule is currently disabled"
            );

            btnCalculate.setEnabled(false);
        }
    }

    private BonusRuleModel getSelectedRule() {
        String selectedName = actBonusRule.getText().toString().trim();

        if (selectedName.isEmpty() || "Select Rule".equalsIgnoreCase(selectedName)) {
            return null;
        }

        for (BonusRuleModel rule : activeRules) {
            if (safe(rule.getRuleName()).equalsIgnoreCase(selectedName)) {
                return rule;
            }
        }

        return null;
    }

    private String getRuleText(BonusRuleModel rule) {
        String type = safe(rule.getBonusType());

        if ("PERCENTAGE".equals(type)) {
            return String.format(Locale.US, "%.2f%% of Milk Amount", rule.getValue());
        }

        if ("FIXED".equals(type)) {
            return String.format(Locale.US, "\u20B9 %.2f Fixed Bonus", rule.getValue());
        }

        if ("PER_AMOUNT".equals(type)) {
            return String.format(Locale.US, "\u20B9 %.2f per \u20B9 %.2f milk amount",
                    rule.getValue(),
                    rule.getPerAmount());
        }

        return String.format(Locale.US, "\u20B9 %.2f per Liter", rule.getValue());
    }

    private double calculateRuleBonus(BonusRuleModel rule, double liters, double milkAmount, double avgFat) {
        String type = safe(rule.getBonusType());

        if ("PERCENTAGE".equals(type)) {
            return milkAmount * rule.getValue() / 100.0;
        }

        if ("FIXED".equals(type)) {
            return rule.getValue();
        }

        if ("PER_AMOUNT".equals(type)) {
            if (rule.getPerAmount() <= 0) return 0;
            return (milkAmount / rule.getPerAmount()) * rule.getValue();
        }

        return liters * rule.getValue();
    }

    private void setThisMonthDates() {
        Calendar c = Calendar.getInstance();

        c.set(Calendar.DAY_OF_MONTH, 1);
        etFromDate.setText(sdf.format(c.getTime()));

        c = Calendar.getInstance();
        etToDate.setText(sdf.format(c.getTime()));

        selectPeriodButton(btnThisMonth);
    }

    private void showDatePicker(EditText target) {
        Calendar calendar = Calendar.getInstance();

        new DatePickerDialog(requireContext(), (view, year, month, day) -> {
            calendar.set(year, month, day);
            target.setText(sdf.format(calendar.getTime()));
            clearResult();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void selectPeriodButton(MaterialButton selectedButton) {
        setPeriodButton(btnThisMonth, selectedButton == btnThisMonth);
        setPeriodButton(btnCustom, selectedButton == btnCustom);
    }

    private void setPeriodButton(MaterialButton button, boolean selected) {
        int bgColor = selected ? Color.parseColor("#ECFDF5") : Color.parseColor("#F4F7FA");
        int textColor = selected ? Color.parseColor("#10B981") : Color.parseColor("#667085");

        button.setBackgroundTintList(ColorStateList.valueOf(bgColor));
        button.setTextColor(textColor);
    }
    private void selectBonusType(
            MaterialButton selectedButton,
            MaterialButton btnPercentage,
            MaterialButton btnFixed,
            MaterialButton btnPerAmount,
            MaterialButton btnPerLiter) {

        setBonusButton(btnPercentage, selectedButton == btnPercentage);
        setBonusButton(btnFixed, selectedButton == btnFixed);
        setBonusButton(btnPerAmount, selectedButton == btnPerAmount);
        setBonusButton(btnPerLiter, selectedButton == btnPerLiter);
    }

    private void setBonusButton(MaterialButton button, boolean selected) {

        int bgColor = selected
                ? Color.parseColor("#14B8A6")
                : Color.parseColor("#FFFFFF");

        int textColor = selected
                ? Color.WHITE
                : Color.parseColor("#667085");

        button.setBackgroundTintList(
                ColorStateList.valueOf(bgColor));

        button.setTextColor(textColor);

        button.setStrokeWidth(selected ? 0 : 1);

        button.setStrokeColor(
                ColorStateList.valueOf(
                        Color.parseColor("#D0D5DD")
                )
        );
    }

    private boolean validateDates() {
        String from = etFromDate.getText().toString();
        String to = etToDate.getText().toString();

        if (from.isEmpty() || to.isEmpty()) {
            Toast.makeText(requireContext(), "Select period dates", Toast.LENGTH_SHORT).show();
            return false;
        }

        try {
            long fromTime = sdf.parse(from).getTime();
            long toTime = sdf.parse(to).getTime();
            long today = Calendar.getInstance().getTimeInMillis();

            if (fromTime > toTime) {
                Toast.makeText(requireContext(), "From date cannot exceed To date", Toast.LENGTH_SHORT).show();
                return false;
            }

            if (fromTime > today || toTime > today) {
                Toast.makeText(requireContext(), "Future dates are not allowed", Toast.LENGTH_SHORT).show();
                return false;
            }

            return true;
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Invalid date format", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private boolean isDateInRange(String date, String from, String to) {
        try {
            long d = parseFlexibleDate(date);
            long f = sdf.parse(from).getTime();
            long t = sdf.parse(to).getTime();

            return d >= f && d <= t;
        } catch (Exception e) {
            return false;
        }
    }

    private long parseFlexibleDate(String date) throws ParseException {
        if (date == null || date.trim().isEmpty()) {
            throw new ParseException("Empty date", 0);
        }

        try {
            return new SimpleDateFormat("dd-MM-yyyy", Locale.US).parse(date).getTime();
        } catch (Exception ignored) {}

        try {
            return new SimpleDateFormat("dd/MM/yyyy", Locale.US).parse(date).getTime();
        } catch (Exception ignored) {}

        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date).getTime();
    }

    private void clearResult() {
        bonusResultList.clear();
        selectedCalculatedRule = null;
        bonusReadyToSave = false;

        layoutBonusLoader.setVisibility(View.GONE);
        tvBonusPreview.setVisibility(View.VISIBLE);
        btnCalculate.setEnabled(!activeRules.isEmpty());
        btnCalculate.setText("Calculate Bonus");
        tvBonusPreview.setText("No bonus calculated yet. Select period and rule, then click 'Calculate Bonus'.");
    }

    private void showCalcError(String message) {
        layoutBonusLoader.setVisibility(View.GONE);
        tvBonusPreview.setVisibility(View.VISIBLE);
        btnCalculate.setEnabled(true);
        btnCalculate.setText("Calculate Bonus");
        bonusReadyToSave = false;

        Toast.makeText(
                requireContext(),
                message == null ? "Bonus calculation failed" : message,
                Toast.LENGTH_LONG
        ).show();
    }

    private boolean isDuplicateRuleName(String name, BonusRuleModel editingRule) {
        for (BonusRuleModel rule : allRules) {
            if (editingRule != null && safe(rule.getRuleId()).equals(editingRule.getRuleId())) {
                continue;
            }

            if (safe(rule.getRuleName()).equalsIgnoreCase(name)) {
                return true;
            }
        }

        return false;
    }

    private double getMilkAmount(QueryDocumentSnapshot doc) {
        double total = firstDouble(doc, "total", "grossAmount", "milkAmount", "amount", "totalAmount");
        if (total > 0) return total;

        double liters = firstDouble(doc, "quantityLiters", "liters", "quantity", "milkLiter");
        double rate = firstDouble(doc, "ratePerLiter", "rate", "milkRate");

        return liters * rate;
    }

    private double getDouble(QueryDocumentSnapshot doc, String key) {
        Object value = doc.get(key);

        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }

        if (value != null) {
            return parseDoubleSafe(value.toString());
        }

        return 0;
    }

    private double firstDouble(QueryDocumentSnapshot doc, String... keys) {
        for (String key : keys) {
            Object value = doc.get(key);

            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }

            if (value != null) {
                double parsed = parseDoubleSafe(value.toString()
                        .replace("\u20B9", "")
                        .replace(",", "")
                        .trim());

                if (parsed != 0) {
                    return parsed;
                }
            }
        }

        return 0;
    }

    private double parseDoubleSafe(String value) {
        try {
            if (value == null || value.trim().isEmpty()) return 0;
            return Double.parseDouble(value.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (ruleListener != null) ruleListener.remove();
        if (distributionListener != null) distributionListener.remove();
        if (farmerListener != null) farmerListener.remove();
    }

    private static class FarmerBonusGroup {
        Farmer farmer;
        double totalLiters = 0;
        double milkAmount = 0;
        double fatTotal = 0;
        double snfTotal = 0;

        FarmerBonusGroup(Farmer farmer) {
            this.farmer = farmer;
        }
    }
}