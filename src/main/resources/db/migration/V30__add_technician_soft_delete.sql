-- Enable soft delete for technicians
IF OBJECT_ID('dbo.technicians', 'U') IS NOT NULL
   AND COL_LENGTH('dbo.technicians', 'is_deleted') IS NULL
BEGIN
    ALTER TABLE dbo.technicians
    ADD is_deleted BIT NOT NULL CONSTRAINT DF_technicians_is_deleted DEFAULT 0 WITH VALUES;
END;

IF OBJECT_ID('dbo.technicians', 'U') IS NOT NULL
   AND COL_LENGTH('dbo.technicians', 'is_deleted') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_technicians_is_deleted' AND object_id = OBJECT_ID('dbo.technicians'))
BEGIN
    CREATE INDEX idx_technicians_is_deleted ON dbo.technicians (is_deleted);
END;
