-- Store asset metadata inputs on work_orders for auto asset creation
IF OBJECT_ID('work_orders', 'U') IS NOT NULL AND COL_LENGTH('work_orders', 'asset_serial_input') IS NULL
BEGIN
    ALTER TABLE work_orders ADD asset_serial_input NVARCHAR(128) NULL;
END;
IF OBJECT_ID('work_orders', 'U') IS NOT NULL AND COL_LENGTH('work_orders', 'asset_model_input') IS NULL
BEGIN
    ALTER TABLE work_orders ADD asset_model_input NVARCHAR(128) NULL;
END;
IF OBJECT_ID('work_orders', 'U') IS NOT NULL AND COL_LENGTH('work_orders', 'asset_manufacture_date_input') IS NULL
BEGIN
    ALTER TABLE work_orders ADD asset_manufacture_date_input DATE NULL;
END;

