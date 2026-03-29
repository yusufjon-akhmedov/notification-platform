alter table roles
    add column created_at timestamp not null default current_timestamp;

alter table roles
    add column updated_at timestamp not null default current_timestamp;