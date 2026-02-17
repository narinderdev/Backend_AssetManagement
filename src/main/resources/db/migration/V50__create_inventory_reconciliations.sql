CREATE TABLE inventory_reconciliations (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    warehouse_id BIGINT NOT NULL,
    inventory_item_id BIGINT NOT NULL,
    reconcile_date DATE NOT NULL,
    entered_by NVARCHAR(150) NULL,
    system_quantity INT NOT NULL,
    physical_quantity INT NOT NULL,
    variance_quantity INT NOT NULL,
    cost_per_unit_snapshot DECIMAL(19,2) NULL,
    variance_cost DECIMAL(19,2) NULL,
    status NVARCHAR(16) NOT NULL,
    reason NVARCHAR(500) NULL,
    created_at DATETIME2 NOT NULL CONSTRAINT df_inventory_reconciliations_created DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NOT NULL CONSTRAINT df_inventory_reconciliations_updated DEFAULT SYSUTCDATETIME()
);

CREATE INDEX idx_invrec_warehouse ON inventory_reconciliations(warehouse_id);
CREATE INDEX idx_invrec_item ON inventory_reconciliations(inventory_item_id);
CREATE INDEX idx_invrec_status ON inventory_reconciliations(status);
CREATE INDEX idx_invrec_date ON inventory_reconciliations(reconcile_date);

ALTER TABLE inventory_reconciliations ADD CONSTRAINT fk_invrec_wh FOREIGN KEY (warehouse_id) REFERENCES warehouses(id);
ALTER TABLE inventory_reconciliations ADD CONSTRAINT fk_invrec_item FOREIGN KEY (inventory_item_id) REFERENCES inventory_items(id);
