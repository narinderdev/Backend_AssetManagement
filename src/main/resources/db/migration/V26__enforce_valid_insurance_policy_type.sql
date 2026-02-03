-- Clean up and enforce valid insurance policy types

-- 1) Null-out any existing invalid enum values to unblock reads
UPDATE asset_insurance
SET policy_type = NULL
WHERE policy_type IS NOT NULL
  AND policy_type NOT IN (
      'LIABILITY',
      'PROPERTY',
      'COMPREHENSIVE',
      'FIRE',
      'EQUIPMENT_BREAKDOWN',
      'WELL_CONTROL',
      'ENVIRONMENTAL_POLLUTION',
      'PIPELINE',
      'OFFSHORE_MARINE',
      'CARGO',
      'BUSINESS_INTERRUPTION'
  );
GO

-- 2) Add a check constraint to prevent future invalid values
IF NOT EXISTS (
    SELECT 1
    FROM sys.check_constraints
    WHERE name = 'chk_asset_insurance_policy_type_valid'
      AND parent_object_id = OBJECT_ID('asset_insurance')
)
BEGIN
    ALTER TABLE asset_insurance
    ADD CONSTRAINT chk_asset_insurance_policy_type_valid CHECK (
        policy_type IS NULL OR policy_type IN (
            'LIABILITY',
            'PROPERTY',
            'COMPREHENSIVE',
            'FIRE',
            'EQUIPMENT_BREAKDOWN',
            'WELL_CONTROL',
            'ENVIRONMENTAL_POLLUTION',
            'PIPELINE',
            'OFFSHORE_MARINE',
            'CARGO',
            'BUSINESS_INTERRUPTION'
        )
    );
END;
GO
