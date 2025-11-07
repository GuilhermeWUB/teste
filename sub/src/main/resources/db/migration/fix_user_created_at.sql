-- Corrige valores NULL na coluna created_at da tabela app_users
-- Isso resolve o erro de TimeStamp que ocorre quando a página de usuários é acessada
-- Define a data de criação como NOW() para usuários que não têm esse valor

UPDATE app_users
SET created_at = NOW()
WHERE created_at IS NULL;
