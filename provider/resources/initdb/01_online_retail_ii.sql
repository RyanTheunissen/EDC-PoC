-- Initialize sample Online Retail II dataset (simplified schema)
-- Source: https://archive.ics.uci.edu/dataset/502/online+retail+ii
-- We provide a minimal subset for demonstration.

CREATE TABLE IF NOT EXISTS online_retail_ii (
    invoice_no      TEXT,
    stock_code      TEXT,
    description     TEXT,
    quantity        INTEGER,
    invoice_date    TIMESTAMP,
    price           NUMERIC(10,2),
    customer_id     INTEGER,
    country         TEXT
);

-- Sample rows (subset)
INSERT INTO online_retail_ii(invoice_no, stock_code, description, quantity, invoice_date, price, customer_id, country) VALUES
('489434', '85123A', 'WHITE HANGING HEART T-LIGHT HOLDER', 6, '2009-12-01 08:26:00', 2.55, 13047, 'United Kingdom'),
('489434', '71053', 'WHITE METAL LANTERN', 6, '2009-12-01 08:26:00', 3.39, 13047, 'United Kingdom'),
('489444', '84406B', 'CREAM CUPID HEARTS COAT HANGER', 8, '2009-12-01 08:28:00', 2.75, 13047, 'United Kingdom'),
('489446', '84029G', 'KNITTED UNION FLAG HOT WATER BOTTLE', 6, '2009-12-01 08:34:00', 3.39, 12583, 'France'),
('489450', '20725', 'LUNCH BAG RED RETROSPOT', 6, '2009-12-01 08:35:00', 1.65, 17850, 'United Kingdom'),
('489450', '22633', 'HAND WARMER UNION JACK', 6, '2009-12-01 08:35:00', 1.85, 17850, 'United Kingdom'),
('489450', '22632', 'HAND WARMER RED POLKA DOT', 6, '2009-12-01 08:35:00', 1.85, 17850, 'United Kingdom'),
('489451', '21730', 'GLASS STAR FROSTED T-LIGHT HOLDER', 6, '2009-12-01 08:45:00', 4.25, 17850, 'United Kingdom'),
('489453', '84879', 'ASSORTED COLOUR BIRD ORNAMENT', 32, '2009-12-01 08:46:00', 1.69, 13047, 'United Kingdom'),
('489455', '22752', 'SET 7 BABUSHKA NESTING BOXES', 2, '2009-12-01 08:51:00', 7.65, 12583, 'France')
ON CONFLICT DO NOTHING;