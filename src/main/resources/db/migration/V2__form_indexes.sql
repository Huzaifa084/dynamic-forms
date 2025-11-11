-- B-tree indexes
CREATE INDEX IF NOT EXISTS idx_form_definitions_company
    ON form_definitions (company_id);

CREATE INDEX IF NOT EXISTS idx_form_submissions_form_company
    ON form_submissions (form_definition_id, company_id);

-- GIN on JSONB
CREATE INDEX IF NOT EXISTS idx_form_submissions_data
    ON form_submissions USING GIN (data);

-- Full-text search (null-safe)
CREATE INDEX IF NOT EXISTS idx_form_submissions_search
    ON form_submissions USING GIN (to_tsvector('english', coalesce(searchable_text, '')));
