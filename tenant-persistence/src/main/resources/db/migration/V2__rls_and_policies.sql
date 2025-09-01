-- Enable RLS on tenant-scoped tables (integration keys). The tenant table is admin-only in most cases.
alter table if exists tenant_integration_key enable row level security;

create policy if not exists p_key_by_tenant on tenant_integration_key
  using (tenant_id = current_setting('app.current_tenant', true)::uuid)
  with check (tenant_id = current_setting('app.current_tenant', true)::uuid);

alter table if exists tenant_integration_key force row level security;
