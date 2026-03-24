-- Add termination_date for contract technicians
IF OBJECT_ID('technicians', 'U') IS NOT NULL AND COL_LENGTH('technicians', 'termination_date') IS NULL
BEGIN
    ALTER TABLE technicians ADD termination_date DATE NULL;
END;
GO

