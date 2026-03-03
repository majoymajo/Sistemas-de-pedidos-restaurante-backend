CREATE TABLE product (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(120) NOT NULL,
  description TEXT,
  is_active BOOLEAN NOT NULL DEFAULT true
);

CREATE TABLE orders (
  id UUID PRIMARY KEY,
  table_id INTEGER NOT NULL,
  status VARCHAR(20) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT now(),
  updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE order_item (
  id BIGSERIAL PRIMARY KEY,
  order_id UUID NOT NULL,
  product_id BIGINT NOT NULL,
  quantity INTEGER NOT NULL,
  note TEXT
);
