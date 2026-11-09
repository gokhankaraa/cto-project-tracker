-- V4: kimlik dogrulama icin kullanici parolasi.
-- Parolanin kendisi degil, BCrypt ozeti saklanir (60 karakter; algoritma degisirse diye 100).
-- Kolon bos gecilebilir: parolasi tanimlanmamis kullanici giris yapamaz. Demo profilinde
-- DataSeeder ornek kullanicilara demo parolasini atar; baska ortamlarda parola ayrica tanimlanir.

ALTER TABLE users ADD COLUMN password_hash VARCHAR(100);
