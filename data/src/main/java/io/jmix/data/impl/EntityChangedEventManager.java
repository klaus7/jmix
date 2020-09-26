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

package io.jmix.data.impl;

import io.jmix.core.*;
import io.jmix.core.entity.EntitySystemAccess;
import io.jmix.core.entity.EntityValues;
import io.jmix.core.entity.annotation.PublishEntityChangedEvents;
import io.jmix.core.metamodel.model.MetaClass;
import io.jmix.core.metamodel.model.MetaProperty;
import io.jmix.data.event.AttributeChanges;
import io.jmix.data.event.EntityChangedEvent;
import org.eclipse.persistence.descriptors.changetracking.ChangeTracker;
import org.eclipse.persistence.internal.descriptors.changetracking.AttributeChangeListener;
import org.eclipse.persistence.sessions.changesets.AggregateChangeRecord;
import org.eclipse.persistence.sessions.changesets.ChangeRecord;
import org.eclipse.persistence.sessions.changesets.ObjectChangeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.ResourceHolderSupport;
import org.springframework.transaction.support.ResourceHolderSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static io.jmix.core.entity.EntitySystemAccess.getEntityEntry;

@Component(EntityChangedEventManager.NAME)
public class EntityChangedEventManager {

    public static final String NAME = "data_EntityChangedEventManager";

    private static final Logger log = LoggerFactory.getLogger(EntityChangedEventManager.class);

    private static final String RESOURCE_KEY = AccumulatedInfoHolder.class.getName();

    @Autowired
    private Metadata metadata;
    @Autowired
    protected PersistenceSupport persistenceSupport;

    @Autowired
    private Events eventPublisher;

    @Autowired
    private ExtendedEntities extendedEntities;

    private Map<Class, PublishingInfo> infoCache = new ConcurrentHashMap<>();

    private static class PublishingInfo {
        final boolean publish;
        final boolean onCreated;
        final boolean onUpdated;
        final boolean onDeleted;
        final MetaClass originalMetaClass;

        public PublishingInfo() {
            publish = false;
            onCreated = false;
            onUpdated = false;
            onDeleted = false;
            originalMetaClass = null;
        }

        public PublishingInfo(boolean onCreated, boolean onUpdated, boolean onDeleted, MetaClass originalMetaClass) {
            this.publish = true;
            this.onCreated = onCreated;
            this.onUpdated = onUpdated;
            this.onDeleted = onDeleted;
            this.originalMetaClass = originalMetaClass;
        }
    }

    private static class AccumulatedInfoHolder extends ResourceHolderSupport {

        List<EntityChangedEventInfo> accumulatedList;
    }

    private static class AccumulatedInfoSynchronization extends ResourceHolderSynchronization<AccumulatedInfoHolder, String> {

        AccumulatedInfoSynchronization(AccumulatedInfoHolder resourceHolder) {
            super(resourceHolder, RESOURCE_KEY);
        }
    }

    private AccumulatedInfoHolder getAccumulatedInfoHolder() {
        AccumulatedInfoHolder holder = (AccumulatedInfoHolder) TransactionSynchronizationManager.getResource(RESOURCE_KEY);
        if (holder == null) {
            holder = new AccumulatedInfoHolder();
            TransactionSynchronizationManager.bindResource(RESOURCE_KEY, holder);
        }
        if (TransactionSynchronizationManager.isSynchronizationActive() && !holder.isSynchronizedWithTransaction()) {
            holder.setSynchronizedWithTransaction(true);
            TransactionSynchronizationManager.registerSynchronization(new AccumulatedInfoSynchronization(holder));
        }
        return holder;
    }

    public void beforeFlush(Collection<Object> instances) {
        log.trace("beforeFlush {}", instances);
        List<EntityChangedEventInfo> infoList = internalCollect(instances);
        AccumulatedInfoHolder holder = getAccumulatedInfoHolder();
        holder.accumulatedList = merge(holder.accumulatedList, infoList);
    }

    private List<EntityChangedEventInfo> merge(Collection<EntityChangedEventInfo> collection1, Collection<EntityChangedEventInfo> collection2) {
        List<EntityChangedEventInfo> list1 = collection1 != null ? new ArrayList<>(collection1) : new ArrayList<>();
        Collection<EntityChangedEventInfo> list2 = collection2 != null ? collection2 : Collections.emptyList();

        for (EntityChangedEventInfo info2 : list2) {
            Optional<EntityChangedEventInfo> opt = list1.stream()
                    .filter(info1 -> info1.getEntity() == info2.getEntity())
                    .findAny();
            if (opt.isPresent()) {
                opt.get().mergeWith(info2);
            } else {
                list1.add(info2);
            }
        }
        log.trace("merged {}", list1);
        return list1;
    }

