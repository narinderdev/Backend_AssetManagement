-- Add additional asset classification/finance identifiers to the assets table
IF COL_LENGTH('assets', 'functional_class') IS NULL
BEGIN
    ALTER TABLE assets ADD functional_class NVARCHAR(128) NULL;
END;

IF COL_LENGTH('assets', 'retirement_unit') IS NULL
BEGIN
    ALTER TABLE assets ADD retirement_unit NVARCHAR(128) NULL;
END;

IF COL_LENGTH('assets', 'utility_account') IS NULL
BEGIN
    ALTER TABLE assets ADD utility_account NVARCHAR(128) NULL;
END;

IF COL_LENGTH('assets', 'property_group') IS NULL
BEGIN
    ALTER TABLE assets ADD property_group NVARCHAR(128) NULL;
END;

IF COL_LENGTH('assets', 'serial_number') IS NULL
BEGIN
    ALTER TABLE assets ADD serial_number NVARCHAR(128) NULL;
END;
