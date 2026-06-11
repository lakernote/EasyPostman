package com.laker.postman.service.collections;

import com.laker.postman.collection.model.CollectionDocument;
import com.laker.postman.collection.model.CollectionRequestContext;
import com.laker.postman.collection.model.RequestGroup;
import com.laker.postman.model.Variable;
import com.laker.postman.request.model.HttpHeader;
import com.laker.postman.request.model.HttpRequestItem;
import com.laker.postman.service.variable.RequestExecutionContext;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.assertEquals;

public class InheritanceServiceTest {

    @AfterMethod
    public void clearRequestScope() {
        RequestExecutionContext.clearCurrentScope();
    }

    @Test
    public void shouldResolveGroupChainFreshEachTimeAndApplyLatestRequestDraft() {
        RequestGroup firstGroup = new RequestGroup("First Parent");
        firstGroup.setHeaders(List.of(new HttpHeader(true, "X-Parent", "1")));
        firstGroup.setVariables(List.of(new Variable(true, "parentToken", "abc")));

        RequestGroup latestGroup = new RequestGroup("Latest Parent");
        latestGroup.setHeaders(List.of(new HttpHeader(true, "X-Latest", "2")));
        latestGroup.setVariables(List.of(new Variable(true, "latestToken", "xyz")));

        AtomicInteger lookupCount = new AtomicInteger();
        CollectionRequestRepository repository = new CollectionRequestRepository() {
            @Override
            public Optional<CollectionRequestContext> findRequestContextById(String requestId) {
                int currentLookup = lookupCount.incrementAndGet();
                RequestGroup group = currentLookup == 1 ? firstGroup : latestGroup;
                return Optional.of(new CollectionRequestContext(null, List.of(group)));
            }

            @Override
            public Optional<CollectionDocument> getDocument() {
                return Optional.empty();
            }
        };

        InheritanceService service = new InheritanceService(repository);

        HttpRequestItem firstDraft = request("https://api.example.com/first");
        HttpRequestItem firstMerged = service.applyInheritance(firstDraft);

        HttpRequestItem latestDraft = request("https://api.example.com/latest");
        HttpRequestItem latestMerged = service.applyInheritance(latestDraft);

        assertEquals(lookupCount.get(), 2);
        assertEquals(firstMerged.getUrl(), "https://api.example.com/first");
        assertEquals(latestMerged.getUrl(), "https://api.example.com/latest");
        assertEquals(latestMerged.getHeadersList().get(0).getKey(), "X-Latest");
        assertEquals(RequestExecutionContext.getCurrentScope().getGroupVariable("latestToken"), "xyz");
    }

    private static HttpRequestItem request(String url) {
        HttpRequestItem item = new HttpRequestItem();
        item.setId("request-1");
        item.setName("Request 1");
        item.setMethod("GET");
        item.setUrl(url);
        return item;
    }
}
