package com.example.ai_client;

public record ScaleResult(
        String action,
        int fromReplicas,
        int toReplicas,
        boolean ready,
        long elapsedMs
) {}