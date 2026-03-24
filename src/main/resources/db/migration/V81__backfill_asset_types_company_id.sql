-- Backfill asset_types.company_id where it can be inferred safely.

IF OBJECT_ID('dbo.asset_types', 'U') IS NOT NULL
   AND COL_LENGTH('asset_types', 'company_id') IS NOT NULL
BEGIN
    -- If an asset type is used only by assets from one company, assign that company.
    IF OBJECT_ID('dbo.assets', 'U') IS NOT NULL
       AND COL_LENGTH('assets', 'asset_type_id') IS NOT NULL
       AND COL_LENGTH('assets', 'company_id') IS NOT NULL
    BEGIN
        ;WITH single_company_usage AS (
            SELECT
                a.asset_type_id,
                MIN(a.company_id) AS company_id,
                COUNT(DISTINCT a.company_id) AS company_count
            FROM dbo.assets a
            WHERE a.asset_type_id IS NOT NULL
              AND a.company_id IS NOT NULL
            GROUP BY a.asset_type_id
        )
        UPDATE at
        SET at.company_id = scu.company_id
        FROM dbo.asset_types at
        JOIN single_company_usage scu ON scu.asset_type_id = at.id
        WHERE at.company_id IS NULL
          AND scu.company_count = 1;
    END;

    -- If there is exactly one active company, assign remaining NULL rows to it.
    IF OBJECT_ID('dbo.companies', 'U') IS NOT NULL
    BEGIN
        DECLARE @singleCompanyId BIGINT = NULL;
        DECLARE @activeCompanyCount INT = 0;

        SELECT
            @activeCompanyCount = COUNT(*),
            @singleCompanyId = MIN(id)
        FROM dbo.companies
        WHERE active = 1;

        IF @activeCompanyCount = 1 AND @singleCompanyId IS NOT NULL
        BEGIN
            UPDATE dbo.asset_types
            SET company_id = @singleCompanyId
            WHERE company_id IS NULL;
        END;
    END;
END;
