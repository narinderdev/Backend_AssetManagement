package com.example.eam.Enum;

public enum InventoryTransactionType {
    RECEIVE,    // stock increased
    ISSUE,      // stock decreased
    RETURN,     // stock increased back
    ADJUSTMENT  // manual/reconciliation adjustments
}
