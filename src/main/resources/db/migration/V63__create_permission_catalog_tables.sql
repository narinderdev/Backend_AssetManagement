CREATE TABLE permission_classes (
    id DECIMAL(19,0) NOT NULL,
    name VARCHAR(120) NOT NULL,
    sort_order DECIMAL(10,0) NOT NULL,
    active SMALLINT DEFAULT 1 NOT NULL,
    CONSTRAINT pk_permission_classes PRIMARY KEY (id),
    CONSTRAINT uk_permission_classes_name UNIQUE (name)
);

CREATE TABLE permission_objects (
    id DECIMAL(19,0) NOT NULL,
    class_id DECIMAL(19,0) NOT NULL,
    name VARCHAR(160) NOT NULL,
    module VARCHAR(64) NOT NULL,
    view_only SMALLINT DEFAULT 0 NOT NULL,
    sort_order DECIMAL(10,0) NOT NULL,
    active SMALLINT DEFAULT 1 NOT NULL,
    CONSTRAINT pk_permission_objects PRIMARY KEY (id),
    CONSTRAINT fk_permission_objects_class FOREIGN KEY (class_id) REFERENCES permission_classes(id) ON DELETE CASCADE,
    CONSTRAINT uk_permission_objects_class_name UNIQUE (class_id, name)
);

CREATE INDEX idx_permission_objects_class_sort ON permission_objects(class_id, sort_order);
CREATE INDEX idx_permission_objects_module ON permission_objects(module);

INSERT INTO permission_classes (id, name, sort_order, active) VALUES (1, 'Dashboard', 1, 1);
INSERT INTO permission_classes (id, name, sort_order, active) VALUES (2, 'Asset', 2, 1);
INSERT INTO permission_classes (id, name, sort_order, active) VALUES (3, 'Work Order', 3, 1);
INSERT INTO permission_classes (id, name, sort_order, active) VALUES (4, 'Maintenance', 4, 1);
INSERT INTO permission_classes (id, name, sort_order, active) VALUES (5, 'Inventory', 5, 1);
INSERT INTO permission_classes (id, name, sort_order, active) VALUES (6, 'Procurement', 6, 1);
INSERT INTO permission_classes (id, name, sort_order, active) VALUES (7, 'Technician', 7, 1);
INSERT INTO permission_classes (id, name, sort_order, active) VALUES (8, 'Reports', 8, 1);
INSERT INTO permission_classes (id, name, sort_order, active) VALUES (9, 'Security', 9, 1);

INSERT INTO permission_objects (id, class_id, name, module, view_only, sort_order, active) VALUES (1, 1, 'Maintenance Dashboard', 'DASHBOARD', 1, 1, 1);
INSERT INTO permission_objects (id, class_id, name, module, view_only, sort_order, active) VALUES (2, 1, 'Security Dashboard', 'DASHBOARD', 1, 2, 1);
INSERT INTO permission_objects (id, class_id, name, module, view_only, sort_order, active) VALUES (3, 1, 'Budget Dashboard', 'DASHBOARD', 1, 3, 1);
INSERT INTO permission_objects (id, class_id, name, module, view_only, sort_order, active) VALUES (4, 2, 'Asset Type', 'ASSET_TYPE', 0, 1, 1);
INSERT INTO permission_objects (id, class_id, name, module, view_only, sort_order, active) VALUES (5, 2, 'Asset', 'ASSET', 0, 2, 1);
INSERT INTO permission_objects (id, class_id, name, module, view_only, sort_order, active) VALUES (6, 3, 'Work Order Type', 'WORK_ORDER_TYPE', 0, 1, 1);
INSERT INTO permission_objects (id, class_id, name, module, view_only, sort_order, active) VALUES (7, 3, 'Work Order', 'WORK_ORDER', 0, 2, 1);
INSERT INTO permission_objects (id, class_id, name, module, view_only, sort_order, active) VALUES (8, 4, 'Preventive Maintenance', 'PREVENTIVE_MAINTENANCE', 0, 1, 1);
INSERT INTO permission_objects (id, class_id, name, module, view_only, sort_order, active) VALUES (9, 4, 'Corrective Maintenance', 'CORRECTIVE_MAINTENANCE', 0, 2, 1);
INSERT INTO permission_objects (id, class_id, name, module, view_only, sort_order, active) VALUES (10, 5, 'Warehouse', 'INVENTORY', 0, 1, 1);
INSERT INTO permission_objects (id, class_id, name, module, view_only, sort_order, active) VALUES (11, 5, 'Inventory', 'INVENTORY', 0, 2, 1);
INSERT INTO permission_objects (id, class_id, name, module, view_only, sort_order, active) VALUES (12, 5, 'Inventory Reconcile', 'INVENTORY', 0, 3, 1);
INSERT INTO permission_objects (id, class_id, name, module, view_only, sort_order, active) VALUES (13, 5, 'Inventory Audit Logs', 'INVENTORY', 0, 4, 1);
INSERT INTO permission_objects (id, class_id, name, module, view_only, sort_order, active) VALUES (14, 6, 'Material Requisition', 'MATERIAL_REQUISITION', 0, 1, 1);
INSERT INTO permission_objects (id, class_id, name, module, view_only, sort_order, active) VALUES (15, 6, 'Purchase Order', 'PURCHASE_ORDER', 0, 2, 1);
INSERT INTO permission_objects (id, class_id, name, module, view_only, sort_order, active) VALUES (16, 6, 'Goods Receipt Notes', 'GOODS_RECEIPT_NOTE', 0, 3, 1);
INSERT INTO permission_objects (id, class_id, name, module, view_only, sort_order, active) VALUES (17, 7, 'Technician', 'TECHNICIAN', 0, 1, 1);
INSERT INTO permission_objects (id, class_id, name, module, view_only, sort_order, active) VALUES (18, 7, 'Technician Team', 'TECHNICIAN_TEAM', 0, 2, 1);
INSERT INTO permission_objects (id, class_id, name, module, view_only, sort_order, active) VALUES (19, 8, 'Inventory Report', 'REPORTS', 1, 1, 1);
INSERT INTO permission_objects (id, class_id, name, module, view_only, sort_order, active) VALUES (20, 8, 'Asset Report', 'REPORTS', 1, 2, 1);
INSERT INTO permission_objects (id, class_id, name, module, view_only, sort_order, active) VALUES (21, 8, 'Transaction Report', 'REPORTS', 1, 3, 1);
INSERT INTO permission_objects (id, class_id, name, module, view_only, sort_order, active) VALUES (22, 8, 'Work Order Report', 'REPORTS', 1, 4, 1);
INSERT INTO permission_objects (id, class_id, name, module, view_only, sort_order, active) VALUES (23, 9, 'Roles', 'MANAGE_ROLES', 0, 1, 1);
INSERT INTO permission_objects (id, class_id, name, module, view_only, sort_order, active) VALUES (24, 9, 'User', 'MANAGE_USERS', 0, 2, 1);
INSERT INTO permission_objects (id, class_id, name, module, view_only, sort_order, active) VALUES (25, 9, 'MFA', 'MANAGE_USERS', 0, 3, 1);
INSERT INTO permission_objects (id, class_id, name, module, view_only, sort_order, active) VALUES (26, 9, 'Security Report', 'REPORTS', 1, 4, 1);
