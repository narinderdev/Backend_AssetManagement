-- Add department field to MR and PO
IF OBJECT_ID('material_requisitions', 'U') IS NOT NULL AND COL_LENGTH('material_requisitions', 'department') IS NULL
BEGIN
    ALTER TABLE material_requisitions ADD department NVARCHAR(255) NULL;
END;
GO
IF OBJECT_ID('purchase_orders', 'U') IS NOT NULL AND COL_LENGTH('purchase_orders', 'department') IS NULL
BEGIN
    ALTER TABLE purchase_orders ADD department NVARCHAR(255) NULL;
END;
GO

