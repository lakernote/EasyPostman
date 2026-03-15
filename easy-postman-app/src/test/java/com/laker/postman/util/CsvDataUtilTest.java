package com.laker.postman.util;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class CsvDataUtilTest {

    @Test(description = "标准 CSV 应支持双引号字段中的换行")
    public void shouldParseQuotedMultilineField() {
        CsvDataUtil.CsvTextData csvTextData = CsvDataUtil.parseCsvText(
                "name,comment\n" +
                        "alice,\"line1\nline2\"\n"
        );

        assertEquals(csvTextData.headers().size(), 2);
        assertEquals(csvTextData.rows().size(), 1);
        assertEquals(csvTextData.rows().get(0).get(1), "line1\nline2");
    }

    @Test(description = "标准 CSV 不应无条件裁剪字段前后空格")
    public void shouldPreserveFieldWhitespace() {
        CsvDataUtil.CsvTextData csvTextData = CsvDataUtil.parseCsvText(
                "name,value\n" +
                        "alice,  keep-space  \n"
        );

        assertEquals(csvTextData.rows().size(), 1);
        assertEquals(csvTextData.rows().get(0).get(1), "  keep-space  ");
    }
}
