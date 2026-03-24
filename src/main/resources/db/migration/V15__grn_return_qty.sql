-- Add return_qty to GRN lines for tracking vendor returns (stock unaffected)
IF OBJECT_ID('goods_receipt_note_lines', 'U') IS NOT NULL AND COL_LENGTH('goods_receipt_note_lines', 'return_qty') IS NULL
BEGIN
    ALTER TABLE goods_receipt_note_lines ADD return_qty DECIMAL(19,4) NULL;
END;
GO

