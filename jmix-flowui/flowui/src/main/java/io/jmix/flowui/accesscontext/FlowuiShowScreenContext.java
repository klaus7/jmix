/*
 * Copyright 2020 Haulmont.
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

package io.jmix.flowui.accesscontext;

import io.jmix.core.accesscontext.AccessContext;

import javax.annotation.Nullable;

public class FlowuiShowScreenContext implements AccessContext {

    protected final String screenId;

    protected boolean permitted = true;

    public FlowuiShowScreenContext(String screenId) {
        this.screenId = screenId;
    }

    public String getScreenId() {
        return screenId;
    }

    public void setDenied() {
        permitted = false;
    }

    public boolean isPermitted() {
        return permitted;
    }

    @Nullable
    @Override
    public String explainConstraints() {
        return !permitted ? "screen: " + screenId : null;
    }
}
