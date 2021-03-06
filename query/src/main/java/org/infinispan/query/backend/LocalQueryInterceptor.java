/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.infinispan.query.backend;

import org.hibernate.search.engine.SearchFactoryImplementor;
import org.infinispan.context.InvocationContext;
import org.infinispan.factories.annotations.Inject;

import javax.transaction.TransactionManager;

/**
 * <p/>
 * This class is an interceptor that will index data only if it has come from a local source.
 * <p/>
 * Currently, this is a property that is determined by setting "infinispan.query.indexLocalOnly" as a System property to
 * "true".
 *
 * @author Navin Surtani
 * @since 4.0
 */
public class LocalQueryInterceptor extends QueryInterceptor {

   @Inject
   public void init(SearchFactoryImplementor searchFactory, TransactionManager transactionManager) {

      log.debug("Entered LocalQueryInterceptor.init()");

      // Fields on superclass.

      this.searchFactory = searchFactory;
      this.transactionManager = transactionManager;
   }

   @Override
   protected boolean shouldModifyIndexes(InvocationContext ctx) {
      return ctx.isOriginLocal();   
   }
}
