-- Enable soft delete for technicians
ALTER TABLE technicians
ADD is_deleted BIT NOT NULL CONSTRAINT DF_technicians_is_deleted DEFAULT 0 WITH VALUES;

CREATE INDEX idx_technicians_is_deleted ON technicians (is_deleted);
