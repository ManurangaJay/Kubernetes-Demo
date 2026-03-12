package com.example.order_service.dto;

public record OrderRequest(String productId, int quantity, double price) {}