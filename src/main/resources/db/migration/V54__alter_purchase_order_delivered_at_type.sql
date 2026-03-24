-- Align delivered_at with UTC offset type expected by Hibernate
IF OBJECT_ID('purchase_orders', 'U') IS NOT NULL AND COL_LENGTH('purchase_orders', 'delivered_at') IS NOT NULL
BEGIN
    ALTER TABLE purchase_orders
    ALTER COLUMN delivered_at DATETIMEOFFSET(7) NULL;
END;

