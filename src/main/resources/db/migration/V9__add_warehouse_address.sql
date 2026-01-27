-- Add address to warehouses for richer location details
IF COL_LENGTH('warehouses', 'address') IS NULL
BEGIN
    ALTER TABLE warehouses ADD address NVARCHAR(512) NULL;
END;
GO
