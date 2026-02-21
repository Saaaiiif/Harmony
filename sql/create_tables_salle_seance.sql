-- ============================================================
-- Script SQL pour créer les tables Salle et Seance
-- À exécuter dans phpMyAdmin (onglet SQL, base "projet")
-- ============================================================

-- Table salle
CREATE TABLE IF NOT EXISTS salle (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nom VARCHAR(255) NOT NULL,
    capacite INT NOT NULL DEFAULT 20,
    equipements TEXT NULL,
    disponible BOOLEAN NOT NULL DEFAULT true,
    description TEXT NULL,
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table seance
CREATE TABLE IF NOT EXISTS seance (
    id INT AUTO_INCREMENT PRIMARY KEY,
    titre VARCHAR(255) NOT NULL,
    description TEXT NULL,
    date_debut TIMESTAMP NOT NULL,
    date_fin TIMESTAMP NOT NULL,
    salle_id INT NOT NULL,
    confirmee BOOLEAN NOT NULL DEFAULT false,
    type_seance VARCHAR(50) NOT NULL DEFAULT 'COURS',
    nombre_participants INT NOT NULL DEFAULT 0,
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_seance_salle
        FOREIGN KEY (salle_id) REFERENCES salle(id)
        ON DELETE CASCADE ON UPDATE CASCADE
);

-- Insérer quelques salles d'exemple
INSERT IGNORE INTO salle (nom, capacite, equipements, disponible, description) VALUES
('Salle A101', 30, 'Projecteur, Tableau blanc, Wi-Fi', true, 'Salle de cours standard'),
('Salle B205', 50, 'Projecteur, Tableau interactif, Wi-Fi, Micro', true, 'Grande salle pour conférences'),
('Salle C301', 20, 'Tableau blanc, Wi-Fi', true, 'Petite salle pour réunions'),
('Salle D102', 40, 'Projecteur, Tableau blanc, Wi-Fi, Système audio', false, 'Salle en maintenance');
