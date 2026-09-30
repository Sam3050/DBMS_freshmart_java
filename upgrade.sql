USE grocery;
ALTER TABLE customers
  ADD COLUMN password_hash CHAR(64) NOT NULL DEFAULT '',
  ADD COLUMN role VARCHAR(10) NOT NULL DEFAULT 'CUSTOMER';
ALTER TABLE products
  ADD COLUMN description VARCHAR(255) NOT NULL DEFAULT '',
  ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;
UPDATE products SET description = CONCAT('Fresh ', LOWER(title), ', quality checked before dispatch');

-- demo@example.com / demo123 (customer)    admin@freshcart.com / admin123 (admin)
UPDATE customers SET password_hash = SHA2('demo123',256) WHERE email='demo@example.com';
INSERT INTO customers(full_name,email,phone,address,password_hash,role)
VALUES ('Store Admin','admin@freshcart.com','9000000000','Warehouse, Pune',SHA2('admin123',256),'ADMIN');

CREATE OR REPLACE VIEW v_catalog AS
SELECT p.product_id, p.category_id, p.sku, p.title, p.emoji, p.unit_type, p.price_per_unit,
       p.description, p.is_active,
       COALESCE(SUM(b.stock_quantity),0) AS stock,
       MIN(b.expiry_date) AS next_expiry,
       COALESCE(MIN(b.expiry_date) <= DATE_ADD(CURDATE(), INTERVAL 2 DAY),0) AS expiring_soon,
       fn_price(p.price_per_unit, COALESCE(MIN(b.expiry_date), DATE_ADD(CURDATE(), INTERVAL 30 DAY))) AS effective_price
FROM products p
LEFT JOIN product_batches b ON b.product_id=p.product_id AND b.expiry_date>CURDATE() AND b.stock_quantity>0
GROUP BY p.product_id;