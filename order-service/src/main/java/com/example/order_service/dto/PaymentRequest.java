package com.example.order_service.dto;

public record PaymentRequest(String orderId, double amount) {}