create table charges (
    id uuid not null,
    amount_centavos bigint not null,
    status varchar(255) not null check (status in ('ACTIVE','PAID','EXPIRED','CANCELLED')),
    created_at timestamp(6) with time zone not null,
    expires_at timestamp(6) with time zone not null,
    primary key (id)
);

create table idempotency_records (
    id uuid not null,
    idempotency_key varchar(255) not null,
    request_hash varchar(255) not null,
    charge_id uuid not null,
    response_body jsonb not null,
    created_at timestamp(6) with time zone not null,
    primary key (id),
    constraint uk_idempotency_records_key unique (idempotency_key)
);

create table outbox_events (
    id uuid not null,
    aggregate_type varchar(255) not null,
    aggregate_id varchar(255) not null,
    event_type varchar(255) not null,
    payload jsonb not null,
    status varchar(255) not null check (status in ('PENDING','PUBLISHED')),
    created_at timestamp(6) with time zone not null,
    published_at timestamp(6) with time zone,
    primary key (id)
);

create index idx_outbox_status_created_at on outbox_events (status, created_at);

create table webhook_events (
    id uuid not null,
    event_id varchar(255) not null,
    charge_id uuid not null,
    created_at timestamp(6) with time zone not null,
    primary key (id),
    constraint uk_webhook_events_event_id unique (event_id)
);
