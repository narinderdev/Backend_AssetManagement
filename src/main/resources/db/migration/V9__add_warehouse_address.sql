-- Add address to warehouses for richer location details
IF OBJECT_ID('warehouses', 'U') IS NOT NULL AND COL_LENGTH('warehouses', 'address') IS NULL
BEGIN
    ALTER TABLE warehouses ADD address NVARCHAR(512) NULL;
END;
GO

