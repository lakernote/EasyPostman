package com.laker.postman.util;

import org.testng.annotations.Test;

import java.util.Locale;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class UiFontCatalogTest {

    @Test(description = "字体列表应忽略空项并按名称去重后排序")
    public void testNormalizeFamilies_DeduplicatesAndSorts() {
        List<String> normalized = UiFontCatalog.normalizeFamilies(List.of(
                "Consolas",
                "Microsoft YaHei UI",
                "consolas",
                " ",
                "",
                "Arial"
        ));

        assertEquals(normalized, List.of("Arial", "Consolas", "Microsoft YaHei UI"));
    }

    @Test(description = "字体目录应保留可用性检测结果，供界面提示和校验复用")
    public void testBuildFontOptions_PreservesSupportStatus() {
        List<UiFontCatalog.FontOption> options = UiFontCatalog.buildFontOptions(
                List.of("Font B", "font b", "Font A"),
                family -> switch (family) {
                    case "Font A" -> UiFontCatalog.FontSupport.FULL;
                    case "Font B" -> UiFontCatalog.FontSupport.NO_CJK;
                    default -> UiFontCatalog.FontSupport.NO_EMOJI;
                }
        );

        assertEquals(options.size(), 2);
        assertEquals(options.get(0).family(), "Font A");
        assertEquals(options.get(0).support(), UiFontCatalog.FontSupport.FULL);
        assertEquals(options.get(1).family(), "Font B");
        assertEquals(options.get(1).support(), UiFontCatalog.FontSupport.NO_CJK);
    }

    @Test(description = "不支持中文的字体不应被视为可安全用于界面字体")
    public void testFontSupport_IsUiSafeRequiresCjkSupport() {
        assertTrue(UiFontCatalog.FontSupport.FULL.isUiSafe());
        assertTrue(UiFontCatalog.FontSupport.NO_EMOJI.isUiSafe());
        assertFalse(UiFontCatalog.FontSupport.NO_CJK.isUiSafe());
    }

    @Test(description = "中文界面不应允许缺少中文 glyph 的 UI 字体，英文界面只提示风险")
    public void testFontSupport_ChineseLocaleRequiresCjkSafeUiFont() {
        assertFalse(UiFontCatalog.isUiFontAllowedForLocale(UiFontCatalog.FontSupport.NO_CJK, Locale.CHINESE));
        assertTrue(UiFontCatalog.isUiFontAllowedForLocale(UiFontCatalog.FontSupport.NO_CJK, Locale.ENGLISH));
        assertTrue(UiFontCatalog.isUiFontAllowedForLocale(UiFontCatalog.FontSupport.NO_EMOJI, Locale.CHINESE));
        assertTrue(UiFontCatalog.isUiFontAllowedForLocale(UiFontCatalog.FontSupport.FULL, Locale.CHINESE));
    }

    @Test(description = "不存在的物理字体不应被 Java 的 Dialog 回退误判为支持中文")
    public void testInspectFamily_MissingPhysicalFontShouldNotBeCjkSafe() {
        String missingFamily = "Definitely Missing EasyPostman Font " + System.nanoTime();

        assertEquals(UiFontCatalog.inspectFamily(missingFamily), UiFontCatalog.FontSupport.NO_CJK);
    }

    @Test(description = "懒加载字体列表时应保留当前值并与新列表合并去重排序")
    public void testMergeFamiliesForCombo_PreservesCurrentValue() {
        List<String> merged = UiFontCatalog.mergeFamiliesForCombo(
                "Microsoft YaHei Mono",
                List.of("Consolas", "microsoft yahei mono", "JetBrains Mono")
        );

        assertEquals(merged, List.of("Consolas", "JetBrains Mono", "Microsoft YaHei Mono"));
    }

    @Test(description = "中文界面的 UI 字体下拉框应过滤掉不支持中文的字体")
    public void testMergeUiFamiliesForCombo_ChineseLocaleKeepsOnlyCjkSafeFonts() {
        List<String> merged = UiFontCatalog.mergeUiFamiliesForCombo(
                "",
                List.of(
                        new UiFontCatalog.FontOption("Consolas", UiFontCatalog.FontSupport.NO_CJK),
                        new UiFontCatalog.FontOption("Microsoft YaHei UI", UiFontCatalog.FontSupport.NO_EMOJI),
                        new UiFontCatalog.FontOption("Noto Color Emoji", UiFontCatalog.FontSupport.NO_CJK)
                ),
                Locale.CHINESE
        );

        assertEquals(merged, List.of("Microsoft YaHei UI"));
    }

    @Test(description = "编辑器主字体下拉框应优先保留代码编辑更适合的等宽字体")
    public void testMergeEditorPrimaryFamiliesForCombo_KeepsLikelyCodeFonts() {
        List<String> merged = UiFontCatalog.mergeEditorPrimaryFamiliesForCombo(
                "",
                List.of(
                        new UiFontCatalog.FontOption("Arial", UiFontCatalog.FontSupport.NO_EMOJI),
                        new UiFontCatalog.FontOption("Consolas", UiFontCatalog.FontSupport.NO_CJK),
                        new UiFontCatalog.FontOption("JetBrains Mono", UiFontCatalog.FontSupport.NO_CJK),
                        new UiFontCatalog.FontOption("PingFang SC", UiFontCatalog.FontSupport.NO_EMOJI)
                )
        );

        assertEquals(merged, List.of("Consolas", "JetBrains Mono"));
    }

    @Test(description = "编辑器回退字体下拉框应优先保留能显示中文的字体")
    public void testMergeEditorFallbackFamiliesForCombo_KeepsCjkSafeFonts() {
        List<String> merged = UiFontCatalog.mergeEditorFallbackFamiliesForCombo(
                "",
                List.of(
                        new UiFontCatalog.FontOption("Consolas", UiFontCatalog.FontSupport.NO_CJK),
                        new UiFontCatalog.FontOption("Microsoft YaHei UI", UiFontCatalog.FontSupport.NO_EMOJI),
                        new UiFontCatalog.FontOption("PingFang SC", UiFontCatalog.FontSupport.FULL)
                )
        );

        assertEquals(merged, List.of("Microsoft YaHei UI", "PingFang SC"));
    }

    @Test(description = "字体缓存应只触发一次后台加载并复用结果")
    public void testFontOptionCache_LoadsOnlyOnce() {
        UiFontCatalog.FontOptionCache cache = new UiFontCatalog.FontOptionCache();
        AtomicInteger loadCount = new AtomicInteger();
        List<UiFontCatalog.FontOption> loaded = new ArrayList<>(List.of(
                new UiFontCatalog.FontOption("Font A", UiFontCatalog.FontSupport.FULL)
        ));

        List<UiFontCatalog.FontOption> first = cache.getOrLoadAsync(() -> {
            loadCount.incrementAndGet();
            return loaded;
        }).join();
        List<UiFontCatalog.FontOption> second = cache.getOrLoadAsync(() -> {
            loadCount.incrementAndGet();
            return List.of(new UiFontCatalog.FontOption("Font B", UiFontCatalog.FontSupport.NO_EMOJI));
        }).join();

        assertEquals(loadCount.get(), 1);
        assertSame(first, second);
        assertSame(cache.getCachedOptions(), loaded);
    }
}
