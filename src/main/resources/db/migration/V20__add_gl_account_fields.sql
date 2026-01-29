-- Add GL account and expense code fields to inventory_items and purchase_orders

IF COL_LENGTH('inventory_items', 'gl_account_string') IS NULL
BEGIN
    ALTER TABLE inventory_items
    ADD gl_account_string NVARCHAR(255) NULL;
END;

IF COL_LENGTH('inventory_items', 'expense_code') IS NULL
BEGIN
    ALTER TABLE inventory_items
    ADD expense_code NVARCHAR(64) NULL;
END;

IF COL_LENGTH('purchase_orders', 'gl_account_string') IS NULL
BEGIN
    ALTER TABLE purchase_orders
    ADD gl_account_string NVARCHAR(255) NULL;
END;
