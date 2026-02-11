-- Increase technicians.phone_number length to 20
IF COL_LENGTH('technicians', 'phone_number') IS NOT NULL
BEGIN
    ALTER TABLE technicians ALTER COLUMN phone_number NVARCHAR(20) NULL;
END
