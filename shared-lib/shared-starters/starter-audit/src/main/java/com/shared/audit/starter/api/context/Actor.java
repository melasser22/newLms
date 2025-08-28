package com.shared.audit.starter.api.context;

import java.util.List;

public record Actor(String id, String username, List<String> roles, String authType) {}
