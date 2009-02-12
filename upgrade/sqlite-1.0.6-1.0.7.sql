-- Add columns for port range
ALTER TABLE glidein ADD COLUMN highport INTEGER;
ALTER TABLE glidein ADD COLUMN lowport INTEGER;