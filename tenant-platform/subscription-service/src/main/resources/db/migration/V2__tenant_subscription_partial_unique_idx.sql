create unique index if not exists ux_tenant_subscription_active
    on tenant_subscription.subscription (tenant_id)
    where status in ('TRIALING','ACTIVE','PAST_DUE');
