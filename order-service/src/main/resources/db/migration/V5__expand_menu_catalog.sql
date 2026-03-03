ALTER TABLE products
    ADD COLUMN IF NOT EXISTS price NUMERIC(10,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS category VARCHAR(50) NOT NULL DEFAULT 'otros',
    ADD COLUMN IF NOT EXISTS image_url TEXT;

UPDATE products
SET price = 450,
    category = 'entradas',
    image_url = 'https://images.unsplash.com/photo-1603360946369-dc9bb6258143?w=400&h=300&fit=crop'
WHERE id = 1 OR name = 'Pizza Margherita';

UPDATE products
SET name = 'Provoleta grillada',
    description = 'Queso provolone con oregano y oliva.',
    price = 520,
    category = 'entradas',
    image_url = 'https://www.clarin.com/2022/08/31/SvRumKBuh_2000x1500__1.jpg'
WHERE id = 2 OR name = 'Hamburguesa Clásica';

UPDATE products
SET name = 'Bife de chorizo',
    description = 'Corte premium con papas rusticas.',
    price = 1850,
    category = 'principales',
    image_url = 'https://images.unsplash.com/photo-1558030006-450675393462?w=400&h=300&fit=crop'
WHERE id = 3 OR name = 'Ensalada César';

INSERT INTO products (name, description, price, category, image_url, is_active)
SELECT 'Ceviche de pescado', 'Pescado fresco marinado con limon y cilantro.', 680, 'entradas',
       'https://images.unsplash.com/photo-1625944230945-1b7dd3b949ab?w=400&h=300&fit=crop', true
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = 'Ceviche de pescado');

INSERT INTO products (name, description, price, category, image_url, is_active)
SELECT 'Tabla de fiambres', 'Seleccion de quesos y embutidos artesanales.', 890, 'entradas',
       'https://images.unsplash.com/photo-1541013406133-94ed77ee8ba8?w=400&h=300&fit=crop', true
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = 'Tabla de fiambres');

INSERT INTO products (name, description, price, category, image_url, is_active)
SELECT 'Milanesa napolitana', 'Milanesa con salsa pomodoro y queso.', 1420, 'principales',
       'https://images.unsplash.com/photo-1565299624946-b28f40a0ae38?w=400&h=300&fit=crop', true
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = 'Milanesa napolitana');

INSERT INTO products (name, description, price, category, image_url, is_active)
SELECT 'Salmon a la plancha', 'Filete de salmon con vegetales asados y arroz.', 1650, 'principales',
       'https://images.unsplash.com/photo-1467003909585-2f8a72700288?w=400&h=300&fit=crop', true
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = 'Salmon a la plancha');

INSERT INTO products (name, description, price, category, image_url, is_active)
SELECT 'Pasta carbonara', 'Fettuccini con panceta, crema y parmesano.', 1180, 'principales',
       'https://www.laragazzacolmattarello.com/wp-content/uploads/2025/01/pasta-a-la-carbonara.jpg', true
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = 'Pasta carbonara');

INSERT INTO products (name, description, price, category, image_url, is_active)
SELECT 'Tacos de pollo', 'Tres tacos con pollo asado, guacamole y pico de gallo.', 980, 'principales',
       'https://images.unsplash.com/photo-1613514785940-daed07799d9b?w=400&h=300&fit=crop', true
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = 'Tacos de pollo');

INSERT INTO products (name, description, price, category, image_url, is_active)
SELECT 'Locro tradicional', 'Guiso de maiz, porotos y carne de cerdo.', 1250, 'principales',
       'https://images.unsplash.com/photo-1547592166-23ac45744acd?w=400&h=300&fit=crop', true
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = 'Locro tradicional');

INSERT INTO products (name, description, price, category, image_url, is_active)
SELECT 'Flan con dulce de leche', 'Flan casero con caramelo y dulce de leche.', 520, 'postres',
       'https://images.unsplash.com/photo-1621303837174-89787a7d4729?w=400&h=300&fit=crop', true
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = 'Flan con dulce de leche');

INSERT INTO products (name, description, price, category, image_url, is_active)
SELECT 'Tiramisu', 'Postre italiano con cafe y cacao.', 560, 'postres',
       'https://images.unsplash.com/photo-1571877227200-a0d98ea607e9?w=400&h=300&fit=crop', true
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = 'Tiramisu');

INSERT INTO products (name, description, price, category, image_url, is_active)
SELECT 'Volcan de chocolate', 'Bizcocho tibio con centro fundido.', 480, 'postres',
       'https://images.unsplash.com/photo-1624353365286-3f8d62daad51?w=400&h=300&fit=crop', true
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = 'Volcan de chocolate');

INSERT INTO products (name, description, price, category, image_url, is_active)
SELECT 'Limonada de la casa', 'Limon, menta y almibar ligero.', 280, 'bebidas',
       'https://images.unsplash.com/photo-1523677011781-c91d1bbe2f9e?w=400&h=300&fit=crop', true
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = 'Limonada de la casa');

INSERT INTO products (name, description, price, category, image_url, is_active)
SELECT 'Jugo de maracuya', 'Jugo natural de maracuya con hielo.', 320, 'bebidas',
       'https://images.unsplash.com/photo-1622597467836-f3285f2131b8?w=400&h=300&fit=crop', true
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = 'Jugo de maracuya');

INSERT INTO products (name, description, price, category, image_url, is_active)
SELECT 'Limonada de coco', 'Bebida fria cremosa con limon y coco.', 360, 'bebidas',
       'https://images.unsplash.com/photo-1497534446932-c925b458314e?w=400&h=300&fit=crop', true
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = 'Limonada de coco');
