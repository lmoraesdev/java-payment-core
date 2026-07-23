alter table outbox_events drop constraint outbox_events_status_check;

alter table outbox_events
    add constraint outbox_events_status_check
    check (status in ('PENDING','IN_FLIGHT','PUBLISHED'));
