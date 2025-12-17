-- Users
INSERT INTO users (first_name, last_name, email, password, role) VALUES
('John', 'Doe', 'john@example.com', '$2a$10$hashedpassword1', 'CUSTOMER,VENDOR'),
('Alice', 'Smith', 'alice@example.com', '$2a$10$hashedpassword2', 'CUSTOMER'),
('Vendor', 'One', 'vendor1@example.com', '$2a$10$hashedpassword3', 'CUSTOMER,VENDOR');

-- Products
INSERT INTO products (name, price, original_price, description, category, image, rating, review_count, in_stock, features, vendor_id) VALUES
('Smartphone X10', 699.99, 899.99, 'Latest smartphone with high-end features', 'Electronics', 'image1.jpg', 4.5, 120, TRUE, '5G,128GB Storage,6GB RAM', 3),
('Gaming Laptop Z', 1499.99, 1799.99, 'Powerful gaming laptop with RTX GPU', 'Computers', 'image2.jpg', 4.8, 85, TRUE, '16GB RAM,RTX 3060,1TB SSD', 3),
                                                                                                                                          ('Wireless Earbuds Pro', 129.99, 159.99, 'Noise-cancelling wireless earbuds', 'Accessories', 'image3.jpg', 4.3, 200, TRUE, 'Bluetooth 5.0,Noise Cancellation,Water Resistant', 3);

-- Cart
INSERT INTO cart (user_id, product_id, quantity) VALUES
(1, 1, 1),
(1, 2, 2);

-- Orders
INSERT INTO orders (user_id, total_amount, status) VALUES
(1, 1899.98, 'PENDING');

-- Order Items
INSERT INTO order_items (order_id, product_id, quantity) VALUES
(1, 1, 1),
(1, 2, 2);
