/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.infinispan.config;

import org.infinispan.CacheException;
import org.infinispan.config.Configuration.*;
import org.infinispan.util.ReflectionUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * OverrideConfigurationVisitor breaks down fields of Configuration object to individual components
 * and then compares them for field updates.
 * 
 * @author Vladimir Blagojevic
 * @since 4.0
 */
public class OverrideConfigurationVisitor extends AbstractConfigurationBeanVisitor {

   private AsyncType asyncType = null;
   private CacheLoaderManagerConfig cacheLoaderManagerConfig = null;
   private ClusteringType clusteringType = null;
   private final Map <String,BooleanAttributeType> bats = new HashMap<String,BooleanAttributeType>();

   private CustomInterceptorsType customInterceptorsType = null;
   private DeadlockDetectionType deadlockDetectionType = null;
   private EvictionType evictionType = null;
   private ExpirationType expirationType = null;
   private HashType hashType = null;
   private L1Type l1Type = null;
   private LockingType lockingType = null;
   private StateRetrievalType stateRetrievalType = null;
   private SyncType syncType = null;
   private TransactionType transactionType = null;
   private UnsafeType unsafeType = null;
   private QueryConfigurationBean indexingType = null;
   private RecoveryType recoveryType = null;

   public void override(OverrideConfigurationVisitor override) {
      
      // special handling for BooleanAttributeType
      Set<Entry<String, BooleanAttributeType>> entrySet = override.bats.entrySet();
      for (Entry<String, BooleanAttributeType> entry : entrySet) {
         String booleanAttributeName = entry.getKey();
         BooleanAttributeType attributeType = bats.get(booleanAttributeName);
         BooleanAttributeType overrideAttributeType = override.bats.get(booleanAttributeName);
         overrideFields(attributeType, overrideAttributeType);
      }
      
      //do we need to make clones of complex objects like list of cache loaders?
      overrideFields(cacheLoaderManagerConfig, override.cacheLoaderManagerConfig);      
      
      //everything else...
      overrideFields(asyncType, override.asyncType);
      overrideFields(clusteringType, override.clusteringType);
      overrideFields(deadlockDetectionType, override.deadlockDetectionType);
      overrideFields(evictionType, override.evictionType);
      overrideFields(expirationType, override.expirationType);
      overrideFields(hashType, override.hashType);
      overrideFields(l1Type, override.l1Type);
      overrideFields(lockingType, override.lockingType);
      overrideFields(stateRetrievalType, override.stateRetrievalType);
      overrideFields(syncType, override.syncType);
      overrideFields(transactionType, override.transactionType);
      overrideFields(recoveryType, override.recoveryType);
      overrideFields(unsafeType, override.unsafeType);
      overrideFields(indexingType, override.indexingType);
      overrideFields(customInterceptorsType, override.customInterceptorsType);      
   }

   private void overrideFields(AbstractConfigurationBean bean, AbstractConfigurationBean overrides) {
      if (overrides != null && bean != null) {
         // does this component have overridden fields?
         for (String overridenField : overrides.overriddenConfigurationElements) {
            try {
               ReflectionUtil.setValue(bean, overridenField, ReflectionUtil.getValue(overrides,overridenField));
            } catch (Exception e1) {
               throw new CacheException("Could not apply value for field " + overridenField
                        + " from instance " + overrides + " on instance " + this, e1);
            }
         }
      } 
   }

   @Override
   public void visitAsyncType(AsyncType bean) {
      asyncType = bean;
   }

   @Override
   public void visitBooleanAttributeType(BooleanAttributeType bat) {
      bats.put(bat.getFieldName(), bat);
   }

   @Override
   public void visitCacheLoaderManagerConfig(CacheLoaderManagerConfig bean) {
      cacheLoaderManagerConfig = bean;
   }

   @Override
   public void visitClusteringType(ClusteringType bean) {
      clusteringType = bean;
   }

   @Override
   public void visitCustomInterceptorsType(CustomInterceptorsType bean) {
      customInterceptorsType = bean;
   }

   @Override
   public void visitDeadlockDetectionType(DeadlockDetectionType bean) {
      deadlockDetectionType = bean;
   }

   @Override
   public void visitEvictionType(EvictionType bean) {
      evictionType = bean;
   }

   @Override
   public void visitExpirationType(ExpirationType bean) {
      expirationType = bean;
   }

   @Override
   public void visitHashType(HashType bean) {
      hashType = bean;
   }

   @Override
   public void visitL1Type(L1Type bean) {
      l1Type = bean;
   }

   @Override
   public void visitLockingType(LockingType bean) {
      lockingType = bean;
   }

   @Override
   public void visitStateRetrievalType(StateRetrievalType bean) {
      stateRetrievalType = bean;
   }

   @Override
   public void visitSyncType(SyncType bean) {
      syncType = bean;
   }

   @Override
   public void visitTransactionType(TransactionType bean) {
      transactionType = bean;
   }

   @Override
   public void visitUnsafeType(UnsafeType bean) {
      unsafeType = bean;
   }

   @Override
   public void visitQueryConfigurationBean(QueryConfigurationBean bean) {
      indexingType = bean;
   }

   @Override
   public void visitRecoveryType(RecoveryType config) {
      this.recoveryType = config;
   }
}
