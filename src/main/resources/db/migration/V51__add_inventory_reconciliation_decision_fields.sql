IF COL_LENGTH('inventory_reconciliations', 'approved_by') IS NULL
BEGIN
    ALTER TABLE inventory_reconciliations ADD approved_by NVARCHAR(150) NULL;
END;

IF COL_LENGTH('inventory_reconciliations', 'approved_at') IS NULL
BEGIN
    ALTER TABLE inventory_reconciliations ADD approved_at DATETIME2 NULL;
END;

IF COL_LENGTH('inventory_reconciliations', 'approval_comment') IS NULL
BEGIN
    ALTER TABLE inventory_reconciliations ADD approval_comment NVARCHAR(500) NULL;
END;

IF COL_LENGTH('inventory_reconciliations', 'rejected_by') IS NULL
BEGIN
    ALTER TABLE inventory_reconciliations ADD rejected_by NVARCHAR(150) NULL;
END;

IF COL_LENGTH('inventory_reconciliations', 'rejected_at') IS NULL
BEGIN
    ALTER TABLE inventory_reconciliations ADD rejected_at DATETIME2 NULL;
END;

IF COL_LENGTH('inventory_reconciliations', 'rejection_comment') IS NULL
BEGIN
    ALTER TABLE inventory_reconciliations ADD rejection_comment NVARCHAR(500) NULL;
END;
