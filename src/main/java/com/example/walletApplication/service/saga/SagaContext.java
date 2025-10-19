package com.example.walletApplication.service.saga;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Data
@NoArgsConstructor
public class SagaContext {

    private Map<String, Object> data;

    public SagaContext(Map<String, Object> data) {
        this.data = null == data ? new HashMap<>() : data;
    }

    public void put(String key, Object val) {
        data.put(key, val);
    }

    public Object get(String key) {
        return data.get(key);
    }

    public Long getLong(String key) {
        Object value = get(key);
        if(value instanceof Number) {
            return ((Number) value).longValue();
        }
        return null;
    }

    public BigDecimal getBigDecimal(String key) {
        Object value = get(key);
        if(value instanceof Number){
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        return null;
    }

}
