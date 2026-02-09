-- Preferred assignment selections for service requests
ALTER TABLE service_requests
ADD preferred_technician_id BIGINT NULL,
    preferred_team_id BIGINT NULL;
