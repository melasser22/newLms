insert into subscription.subscription (
    subscription_id,
    ext_subscription_id,
    ext_customer_id,
    ext_product_id,
    ext_tier_id,
    tier_nm_en,
    tier_nm_ar,
    start_dt,
    end_dt,
    subscription_amount,
    total_billed_amount,
    total_paid_amount,
    subscription_stts_cd,
    create_channel,
    unlimited_users_flag,
    unlimited_trans_flag,
    environment_size_cd,
    is_auto_prov_enabled,
    is_deleted,
    created_at,
    created_by
) values (
    500,
    9000,
    7000,
    321,
    654,
    'Gold',
    'ذهبي',
    current_date,
    current_date + interval '30 days',
    100.00,
    0,
    0,
    'ACTIVE',
    'PORTAL',
    'N',
    'N',
    'L',
    'N',
    false,
    now(),
    'fixture'
);

insert into subscription.subscription_feature (
    subscription_feature_id,
    subscription_id,
    feature_cd,
    feature_count
) values (
    601,
    500,
    'API_CALLS',
    25
);

insert into subscription.subscription_additional_service (
    subscription_additional_service_id,
    subscription_id,
    product_additional_service_id,
    service_cd,
    service_name_en,
    service_name_ar,
    service_desc_en,
    service_desc_ar,
    service_price,
    total_amount,
    currency,
    is_countable,
    requested_count,
    payment_type_cd
) values (
    701,
    500,
    9901,
    'LOGS',
    'Logs bundle',
    'حزمة السجلات',
    null,
    null,
    50.00,
    50.00,
    'SAR',
    'Y',
    5,
    'ONE_TIME_FEES'
);
