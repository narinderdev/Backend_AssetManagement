-- Store free-text asset name on work orders for auto asset creation

IF COL_LENGTH('work_orders', 'asset_name_input') IS NULL
BEGIN
    ALTER TABLE work_orders ADD asset_name_input NVARCHAR(255) NULL;
END;
