/*
  # Create products table and security policies

  1. New Tables
    - `products`
      - `id` (uuid, primary key)
      - `name` (text, required)
      - `description` (text)
      - `price` (decimal, required)
      - `image_url` (text)
      - `stock` (integer, default: 0)
      - `created_at` (timestamp with timezone)
      - `updated_at` (timestamp with timezone)

  2. Security
    - Enable RLS on products table
    - Add policies for:
      - Public read access to all products
      - Authenticated admin users can manage products
*/

CREATE TABLE products (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  name text NOT NULL,
  description text,
  price decimal(10,2) NOT NULL,
  image_url text,
  stock integer DEFAULT 0,
  created_at timestamptz DEFAULT now(),
  updated_at timestamptz DEFAULT now()
);

ALTER TABLE products ENABLE ROW LEVEL SECURITY;

-- Allow public read access to products
CREATE POLICY "Products are viewable by everyone"
  ON products
  FOR SELECT
  TO public
  USING (true);

-- Allow admin users to manage products
CREATE POLICY "Admin users can manage products"
  ON products
  USING (auth.uid() IN (
    SELECT user_id FROM admin_users
  ));