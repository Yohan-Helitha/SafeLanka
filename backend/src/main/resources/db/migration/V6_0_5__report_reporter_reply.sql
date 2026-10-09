-- Owner: reports module (UC02). The reporter's answer to an officer's "more information" question.
ALTER TABLE hazard_reports
    ADD COLUMN reporter_reply VARCHAR(300),
    ADD COLUMN replied_at     TIMESTAMPTZ;