    public List<EntityChangedEventInfo> collect(Collection<Object> entities) {
        log.trace("collect {}", entities);
        AccumulatedInfoHolder holder = getAccumulatedInfoHolder();
        List<EntityChangedEventInfo> infoList = internalCollect(entities);
        return merge(holder.accumulatedList, infoList);
    }

    public List<EntityChangedEventInfo> internalCollect(Collection<Object> entities) {
        List<EntityChangedEventInfo> list = new ArrayList<>();
        for (Object entity : entities) {

            PublishingInfo info = infoCache.computeIfAbsent(entity.getClass(), aClass -> {
                MetaClass metaClass = metadata.getClass(entity.getClass());
                MetaClass originalMetaClass = extendedEntities.getOriginalOrThisMetaClass(metaClass);
                Map attrMap = (Map) metaClass.getAnnotations().get(PublishEntityChangedEvents.class.getName());
                if (attrMap != null) {
                    return new PublishingInfo(
                            Boolean.TRUE.equals(attrMap.get("created")),
                            Boolean.TRUE.equals(attrMap.get("updated")),
                            Boolean.TRUE.equals(attrMap.get("deleted")),
                            originalMetaClass);
                }
                return new PublishingInfo();
            });


            if (info.publish) {
                EntityChangedEvent.Type type = null;
                AttributeChanges attributeChanges = null;
                if (info.onCreated && getEntityEntry(entity).isNew()) {
                    type = EntityChangedEvent.Type.CREATED;
                    attributeChanges = getEntityAttributeChanges(entity, false);
                } else {
                    if (info.onUpdated || info.onDeleted) {
                        AttributeChangeListener changeListener =
                                (AttributeChangeListener) ((ChangeTracker) entity)._persistence_getPropertyChangeListener();
                        if (changeListener == null) {
                            log.debug("Cannot publish EntityChangedEvent for {} because its AttributeChangeListener is null", entity);
                            continue;
                        }
                        if (info.onDeleted && persistenceSupport.isDeleted(entity, changeListener)) {
                            type = EntityChangedEvent.Type.DELETED;
                            attributeChanges = getEntityAttributeChanges(entity, true);
                        } else if (info.onUpdated && changeListener.hasChanges()) {
                            type = EntityChangedEvent.Type.UPDATED;
                            attributeChanges = getEntityAttributeChanges(entity, changeListener.getObjectChangeSet());
                        }
                    }
                }
                if (type != null && attributeChanges != null) {
                    @SuppressWarnings("unchecked")
                    EntityChangedEventInfo eventData = new EntityChangedEventInfo(this, entity, type,
                            attributeChanges, info.originalMetaClass);
                    list.add(eventData);
                }
            }
        }
        log.trace("collected {}", list);
        return list;
    }

