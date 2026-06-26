package com.prashant.milkdairy.ui.Dashboard;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.prashant.milkdairy.Model.DashboardModel;
import com.prashant.milkdairy.R;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class DashboardFragment extends Fragment {

    private static final String TAG = "DashboardFragment";
    private static final long NAVIGATION_DELAY_MS = 700;

    private TextView tvGreeting;
    private TextView tvDashboardDate;

    private TextView tvMilkLiters;
    private TextView tvMilkAmount;

    private TextView tvInventoryValue;
    private TextView tvInventoryLabel;

    private TextView tvBillsCount;
    private TextView tvBillsAmount;

    private TextView tvFarmersCount;
    private TextView tvFarmersLabel;

    private TextView tvMorningCow;
    private TextView tvMorningBuffalo;
    private TextView tvMorningMixed;
    private TextView tvMorningTotal;

    private TextView tvEveningCow;
    private TextView tvEveningBuffalo;
    private TextView tvEveningMixed;
    private TextView tvEveningTotal;

    private DashboardRepository repository;
    private long lastNavigationTime;

    public DashboardFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        bindHeader();
        renderDashboard(new DashboardModel());
        setupNavigation(view);

        repository = new DashboardRepository();

        repository.startListening(new DashboardRepository.DashboardCallback() {
            @Override
            public void onDashboardUpdated(DashboardModel model) {
                if (!isAdded()) return;
                renderDashboard(model);
            }

            @Override
            public void onDashboardError(String source, Exception error) {
                Log.e(TAG, "Dashboard load failed: " + source, error);
            }
        });
    }

    private void initViews(View view) {
        tvGreeting = view.findViewById(R.id.tvGreeting);
        tvDashboardDate = view.findViewById(R.id.tvDashboardDate);

        tvMilkLiters = view.findViewById(R.id.tvMilkLiters);
        tvMilkAmount = view.findViewById(R.id.tvMilkAmount);

        tvInventoryValue = view.findViewById(R.id.tvInventoryValue);
        tvInventoryLabel = view.findViewById(R.id.tvInventoryLabel);

        tvBillsCount = view.findViewById(R.id.tvBillsCount);
        tvBillsAmount = view.findViewById(R.id.tvBillsAmount);

        tvFarmersCount = view.findViewById(R.id.tvFarmersCount);
        tvFarmersLabel = view.findViewById(R.id.tvFarmersLabel);

        tvMorningCow = view.findViewById(R.id.tvMorningCow);
        tvMorningBuffalo = view.findViewById(R.id.tvMorningBuffalo);
        tvMorningMixed = view.findViewById(R.id.tvMorningMixed);
        tvMorningTotal = view.findViewById(R.id.tvMorningTotal);

        tvEveningCow = view.findViewById(R.id.tvEveningCow);
        tvEveningBuffalo = view.findViewById(R.id.tvEveningBuffalo);
        tvEveningMixed = view.findViewById(R.id.tvEveningMixed);
        tvEveningTotal = view.findViewById(R.id.tvEveningTotal);
    }

    private void bindHeader() {
        Calendar now = Calendar.getInstance();
        int hour = now.get(Calendar.HOUR_OF_DAY);

        String greeting;

        if (hour >= 5 && hour < 12) {
            greeting = "Good Morning \u2600\uFE0F";
        } else if (hour >= 12 && hour < 17) {
            greeting = "Good Afternoon \uD83C\uDF24\uFE0F";
        } else if (hour >= 17 && hour < 21) {
            greeting = "Good Evening \uD83C\uDF19";
        } else {
            greeting = "Good Evening \uD83C\uDF19";
        }

        String date = new SimpleDateFormat(
                "dd MMM yyyy, EEEE",
                Locale.getDefault()
        ).format(now.getTime());

        tvGreeting.setText(greeting);
        tvDashboardDate.setText(date);
    }

    private void renderDashboard(DashboardModel model) {
        tvMilkLiters.setText(formatLiters(model.getMilkLitersToday()));
        tvMilkAmount.setText(formatCurrency(model.getMilkAmountToday()));

        tvInventoryValue.setText(formatCurrency(model.getInventoryValue()));
        tvInventoryLabel.setText("Stock Value");

        tvBillsCount.setText(String.valueOf(model.getBillsCount()));
        tvBillsAmount.setText(formatCurrency(model.getBillsAmount()));

        tvFarmersCount.setText(String.valueOf(model.getActiveFarmers()));
        tvFarmersLabel.setText("Active Farmers");

        tvMorningCow.setText(formatLiters(model.getMorningCow()));
        tvMorningBuffalo.setText(formatLiters(model.getMorningBuffalo()));
        tvMorningMixed.setText(formatLiters(model.getMorningMixed()));
        tvMorningTotal.setText("Total: " + formatLiters(model.getMorningTotal()));

        tvEveningCow.setText(formatLiters(model.getEveningCow()));
        tvEveningBuffalo.setText(formatLiters(model.getEveningBuffalo()));
        tvEveningMixed.setText(formatLiters(model.getEveningMixed()));
        tvEveningTotal.setText("Total: " + formatLiters(model.getEveningTotal()));
    }

    private void setupNavigation(View view) {
        view.findViewById(R.id.cardManageFarmers).setOnClickListener(v ->
                safeNavigate(v, "nav_add_farmer", "fragment_add_farmer"));

        view.findViewById(R.id.cardMilkCollection).setOnClickListener(v ->
                safeNavigate(v, "nav_milkCollection", "fragment_milk_collection"));

        view.findViewById(R.id.cardInventory).setOnClickListener(v ->
                safeNavigate(
                        v,
                        "nav_add_inventory_item",
                        "fragment_add__inventory__item",
                        "nav_inventory"
                ));

        view.findViewById(R.id.cardGenerateBill).setOnClickListener(v ->
                safeNavigate(v, "nav_bills", "fragment_bills"));

        view.findViewById(R.id.cardAddDeduction).setOnClickListener(v ->
                safeNavigate(v, "addDeductionFragment", "fragment_add_deduction"));

        view.findViewById(R.id.cardViewReports).setOnClickListener(v ->
                safeNavigate(v, "nav_reports", "fragment_reports"));
    }

    private void safeNavigate(View source, String... destinationNames) {
        long now = System.currentTimeMillis();

        if (now - lastNavigationTime < NAVIGATION_DELAY_MS) {
            return;
        }

        NavController navController = Navigation.findNavController(source);

        for (String name : destinationNames) {
            int destinationId = getResources().getIdentifier(
                    name,
                    "id",
                    requireContext().getPackageName()
            );

            if (destinationId == 0) {
                continue;
            }

            if (navController.getCurrentDestination() != null
                    && navController.getCurrentDestination().getId() == destinationId) {
                return;
            }

            try {
                navController.navigate(destinationId);
                lastNavigationTime = now;
                return;
            } catch (IllegalArgumentException error) {
                Log.w(TAG, "Navigation destination unavailable: " + name, error);
            }
        }

        Toast.makeText(
                requireContext(),
                "Navigation destination not configured",
                Toast.LENGTH_SHORT
        ).show();
    }

    private String formatLiters(double value) {
        return String.format(Locale.US, "%.2f L", value);
    }

    private String formatCurrency(double value) {
        NumberFormat formatter = NumberFormat.getNumberInstance(new Locale("en", "IN"));
        formatter.setMaximumFractionDigits(0);
        formatter.setMinimumFractionDigits(0);

        return "\u20B9" + formatter.format(value);
    }

    @Override
    public void onDestroyView() {
        if (repository != null) {
            repository.stopListening();
        }

        super.onDestroyView();
    }
}