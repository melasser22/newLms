#!/usr/bin/env bash
curl -X POST "http://localhost:8080/subscription/get-token" \
  -H "Content-Type: application/json" \
  -H "rqUID: c73bcdcc-2669-4bf6-81d3-e4ae73fb11fd" \
  -d '{
    "loginName": "demo",
    "password": "b03ddf3ca2e714a6548e7495e2a03f5e824eaac9837cd7f159c67b90fb4b7342"
  }'
