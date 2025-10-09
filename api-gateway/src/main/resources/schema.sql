CREATE TABLE IF NOT EXISTS public.route_definitions (
    id UUID PRIMARY KEY,
    path_pattern TEXT NOT NULL,
    service_uri TEXT NOT NULL,
    predicates JSONB NOT NULL DEFAULT '[]'::jsonb,
    filters JSONB NOT NULL DEFAULT '[]'::jsonb,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    version INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.route_definition_audit (
    audit_id UUID PRIMARY KEY,
    route_id UUID NOT NULL,
    change_type TEXT NOT NULL,
    payload JSONB NOT NULL,
    changed_by TEXT,
    changed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INTEGER NOT NULL,
    CONSTRAINT fk_route_definition
      FOREIGN KEY(route_id) REFERENCES public.route_definitions(id)
);

-- Seed a couple of baseline routes so the gateway can boot with usable traffic
-- definitions even before the management APIs are exercised. Each INSERT uses a
-- deterministic UUID and ON CONFLICT safeguards to remain idempotent when the
-- schema initialiser executes on every start-up.
INSERT INTO public.route_definitions (
    id,
    path_pattern,
    service_uri,
    predicates,
    filters,
    metadata,
    enabled,
    version,
    created_at,
    updated_at
)
VALUES
    (
        '11111111-1111-1111-1111-111111111111',
        '/api/v1/tenants/**',
        'lb://tenant-service',
        '[{"name":"Path","args":{"pattern":"/api/v1/tenants/**"}}]'::jsonb,
        '[{"name":"StripPrefix","args":{"parts":"1"}},{"name":"PrefixPath","args":{"prefix":"/tenant"}},{"name":"AddRequestHeader","args":{"name":"X-Route-Source","value":"database"}}]'::jsonb,
        '{"methods":["GET","POST"],"stripPrefix":1,"prefixPath":"/tenant","requestHeaders":{"X-Route-Seed":"true"}}'::jsonb,
        TRUE,
        1,
        NOW(),
        NOW()
    ),
    (
        '22222222-2222-2222-2222-222222222222',
        '/api/v1/catalog/**',
        'lb://catalog-service',
        '[{"name":"Path","args":{"pattern":"/api/v1/catalog/**"}}]'::jsonb,
        '[{"name":"StripPrefix","args":{"parts":"1"}},{"name":"PrefixPath","args":{"prefix":"/catalog"}}]'::jsonb,
        '{"methods":["GET"],"stripPrefix":1,"prefixPath":"/catalog","requestHeaders":{"X-Route-Seed":"true"}}'::jsonb,
        TRUE,
        1,
        NOW(),
        NOW()
    ),
    (
        '33333333-3333-3333-3333-333333333333',
        '/api/setup/**',
        'lb://setup-service',
        '[{"name":"Path","args":{"pattern":"/api/setup/**"}}]'::jsonb,
        '[{"name":"StripPrefix","args":{"parts":"2"}},{"name":"PrefixPath","args":{"prefix":"/core"}},{"name":"AddRequestHeader","args":{"name":"X-Route-Source","value":"database"}}]'::jsonb,
        '{"methods":["GET","POST","PUT","PATCH","DELETE"],"stripPrefix":2,"prefixPath":"/core","requestHeaders":{"X-Route-Seed":"true"}}'::jsonb,
        TRUE,
        1,
        NOW(),
        NOW()
    ),
    (
        '44444444-4444-4444-4444-444444444444',
        '/api/auth/**',
        'lb://security-service',
        '[{"name":"Path","args":{"pattern":"/api/auth/**"}}]'::jsonb,
        '[{"name":"StripPrefix","args":{"parts":"1"}},{"name":"PrefixPath","args":{"prefix":"/sec/api/v1"}},{"name":"AddRequestHeader","args":{"name":"X-Route-Source","value":"database"}}]'::jsonb,
        '{"methods":["GET","POST","PUT","PATCH","DELETE"],"stripPrefix":1,"prefixPath":"/sec/api/v1","requestHeaders":{"X-Route-Seed":"true"}}'::jsonb,
        TRUE,
        1,
        NOW(),
        NOW()
    )
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.route_definition_audit (
    audit_id,
    route_id,
    change_type,
    payload,
    changed_by,
    changed_at,
    version
)
VALUES
    (
        'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
        '11111111-1111-1111-1111-111111111111',
        'SEED',
        jsonb_build_object(
            'id', '11111111-1111-1111-1111-111111111111',
            'pathPattern', '/api/v1/tenants/**',
            'serviceUri', 'lb://tenant-service',
            'predicates', '[{"name":"Path","args":{"pattern":"/api/v1/tenants/**"}}]'::jsonb,
            'filters', '[{"name":"StripPrefix","args":{"parts":"1"}},{"name":"PrefixPath","args":{"prefix":"/tenant"}},{"name":"AddRequestHeader","args":{"name":"X-Route-Source","value":"database"}}]'::jsonb,
            'metadata', jsonb_build_object(
                'methods', jsonb_build_array('GET', 'POST'),
                'stripPrefix', 1,
                'prefixPath', '/tenant',
                'requestHeaders', jsonb_build_object('X-Route-Seed', 'true')
            ),
            'enabled', TRUE,
            'version', 1,
            'createdAt', NOW(),
            'updatedAt', NOW()
        ),
        'seed-data',
        NOW(),
        1
    ),
    (
        'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
        '22222222-2222-2222-2222-222222222222',
        'SEED',
        jsonb_build_object(
            'id', '22222222-2222-2222-2222-222222222222',
            'pathPattern', '/api/v1/catalog/**',
            'serviceUri', 'lb://catalog-service',
            'predicates', '[{"name":"Path","args":{"pattern":"/api/v1/catalog/**"}}]'::jsonb,
            'filters', '[{"name":"StripPrefix","args":{"parts":"1"}},{"name":"PrefixPath","args":{"prefix":"/catalog"}}]'::jsonb,
            'metadata', jsonb_build_object(
                'methods', jsonb_build_array('GET'),
                'stripPrefix', 1,
                'prefixPath', '/catalog',
                'requestHeaders', jsonb_build_object('X-Route-Seed', 'true')
            ),
            'enabled', TRUE,
            'version', 1,
            'createdAt', NOW(),
            'updatedAt', NOW()
        ),
        'seed-data',
        NOW(),
        1
    ),
    (
        'cccccccc-cccc-cccc-cccc-cccccccccccc',
        '33333333-3333-3333-3333-333333333333',
        'SEED',
        jsonb_build_object(
            'id', '33333333-3333-3333-3333-333333333333',
            'pathPattern', '/api/setup/**',
            'serviceUri', 'lb://setup-service',
            'predicates', '[{"name":"Path","args":{"pattern":"/api/setup/**"}}]'::jsonb,
            'filters', '[{"name":"StripPrefix","args":{"parts":"2"}},{"name":"PrefixPath","args":{"prefix":"/core"}},{"name":"AddRequestHeader","args":{"name":"X-Route-Source","value":"database"}}]'::jsonb,
            'metadata', jsonb_build_object(
                'methods', jsonb_build_array('GET', 'POST', 'PUT', 'PATCH', 'DELETE'),
                'stripPrefix', 2,
                'prefixPath', '/core',
                'requestHeaders', jsonb_build_object('X-Route-Seed', 'true')
            ),
            'enabled', TRUE,
            'version', 1,
            'createdAt', NOW(),
            'updatedAt', NOW()
        ),
        'seed-data',
        NOW(),
        1
    ),
    (
        'dddddddd-dddd-dddd-dddd-dddddddddddd',
        '44444444-4444-4444-4444-444444444444',
        'SEED',
        jsonb_build_object(
            'id', '44444444-4444-4444-4444-444444444444',
            'pathPattern', '/api/auth/**',
            'serviceUri', 'lb://security-service',
            'predicates', '[{"name":"Path","args":{"pattern":"/api/auth/**"}}]'::jsonb,
            'filters', '[{"name":"StripPrefix","args":{"parts":"1"}},{"name":"PrefixPath","args":{"prefix":"/sec/api/v1"}},{"name":"AddRequestHeader","args":{"name":"X-Route-Source","value":"database"}}]'::jsonb,
            'metadata', jsonb_build_object(
                'methods', jsonb_build_array('GET', 'POST', 'PUT', 'PATCH', 'DELETE'),
                'stripPrefix', 1,
                'prefixPath', '/sec/api/v1',
                'requestHeaders', jsonb_build_object('X-Route-Seed', 'true')
            ),
            'enabled', TRUE,
            'version', 1,
            'createdAt', NOW(),
            'updatedAt', NOW()
        ),
        'seed-data',
        NOW(),
        1
    )
ON CONFLICT (audit_id) DO NOTHING;
