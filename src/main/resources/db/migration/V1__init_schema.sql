create table roles (
    id   bigserial primary key,
    name varchar(50) not null unique
);

create table users (
    id         bigserial primary key,
    full_name  varchar(120) not null,
    email      varchar(120) not null unique,
    password   varchar(255) not null,
    enabled    boolean      not null default true,
    role_id    bigint       not null references roles (id),
    created_at timestamp    not null default current_timestamp,
    updated_at timestamp    not null default current_timestamp
);

create table notification_templates (
    id         bigserial primary key,
    name       varchar(100) not null unique,
    subject    varchar(255),
    body       text         not null,
    channel    varchar(30)  not null,
    active     boolean      not null default true,
    created_at timestamp    not null default current_timestamp,
    updated_at timestamp    not null default current_timestamp
);

create table notifications (
    id          bigserial primary key,
    recipient   varchar(150) not null,
    subject     varchar(255),
    content     text         not null,
    channel     varchar(30)  not null,
    status      varchar(30)  not null,
    template_id bigint references notification_templates (id),
    created_by  bigint references users (id),
    created_at  timestamp    not null default current_timestamp,
    updated_at  timestamp    not null default current_timestamp,
    sent_at     timestamp
);