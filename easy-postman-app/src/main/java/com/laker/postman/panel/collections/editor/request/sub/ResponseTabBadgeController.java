package com.laker.postman.panel.collections.editor.request.sub;

import com.laker.postman.script.model.TestResult;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.RequiredArgsConstructor;

import javax.swing.*;
import java.util.List;

/**
 * 响应标签计数/状态徽标控制器。
 * <p>
 * Headers 和 Tests 的标题会随结果动态变化，集中在这里避免 ResponsePanel 混入文案拼装细节。
 */
@RequiredArgsConstructor
final class ResponseTabBadgeController {
    private static final int TAB_INDEX_RESPONSE_HEADERS = 1;
    private static final int TAB_INDEX_TESTS = 2;

    private final JButton[] tabButtons;

    void updateResponseHeadersCount(int count) {
        if (tabButtons.length <= TAB_INDEX_RESPONSE_HEADERS) {
            return;
        }
        JButton headersButton = tabButtons[TAB_INDEX_RESPONSE_HEADERS];
        if (count > 0) {
            String countText = " (" + count + ")";
            String countHtml = I18nUtil.getMessage(MessageKeys.TAB_RESPONSE_HEADERS) +
                    "<span style='color:" + ModernColors.toHtmlColor(ModernColors.getSuccess())
                            + ";font-weight:bold;'>" + countText + "</span>";
            setTabText(headersButton, "<html>" + countHtml + "</html>");
            return;
        }
        setTabText(headersButton, I18nUtil.getMessage(MessageKeys.TAB_RESPONSE_HEADERS));
    }

    void updateTestResults(List<TestResult> testResults) {
        if (tabButtons.length <= TAB_INDEX_TESTS) {
            return;
        }
        JButton testsButton = tabButtons[TAB_INDEX_TESTS];
        if (testResults != null && !testResults.isEmpty()) {
            boolean allPassed = testResults.stream().allMatch(r -> r.passed);
            String countText = " (" + testResults.size() + ")";
            String color = ModernColors.toHtmlColor(allPassed ? ModernColors.getSuccess() : ModernColors.getError());
            String countHtml = I18nUtil.getMessage(MessageKeys.TAB_TESTS) +
                    "<span style='color:" + color + ";font-weight:bold;'>" + countText + "</span>";
            setTabText(testsButton, "<html>" + countHtml + "</html>");
            return;
        }
        setTabText(testsButton, I18nUtil.getMessage(MessageKeys.TAB_TESTS));
    }

    private static void setTabText(JButton button, String text) {
        button.setText(text);
        button.revalidate();
        if (button.getParent() != null) {
            button.getParent().revalidate();
            button.getParent().repaint();
        }
        button.repaint();
    }
}
