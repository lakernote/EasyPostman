package com.laker.postman.service.collections;

import com.laker.postman.request.model.HttpRequestItem;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;

public class RequestSaveEventPublisherTest {

    @AfterMethod
    public void tearDown() {
        RequestSaveEventPublisher.clearListenersForTest();
    }

    @Test
    public void shouldPublishSavedRequestToRegisteredListener() {
        AtomicReference<HttpRequestItem> received = new AtomicReference<>();
        HttpRequestItem item = new HttpRequestItem();

        RequestSaveEventPublisher.register(received::set);

        RequestSaveEventPublisher.publishRequestSaved(item);

        assertSame(received.get(), item);
    }

    @Test
    public void shouldStopPublishingAfterRegistrationIsClosed() {
        AtomicReference<HttpRequestItem> received = new AtomicReference<>();
        AutoCloseable registration = RequestSaveEventPublisher.register(received::set);

        close(registration);
        RequestSaveEventPublisher.publishRequestSaved(new HttpRequestItem());

        assertNull(received.get());
    }

    private void close(AutoCloseable registration) {
        try {
            registration.close();
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}
