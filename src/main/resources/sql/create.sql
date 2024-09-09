CREATE TABLE IF NOT EXISTS `items` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT,
    `item` VARCHAR(255) NOT NULL,
    `amount` INT NOT NULL,
    `coordinates` VARCHAR(255) NOT NULL,
    `container` VARCHAR(255) NOT NULL,
    `UUID` VARCHAR(36) NOT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (`UUID`, `coordinates`, `container`, `item`)
);
