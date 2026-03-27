create table if not exists project_reference_pattern (
    id varchar(36) primary key,
    artifact_type varchar(64) not null,
    reference_name varchar(255) not null,
    content text not null,
    created_at timestamp not null
);

create index if not exists idx_project_reference_pattern_type_created_at
    on project_reference_pattern (artifact_type, created_at desc);
