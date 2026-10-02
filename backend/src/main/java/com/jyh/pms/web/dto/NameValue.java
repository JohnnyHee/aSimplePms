package com.jyh.pms.web.dto;

import java.util.List;

/**
 * 统计图表用的 {name, value} 数据点。
 */
public record NameValue(String name, long value) {

    public static List<NameValue> none() {
        return List.of();
    }
}
