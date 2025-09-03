-- Create Database
CREATE DATABASE IF NOT EXISTS librarydb;

-- Use Database
USE librarydb;

-- Create Books Table
CREATE TABLE IF NOT EXISTS books (
    id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(100),
    author VARCHAR(100),
    available BOOLEAN DEFAULT TRUE
);

-- Create Users Table (for login system if you add it later)
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE,
    password VARCHAR(100)
);

-- Insert sample data
INSERT INTO books (title, author, available) VALUES
('The Alchemist', 'Paulo Coelho', TRUE),
('Wings of Fire', 'A.P.J. Abdul Kalam', TRUE),
('Rich Dad Poor Dad', 'Robert Kiyosaki', TRUE);
