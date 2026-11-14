-- vector search
CREATE EXTENSION IF NOT EXISTS vector;

-- full text search (built-in, but garantindo o dicionário pt)
CREATE TEXT SEARCH CONFIGURATION IF NOT EXISTS portuguese (COPY = pg_catalog.portuguese);
