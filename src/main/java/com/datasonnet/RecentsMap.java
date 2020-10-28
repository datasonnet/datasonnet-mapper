package com.datasonnet;

/*-
 * Copyright 2019-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A map retaining up to the given capacity by most recently inserted.
 * This is not as efficient for retrieval time as an LRU Map would be,
 * but the difference should be vanishingly small except under very unusual usage patterns,
 * and it serves the main purpose of bounding the space taken.
 *
 * @param <K> Key Type
 * @param <V> Value Type
 */
public class RecentsMap<K, V> extends LinkedHashMap<K, V> {
    private final int maximumCapacity;

    public RecentsMap(int maximumCapacity) {
        // we should not be growing often, and will cap growing at some point,
        // so load factor is higher. We start with more slots, since these will be used places
        // expected to receive multiple entries.
        // The exact values have not been tuned at all.
        super(16, .9f);
        this.maximumCapacity = maximumCapacity;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > maximumCapacity;
    }
}
