CREATE TABLE comments (
    comment_id BIGSERIAL PRIMARY KEY,
    article_id BIGINT NOT NULL REFERENCES articles(article_id) ON DELETE CASCADE,
    author_id BIGINT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    body TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC')
);

CREATE INDEX IF NOT EXISTS idx_comments_article_id ON comments (article_id);
CREATE INDEX IF NOT EXISTS idx_comments_author_id ON comments (author_id);
