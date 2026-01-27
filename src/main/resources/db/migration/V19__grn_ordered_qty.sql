-- Add ordered_qty to GRN lines for reference
IF COL_LENGTH('goods_receipt_note_lines', 'ordered_qty') IS NULL
BEGIN
    ALTER TABLE goods_receipt_note_lines ADD ordered_qty DECIMAL(19,4) NULL;
END;
GO
