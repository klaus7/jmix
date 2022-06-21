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

package io.jmix.flowui.component.delegate;

import com.vaadin.flow.component.AbstractField;
import io.jmix.flowui.data.EntityValueSource;
import io.jmix.flowui.data.ValueSource;
import io.jmix.flowui.data.binding.impl.AbstractValueBinding;
import io.jmix.flowui.data.binding.impl.FieldValueBinding;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Set;

@Component("flowui_CollectionFieldDelegate")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class CollectionFieldDelegate<C extends AbstractField<?, Set<P>>, P, V>
        extends AbstractFieldDelegate<C, Collection<V>, Set<P>> {

    public CollectionFieldDelegate(C component) {
        super(component);
    }

    @Override
    protected AbstractValueBinding<Collection<V>> createValueBinding(ValueSource<Collection<V>> valueSource) {
        return applicationContext.getBean(FieldValueBinding.class, valueSource, component);
    }

    public String applyDefaultCollectionItemFormat(@Nullable P item) {
        if (valueBinding != null
                && valueBinding.getValueSource() instanceof EntityValueSource) {
            EntityValueSource<?, V> entityValueSource = (EntityValueSource<?, V>) valueBinding.getValueSource();
            return metadataTools.format(item, entityValueSource.getMetaPropertyPath().getMetaProperty());
        }

        return metadataTools.format(item);
    }
}
