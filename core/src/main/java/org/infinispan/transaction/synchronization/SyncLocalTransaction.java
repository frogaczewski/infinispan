package org.infinispan.transaction.synchronization;

import org.infinispan.transaction.LocalTransaction;
import org.infinispan.transaction.xa.GlobalTransaction;

import javax.transaction.Transaction;

/**
 * {@link LocalTransaction} implementation to be used with {@link SynchronizationAdapter}.
 *
 * @author Mircea.Markus@jboss.com
 * @since 5.0
 */
public class SyncLocalTransaction extends LocalTransaction {

   public SyncLocalTransaction(Transaction transaction, GlobalTransaction tx) {
      super(transaction, tx);
   }

   private boolean enlisted;

   @Override
   public boolean isEnlisted() {
      return enlisted;
   }

   public void setEnlisted(boolean enlisted) {
      this.enlisted = enlisted;
   }
}
