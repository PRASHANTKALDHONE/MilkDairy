package com.prashant.milkdairy.Utils;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class FirebaseRefs {

    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public static String currentUserId() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            throw new IllegalStateException("User is not logged in");
        }

        return FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    public static DocumentReference currentUserDoc() {
        return db.collection("users")
                .document(currentUserId());
    }

    public static CollectionReference farmers() {
        return currentUserDoc().collection("farmers");
    }

    public static CollectionReference milkCollection() {
        return currentUserDoc().collection("milkCollection");
    }

    public static CollectionReference bills() {
        return currentUserDoc().collection("bills");
    }

    public static CollectionReference bonusRules() {
        return currentUserDoc().collection("bonusRules");
    }

    public static CollectionReference bonusDistributions() {
        return currentUserDoc().collection("bonusDistributions");
    }

    public static CollectionReference rateCharts() {
        return currentUserDoc().collection("rateCharts");
    }

    public static CollectionReference fatSnfSettings() {
        return currentUserDoc().collection("fatSnfSettings");
    }

    public static CollectionReference inventoryItems() {
        return currentUserDoc().collection("inventory_items");
    }

    public static CollectionReference inventoryTransactions() {
        return currentUserDoc().collection("inventory_transactions");
    }

    public static CollectionReference advanceFood() {
        return currentUserDoc().collection("advanceFood");
    }

    // Reports module aliases

    public static CollectionReference milkReportsSource() {
        return milkCollection();
    }

    public static CollectionReference billingReportsSource() {
        return bills();
    }

    public static CollectionReference deductionReportsSource() {
        return advanceFood();
    }

    public static CollectionReference inventoryItemsReportsSource() {
        return inventoryItems();
    }

    public static CollectionReference inventoryTransactionsReportsSource() {
        return inventoryTransactions();
    }

    public static CollectionReference bonusReportsSource() {
        return bonusDistributions();
    }
}
