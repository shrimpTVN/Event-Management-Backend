ALTER TABLE events
ADD COLUMN allow_self_check boolean not null default false;