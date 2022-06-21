/*
 * Copyright 2022 Haulmont.
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

package io.jmix.flowui.component.listbox;

import com.vaadin.flow.component.listbox.ListBox;
import io.jmix.flowui.component.delegate.FieldDelegate;
import io.jmix.flowui.component.delegate.ListOptionsDelegate;
import io.jmix.flowui.data.*;
import io.jmix.flowui.data.options.ContainerOptions;
import io.jmix.flowui.model.CollectionContainer;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.annotation.Nullable;

public class JmixListBox<V> extends ListBox<V> implements SupportsValueSource<V>, SupportsOptions<V>,
        SupportsOptionsContainer<V>, ApplicationContextAware, InitializingBean {

    protected ApplicationContext applicationContext;

    protected FieldDelegate<JmixListBox<V>, V, V> fieldDelegate;
    protected ListOptionsDelegate<JmixListBox<V>, V> optionsDelegate;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        initComponent();
    }

    protected void initComponent() {
        fieldDelegate = createFieldDelegate();
        optionsDelegate = createOptionsDelegate();

        setItemLabelGenerator(fieldDelegate::applyDefaultValueFormat);
    }

    protected FieldDelegate<JmixListBox<V>, V, V> createFieldDelegate() {
        return applicationContext.getBean(FieldDelegate.class, this);
    }

    protected ListOptionsDelegate<JmixListBox<V>, V> createOptionsDelegate() {
        return applicationContext.getBean(ListOptionsDelegate.class, this);
    }

    @Nullable
    @Override
    public Options<V> getOptions() {
        return optionsDelegate.getOptions();
    }

    @Override
    public void setOptions(@Nullable Options<V> options) {
        optionsDelegate.setOptions(options);
    }

    @Override
    public void setOptionsContainer(CollectionContainer<V> container) {
        optionsDelegate.setOptions(new ContainerOptions<>(container));
    }

    @Nullable
    @Override
    public ValueSource<V> getValueSource() {
        return fieldDelegate.getValueSource();
    }

    @Override
    public void setValueSource(@Nullable ValueSource<V> valueSource) {
        fieldDelegate.setValueSource(valueSource);
    }
}
