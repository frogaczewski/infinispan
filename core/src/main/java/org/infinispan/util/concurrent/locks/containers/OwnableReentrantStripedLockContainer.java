/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2000 - 2008, Red Hat Middleware LLC, and individual contributors
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
package org.infinispan.util.concurrent.locks.containers;

import net.jcip.annotations.ThreadSafe;
import org.infinispan.context.InvocationContextContainer;
import org.infinispan.util.concurrent.locks.OwnableReentrantLock;

import java.util.Arrays;

/**
 * A LockContainer that holds {@link org.infinispan.util.concurrent.locks.OwnableReentrantLock}s.
 *
 * @author Manik Surtani (<a href="mailto:manik@jboss.org">manik@jboss.org</a>)
 * @see ReentrantStripedLockContainer
 * @see org.infinispan.util.concurrent.locks.OwnableReentrantLock
 * @since 4.0
 */
@ThreadSafe
public class OwnableReentrantStripedLockContainer extends AbstractStripedLockContainer {
   OwnableReentrantLock[] sharedLocks;
   InvocationContextContainer icc;

   /**
    * Creates a new LockContainer which uses a certain number of shared locks across all elements that need to be
    * locked.
    *
    * @param concurrencyLevel concurrency level for number of stripes to create.  Stripes are created in powers of two,
    *                         with a minimum of concurrencyLevel created.
    * @param icc              invocation context container to use
    */
   public OwnableReentrantStripedLockContainer(int concurrencyLevel, InvocationContextContainer icc) {
      this.icc = icc;
      initLocks(calculateNumberOfSegments(concurrencyLevel));
   }

   protected void initLocks(int numLocks) {
      sharedLocks = new OwnableReentrantLock[numLocks];
      for (int i = 0; i < numLocks; i++) sharedLocks[i] = new OwnableReentrantLock(icc);
   }

   public final OwnableReentrantLock getLock(Object object) {
      return sharedLocks[hashToIndex(object)];
   }

   public final boolean ownsLock(Object object, Object owner) {
      OwnableReentrantLock lock = getLock(object);
      return owner.equals(lock.getOwner());
   }

   public final boolean isLocked(Object object) {
      OwnableReentrantLock lock = getLock(object);
      return lock.isLocked();
   }

   public final int getNumLocksHeld() {
      int i = 0;
      for (OwnableReentrantLock l : sharedLocks) if (l.isLocked()) i++;
      return i;
   }

   public String toString() {
      return "OwnableReentrantStripedLockContainer{" +
            "sharedLocks=" + (sharedLocks == null ? null : Arrays.asList(sharedLocks)) +
            '}';
   }

   public int size() {
      return sharedLocks.length;
   }
}
