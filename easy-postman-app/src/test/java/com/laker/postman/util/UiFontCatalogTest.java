package com.laker.postman.util;

import org.testng.annotations.Test;

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

    @Test(description = "懒加载字体列表时应保留当前值并与新列表合并去重排序")
    public void testMergeFamiliesForCombo_PreservesCurrentValue() {
        List<String> merged = UiFontCatalog.mergeFamiliesForCombo(
                "Microsoft YaHei Mono",
                List.of("Consolas", "microsoft yahei mono", "JetBrains Mono")
        );

        assertEquals(merged, List.of("Consolas", "JetBrains Mono", "Microsoft YaHei Mono"));
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
