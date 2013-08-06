--the document table is here for testing purposes
create table document(
       id serial primary key,
       path text not null
);
grant all on document to postgres;
grant usage, select on sequence document_id_seq to postgres;

create table task(
       id serial primary key,
       workflow text not null,
       state text not null,
       assigned_to text not null,
       document_type_id integer not null,
       document_id integer not null,
       complete boolean not null default false,
       last_updated timestamp not null default now()
);
grant all on task to postgres;
grant usage, select on sequence task_id_seq to postgres;

create function update_timestamp() returns trigger as $update_timestamp$
       begin
        NEW.last_updated := current_timestamp;
        return NEW;
       end;
$update_timestamp$ language plpgsql;

create trigger task_update after update on task for each row execute procedure update_timestamp();

create table task_audit(
       id serial primary key,
       task_id integer not null references task(id),
       initial_state text not null,
       transition text not null,
       new_state text not null,
       executed_by text not null,
       assigned_to text not null,
       created_at timestamp not null default now()
);
grant all on task_audit to postgres;
grant usage, select on sequence task_audit_id_seq to postgres;
