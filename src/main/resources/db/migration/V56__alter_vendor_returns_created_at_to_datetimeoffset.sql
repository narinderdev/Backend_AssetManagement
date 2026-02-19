-- Align vendor_returns.created_at with Hibernate's TIMESTAMP_UTC expectation (datetimeoffset(7))
IF EXISTS (
    SELECT 1
    FROM sys.default_constraints dc
    JOIN sys.columns c ON c.default_object_id = dc.object_id
    JOIN sys.objects t ON t.object_id = c.object_id
    WHERE t.name = 'vendor_returns' AND c.name = 'created_at'
)
BEGIN
    DECLARE @dropConstraint NVARCHAR(300);
    SELECT @dropConstraint = 'ALTER TABLE vendor_returns DROP CONSTRAINT ' + dc.name
    FROM sys.default_constraints dc
    JOIN sys.columns c ON c.default_object_id = dc.object_id
    JOIN sys.objects t ON t.object_id = c.object_id
    WHERE t.name = 'vendor_returns' AND c.name = 'created_at';
    EXEC (@dropConstraint);
END

ALTER TABLE vendor_returns
ALTER COLUMN created_at DATETIMEOFFSET(7) NOT NULL;

ALTER TABLE vendor_returns
ADD CONSTRAINT df_vendor_returns_created DEFAULT SYSDATETIMEOFFSET() FOR created_at;
