-- Add manufacturing metadata to assets

IF COL_LENGTH('assets', 'model_number') IS NULL
BEGIN
    ALTER TABLE assets ADD model_number NVARCHAR(128) NULL;
END;

IF COL_LENGTH('assets', 'manufacture_date') IS NULL
BEGIN
    ALTER TABLE assets ADD manufacture_date DATE NULL;
END;
