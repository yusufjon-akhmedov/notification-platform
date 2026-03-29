create table notification_delivery_attempts (
    id                bigserial primary key,
    notification_id   bigint       not null references notifications (id),
    attempt_number    integer      not null,
    channel           varchar(30)  not null,
    status            varchar(30)  not null,
    error_message     text,
    attempted_at      timestamp    not null default current_timestamp,
    provider_response text,
    created_at        timestamp    not null default current_timestamp,
    updated_at        timestamp    not null default current_timestamp,
    constraint uk_notification_delivery_attempt unique (notification_id, attempt_number)
);

create index idx_notification_delivery_attempts_notification_id
    on notification_delivery_attempts (notification_id);
