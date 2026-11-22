-- Mesmas tabelas de database/initDatabase.sql, adaptadas para o H2 embutido.
-- Pode rodar mais de uma vez: não recria tabelas nem duplica linhas.

CREATE TABLE IF NOT EXISTS base_price (
    pass_id INT AUTO_INCREMENT,
    type VARCHAR(255) NOT NULL,
    cost INT NOT NULL,
    PRIMARY KEY (pass_id),
    UNIQUE (type)
);

MERGE INTO base_price (type, cost) KEY (type) VALUES ('1jour', 35);
MERGE INTO base_price (type, cost) KEY (type) VALUES ('night', 19);

CREATE TABLE IF NOT EXISTS holidays (
    holiday DATE NOT NULL,
    description VARCHAR(255) NOT NULL,
    PRIMARY KEY (holiday)
);

MERGE INTO holidays (holiday, description) KEY (holiday) VALUES ('2019-02-18', 'winter');
MERGE INTO holidays (holiday, description) KEY (holiday) VALUES ('2019-02-25', 'winter');
MERGE INTO holidays (holiday, description) KEY (holiday) VALUES ('2019-03-04', 'winter');
