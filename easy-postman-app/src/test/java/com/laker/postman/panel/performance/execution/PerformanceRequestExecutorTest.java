package com.laker.postman.panel.performance.execution;

import com.laker.postman.model.PreparedRequest;
import com.laker.postman.panel.performance.assertion.AssertionData;
import com.laker.postman.panel.performance.model.JMeterTreeNode;
import com.laker.postman.panel.performance.model.NodeType;
import com.laker.postman.service.setting.SettingManager;
import org.testng.annotations.Test;

import javax.swing.tree.DefaultMutableTreeNode;
import java.lang.reflect.Method;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

public class PerformanceRequestExecutorTest {

    @Test
    public void shouldUseFullResponseBodyOutsideEfficientMode() {
        PreparedRequest.ResponseBodyMode mode = PerformanceRequestExecutor.resolveHttpResponseBodyMode(
                false,
                List.of(),
                ""
        );

        assertEquals(mode, PreparedRequest.ResponseBodyMode.FULL);
    }

    @Test
    public void shouldSkipResponseBodyInEfficientModeWhenOnlyMetadataIsNeeded() {
        PreparedRequest.ResponseBodyMode mode = PerformanceRequestExecutor.resolveHttpResponseBodyMode(
                true,
                List.of(assertionNode("Response Code", true)),
                ""
        );

        assertEquals(mode, PreparedRequest.ResponseBodyMode.METADATA_ONLY);
    }

    @Test
    public void shouldUsePreviewInEfficientModeWhenAssertionsNeedBody() {
        PreparedRequest.ResponseBodyMode mode = PerformanceRequestExecutor.resolveHttpResponseBodyMode(
                true,
                List.of(assertionNode("Contains", true)),
                ""
        );

        assertEquals(mode, PreparedRequest.ResponseBodyMode.PREVIEW);
    }

    @Test
    public void shouldUsePreviewInEfficientModeWhenPostScriptMayReadBody() {
        PreparedRequest.ResponseBodyMode mode = PerformanceRequestExecutor.resolveHttpResponseBodyMode(
                true,
                List.of(),
                "pm.test('body', () => pm.response.text())"
        );

        assertEquals(mode, PreparedRequest.ResponseBodyMode.PREVIEW);
    }

    @Test
    public void shouldResolveConfiguredPreviewLimitToBytes() {
        assertEquals(PerformanceRequestExecutor.resolveResponseBodyPreviewLimitBytes(4), 4 * 1024);
    }

    @Test
    public void shouldClampInvalidPreviewLimitToDefault() {
        assertEquals(
                PerformanceRequestExecutor.resolveResponseBodyPreviewLimitBytes(0),
                SettingManager.DEFAULT_PERFORMANCE_RESPONSE_BODY_PREVIEW_LIMIT_KB * 1024
        );
    }

    @Test
    public void shouldDisableCookieNotificationsForPerformanceRequests() throws Exception {
        PreparedRequest request = new PreparedRequest();
        PerformanceRequestExecutor executor = new PerformanceRequestExecutor(
                () -> true,
                throwable -> false,
                java.util.concurrent.ConcurrentHashMap.newKeySet(),
                java.util.concurrent.ConcurrentHashMap.newKeySet()
        );
        Method configureMethod = PerformanceRequestExecutor.class.getDeclaredMethod(
                "configurePreparedRequest",
                PreparedRequest.class
        );
        configureMethod.setAccessible(true);

        configureMethod.invoke(executor, request);

        assertFalse(request.notifyCookieChanges);
    }

    private static DefaultMutableTreeNode assertionNode(String type, boolean enabled) {
        AssertionData data = new AssertionData();
        data.type = type;
        JMeterTreeNode node = new JMeterTreeNode(type, NodeType.ASSERTION, data);
        node.enabled = enabled;
        return new DefaultMutableTreeNode(node);
    }
}
