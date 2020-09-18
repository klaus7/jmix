/*
 * Copyright 2019 Haulmont.
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

package io.jmix.core.impl;

import io.jmix.core.annotation.Internal;

/**
 * INTERNAL.
 * Describes entity class for metadata loading.
 */
@Internal
public class EntityClassInfo {

    public final String store;
    public final String name;
    public final boolean persistent;

    public EntityClassInfo(String store, String name, boolean persistent) {
        this.store = store;
        this.name = name;
        this.persistent = persistent;
    }

    @Override
    public String toString() {
        return name + " - " + store + " - " + (persistent ? "persistent" : "not persistent");
    }
}
