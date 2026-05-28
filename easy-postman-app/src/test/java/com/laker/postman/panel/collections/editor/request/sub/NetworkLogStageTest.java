package com.laker.postman.panel.collections.editor.request.sub;

import com.laker.postman.service.http.NetworkLogEventStage;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class NetworkLogStageTest {

    @Test
    public void shouldMapEveryHttpNetworkLogEventStageToUiStage() {
        for (NetworkLogEventStage eventStage : NetworkLogEventStage.values()) {
            assertEquals(NetworkLogStage.fromEventStage(eventStage).name(), eventStage.name(),
                    "Every service-layer network log stage should have a UI rendering stage");
        }
    }

    @Test
    public void shouldUseDefaultUiStageWhenEventStageIsNull() {
        assertEquals(NetworkLogStage.fromEventStage(null), NetworkLogStage.DEFAULT);
    }
}
