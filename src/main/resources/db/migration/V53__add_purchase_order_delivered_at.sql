IF COL_LENGTH('purchase_orders', 'delivered_at') IS NULL
BEGIN
    ALTER TABLE purchase_orders ADD delivered_at DATETIME2 NULL;
END;