    public void publish(Collection<EntityChangedEvent> events) {
        log.trace("publish {}", events);
        for (EntityChangedEvent event : events) {
            eventPublisher.publish(event);
        }
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private AttributeChanges getEntityAttributeChanges(@Nullable Object entity, @Nullable ObjectChangeSet changeSet) {
        if (changeSet == null)
            return null;
        Set<AttributeChanges.Change> changes = new HashSet<>();
        Map<String, AttributeChanges> embeddedChanges = new HashMap<>();

        for (ChangeRecord changeRecord : changeSet.getChanges()) {
            if (changeRecord instanceof AggregateChangeRecord) {
                embeddedChanges.computeIfAbsent(changeRecord.getAttribute(), s ->
                        getEntityAttributeChanges(null, ((AggregateChangeRecord) changeRecord).getChangedObject()));
            } else {
                Object oldValue = changeRecord.getOldValue();
                if (oldValue instanceof Entity) {
                    changes.add(new AttributeChanges.Change(changeRecord.getAttribute(), Id.of(oldValue)));
                } else if (oldValue instanceof Collection) {
                    Collection<Object> coll = (Collection<Object>) oldValue;
                    Collection<Id> idColl = oldValue instanceof List ? new ArrayList<>() : new LinkedHashSet<>();
                    for (Object item : coll) {
                        idColl.add(Id.of(item));
                    }
                    changes.add(new AttributeChanges.Change(changeRecord.getAttribute(), idColl));
                } else {
                    changes.add(new AttributeChanges.Change(changeRecord.getAttribute(), oldValue));
                }
            }
        }

        // todo dynamic attributes
//        addDynamicAttributeChanges(entity, changes, false);

        return new AttributeChanges(changes, embeddedChanges);
    }

    // todo dynamic attributes
//    @SuppressWarnings("unchecked")
//    private void addDynamicAttributeChanges(@Nullable Entity entity, Set<AttributeChanges.Change> changes, boolean deleted) {
//        if (entity instanceof BaseGenericIdEntity && ((BaseGenericIdEntity) entity).getDynamicAttributes() != null) {
//            Map<String, CategoryAttributeValue> dynamicAttributes = ((BaseGenericIdEntity) entity).getDynamicAttributes();
//            for (CategoryAttributeValue cav : dynamicAttributes.values()) {
//                if (BaseEntityInternalAccess.isNew(cav)) {
//                    changes.add(new AttributeChanges.Change(DynamicAttributesUtils.encodeAttributeCode(cav.getCode()), null));
//                } else {
//                    if (deleted) {
//                        Object oldValue;
//                        switch (cav.getCategoryAttribute().getDataType()) {
//                            case STRING:
//                            case ENUMERATION:
//                                oldValue = cav.getStringValue();
//                                break;
//                            case INTEGER:
//                                oldValue = cav.getIntValue();
//                                break;
//                            case DOUBLE:
//                                oldValue = cav.getDoubleValue();
//                                break;
//                            case BOOLEAN:
//                                oldValue = cav.getBooleanValue();
//                                break;
//                            case DATE:
//                                oldValue = cav.getDateValue();
//                                break;
//                            case ENTITY:
//                                Object entityId = cav.getEntityValue().getObjectEntityId();
//                                Class entityClass = cav.getCategoryAttribute().getJavaClassForEntity();
//                                oldValue = entityId != null ? Id.of(entityId, entityClass) : null;
//                                break;
//                            default:
//                                log.warn("Unsupported dynamic attribute type: " + cav.getCategoryAttribute().getDataType());
//                                oldValue = null;
//                        }
//                        changes.add(new AttributeChanges.Change(DynamicAttributesUtils.encodeAttributeCode(cav.getCode()), oldValue));
//                    } else {
//                        AttributeChangeListener changeListener =
//                                (AttributeChangeListener) ((ChangeTracker) cav)._persistence_getPropertyChangeListener();
//                        if (changeListener != null && changeListener.getObjectChangeSet() != null) {
//                            Object oldValue = null;
//                            boolean changed = false;
//                            for (ChangeRecord changeRecord : changeListener.getObjectChangeSet().getChanges()) {
//                                switch (changeRecord.getAttribute()) {
//                                    case "stringValue":
//                                    case "intValue":
//                                    case "doubleValue":
//                                    case "booleanValue":
//                                    case "dateValue":
//                                        oldValue = changeRecord.getOldValue();
//                                        changed = true;
//                                        break;
//                                    case "entityValue":
//                                        Object entityId = ((ReferenceToEntity) changeRecord.getOldValue()).getObjectEntityId();
//                                        Class entityClass = cav.getCategoryAttribute().getJavaClassForEntity();
//                                        oldValue = entityId != null ? Id.of(entityId, entityClass) : null;
//                                        changed = true;
//                                        break;
//                                }
//                                if (changed) {
//                                    changes.add(new AttributeChanges.Change(DynamicAttributesUtils.encodeAttributeCode(cav.getCode()), oldValue));
//                                    break;
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }

    @SuppressWarnings("unchecked")
    private AttributeChanges getEntityAttributeChanges(Object entity, boolean deleted) {
        Set<AttributeChanges.Change> changes = new HashSet<>();
        Map<String, AttributeChanges> embeddedChanges = new HashMap<>();

        for (MetaProperty property : metadata.getClass(entity.getClass()).getProperties()) {
            Object value = EntityValues.getValue(entity, property.getName());
            if (deleted) {
                if (value instanceof Entity) {
                    if (EntitySystemAccess.isEmbeddable(entity)) {
                        embeddedChanges.computeIfAbsent(property.getName(), s -> getEntityAttributeChanges(value, true));
                    } else {
                        changes.add(new AttributeChanges.Change(property.getName(), Id.of(value)));
                    }
                } else if (value instanceof Collection) {
                    Collection<Object> coll = (Collection<Object>) value;
                    Collection<Id> idColl = value instanceof List ? new ArrayList<>() : new LinkedHashSet<>();
                    for (Object item : coll) {
                        idColl.add(Id.of(item));
                    }
                    changes.add(new AttributeChanges.Change(property.getName(), idColl));
                } else {
                    changes.add(new AttributeChanges.Change(property.getName(), value));
                }

            } else {
                if (value != null) {
                    changes.add(new AttributeChanges.Change(property.getName(), null));
                }
            }
        }

        if (deleted) {
            // todo dynamic attributes
//            addDynamicAttributeChanges(entity, changes, true);
        }

        return new AttributeChanges(changes, embeddedChanges);
    }
}

