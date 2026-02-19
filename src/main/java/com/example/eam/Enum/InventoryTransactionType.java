package com.example.eam.Enum;

public enum InventoryTransactionType {
    RECEIVE,    // stock increased
    ISSUE,      // stock decreased
    RETURN,     // stock returned to vendor (stock decreased)
    ADJUSTMENT  // manual/reconciliation adjustments
}
