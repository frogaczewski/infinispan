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
package org.infinispan.remoting;

import org.infinispan.commands.CommandsFactory;
import org.infinispan.commands.ReplicableCommand;
import org.infinispan.commands.remote.MultipleRpcCommand;
import org.infinispan.config.Configuration;
import org.infinispan.factories.KnownComponentNames;
import org.infinispan.factories.annotations.ComponentName;
import org.infinispan.factories.annotations.Inject;
import org.infinispan.factories.annotations.Start;
import org.infinispan.factories.annotations.Stop;
import org.infinispan.lifecycle.Lifecycle;
import org.infinispan.remoting.rpc.ResponseMode;
import org.infinispan.remoting.rpc.RpcManager;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Periodically (or when certain size is exceeded) takes elements and replicates them.
 *
 * @author <a href="mailto:bela@jboss.org">Bela Ban</a>
 * @author Mircea.Markus@jboss.com
 * @since 4.0
 */
public interface ReplicationQueue extends Lifecycle {


   /**
    * @return true if this replication queue is enabled, false otherwise.
    */
   boolean isEnabled();

   /**
    * Adds a new command to the replication queue.
    *
    * @param job command to add to the queue
    */
   void add(ReplicableCommand job);

   /**
    * Flushes existing jobs in the replication queue, and returns the number of jobs flushed.
    * @return the number of jobs flushed
    */
   int flush();

   /**
    * @return the number of elements in the replication queue.
    */
   int getElementsCount();

   /**
    * Resets the replication queue, typically used when a cache is restarted.
    */
   void reset();
}
