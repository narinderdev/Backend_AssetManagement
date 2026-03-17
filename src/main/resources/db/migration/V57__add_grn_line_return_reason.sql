IF OBJECT_ID('dbo.goods_receipt_note_lines', 'U') IS NOT NULL
   AND COL_LENGTH('dbo.goods_receipt_note_lines', 'return_reason') IS NULL
BEGIN
    ALTER TABLE dbo.goods_receipt_note_lines
    ADD return_reason NVARCHAR(500) NULL;
END;
