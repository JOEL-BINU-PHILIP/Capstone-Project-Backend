package com.app.billing.service;

import com.app.billing.event.BillingEvent;

public interface EventPublisherService {
    void publishBillingEvent(BillingEvent event);
}