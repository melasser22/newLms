-- Helpful indexes and seed
create index if not exists ix_key_tenant_prefix on tenant_integration_key(tenant_id, key_prefix);

insert into tenant(tenant_slug, name)
values ('demo', 'Demo Tenant')
on conflict (tenant_slug) do nothing;
