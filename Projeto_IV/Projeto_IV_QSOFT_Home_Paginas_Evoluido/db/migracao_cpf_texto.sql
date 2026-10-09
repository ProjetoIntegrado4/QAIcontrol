-- Converte a coluna CPF de bigint para texto (11 dígitos).
-- Necessário porque o ddl-auto=update do Hibernate não altera o tipo de colunas existentes.
-- CPFs antigos que perderam o zero à esquerda são completados com lpad.
-- Execute uma vez no banco iadb antes de subir a nova versão do backend.

ALTER TABLE admin
    ALTER COLUMN cpf TYPE varchar(11) USING lpad(cpf::text, 11, '0');

ALTER TABLE funcionario
    ALTER COLUMN cpf TYPE varchar(11) USING lpad(cpf::text, 11, '0');
