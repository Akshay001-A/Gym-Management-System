
package com.gym.payment.dto;

import tools.jackson.databind.JsonNode;

public class PaymentWebhookRequest {

    private String entity;
    private String accountId;
    private String event;
    private JsonNode contains;
    private JsonNode payload;
    private Long createdAt;

    public PaymentWebhookRequest() {
    }

    public String getEntity() {
        return entity;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public JsonNode getContains() {
        return contains;
    }

    public void setContains(JsonNode contains) {
        this.contains = contains;
    }

    public JsonNode getPayload() {
        return payload;
    }

    public void setPayload(JsonNode payload) {
        this.payload = payload;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }
}
