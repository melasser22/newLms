-- Focused indexes to keep the hot paths fast

-- USAGE
create index if not exists idx_usage_counter_sub_typ
  on usage_counter (ext_subscription_id, consumption_typ_cd);
create index if not exists idx_usage_counter_updated_at
  on usage_counter (updated_at desc);

create index if not exists idx_usage_event_product_time
  on usage_event (ext_product_id, received_at desc);
create index if not exists idx_usage_event_rq_uid
  on usage_event (rq_uid);

-- INVOICES
create index if not exists idx_invoice_sub
  on invoice (ext_subscription_id);
create index if not exists idx_invoice_customer
  on invoice (ext_customer_id);
create index if not exists idx_invoice_status_dt
  on invoice (status_cd, invoice_dt desc);

create index if not exists idx_invoice_item_invoice
  on invoice_item (invoice_id, line_no);

-- ATTACHMENTS
create index if not exists idx_invoice_attachment_invoice_created
  on invoice_attachment (invoice_id, created_at desc);

-- OUTBOX
create index if not exists idx_outbox_unpublished
  on outbox_event (published, created_at);
