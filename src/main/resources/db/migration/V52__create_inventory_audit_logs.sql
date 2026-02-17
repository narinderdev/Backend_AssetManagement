CREATE TABLE inventory_audit_logs (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    inventory_item_id BIGINT NOT NULL,
    transaction_type NVARCHAR(16) NOT NULL,
    reference_type NVARCHAR(32) NOT NULL,
    reference_number NVARCHAR(128) NULL,
    before_quantity INT NOT NULL,
    after_quantity INT NOT NULL,
    variance_quantity INT NOT NULL,
    performed_by NVARCHAR(150) NULL,
    reason NVARCHAR(500) NULL,
    created_at DATETIME2 NOT NULL CONSTRAINT df_inventory_audit_logs_created DEFAULT SYSUTCDATETIME()
);

CREATE INDEX idx_inv_audit_item ON inventory_audit_logs(inventory_item_id);
CREATE INDEX idx_inv_audit_txn_type ON inventory_audit_logs(transaction_type);
CREATE INDEX idx_inv_audit_ref_type ON inventory_audit_logs(reference_type);
CREATE INDEX idx_inv_audit_created ON inventory_audit_logs(created_at);

ALTER TABLE inventory_audit_logs
ADD CONSTRAINT fk_inv_audit_item FOREIGN KEY (inventory_item_id) REFERENCES inventory_items(id);
