-- Add auto-create-asset fields to work order types and supporting property unit on assets
IF OBJECT_ID('work_order_types', 'U') IS NOT NULL AND COL_LENGTH('work_order_types', 'create_asset') IS NULL
BEGIN
    ALTER TABLE work_order_types
    ADD create_asset BIT NOT NULL CONSTRAINT df_work_order_types_create_asset DEFAULT 0;
END;
IF OBJECT_ID('work_order_types', 'U') IS NOT NULL AND COL_LENGTH('work_order_types', 'property_unit') IS NULL
BEGIN
    ALTER TABLE work_order_types ADD property_unit NVARCHAR(128) NULL;
END;
IF OBJECT_ID('work_order_types', 'U') IS NOT NULL AND COL_LENGTH('work_order_types', 'property_group') IS NULL
BEGIN
    ALTER TABLE work_order_types ADD property_group NVARCHAR(128) NULL;
END;
IF OBJECT_ID('work_order_types', 'U') IS NOT NULL AND COL_LENGTH('work_order_types', 'retirement_unit') IS NULL
BEGIN
    ALTER TABLE work_order_types ADD retirement_unit NVARCHAR(128) NULL;
END;
IF OBJECT_ID('work_order_types', 'U') IS NOT NULL AND COL_LENGTH('work_order_types', 'functional_class') IS NULL
BEGIN
    ALTER TABLE work_order_types ADD functional_class NVARCHAR(128) NULL;
END;
IF OBJECT_ID('assets', 'U') IS NOT NULL AND COL_LENGTH('assets', 'property_unit') IS NULL
BEGIN
    ALTER TABLE assets ADD property_unit NVARCHAR(128) NULL;
END;

