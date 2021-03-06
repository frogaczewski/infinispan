package org.infinispan.transaction.xa;

import org.infinispan.config.Configuration;
import org.infinispan.context.InvocationContextContainer;
import org.infinispan.transaction.TransactionCoordinator;
import org.infinispan.transaction.TransactionTable;
import org.infinispan.transaction.xa.recovery.RecoveryManager;
import org.infinispan.transaction.xa.recovery.SerializableXid;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

/**
 * This acts both as an local {@link org.infinispan.transaction.xa.CacheTransaction} and implementor of an {@link
 * javax.transaction.xa.XAResource} that will be called by tx manager on various tx stages.
 *
 * @author Mircea.Markus@jboss.com
 * @since 4.0
 */
public class TransactionXaAdapter implements XAResource {

   private static final Log log = LogFactory.getLog(TransactionXaAdapter.class);
   private static boolean trace = log.isTraceEnabled();

   private int txTimeout;

   private final InvocationContextContainer icc;
   private final Configuration configuration;

   private final XaTransactionTable txTable;

   private final TransactionCoordinator txCoordinator;

   /**
    * XAResource is associated with a transaction between enlistment (XAResource.start()) XAResource.end(). It's only the
    * boundary methods (prepare, commit, rollback) that need to be "stateless".
    * Reefer to section 3.4.4 from JTA spec v.1.1
    */
   private final LocalXaTransaction localTransaction;

   private final RecoveryManager recoveryManager;

   private volatile RecoveryManager.RecoveryIterator recoveryIterator;


   public TransactionXaAdapter(LocalXaTransaction localTransaction, TransactionTable txTable,
                               Configuration configuration, InvocationContextContainer icc, RecoveryManager rm, TransactionCoordinator txCoordinator) {
      this.localTransaction = localTransaction;
      this.txTable = (XaTransactionTable) txTable;
      this.configuration = configuration;
      this.icc = icc;
      this.recoveryManager = rm;
      this.txCoordinator = txCoordinator;
   }

   /**
    * This can be call for any transaction object. See Section 3.4.6 (Resource Sharing) from JTA spec v1.1.
    */
   public int prepare(Xid externalXid) throws XAException {
      Xid xid = convertXid(externalXid);
      LocalXaTransaction localTransaction = getLocalTransactionAndValidate(xid);
      return txCoordinator.prepare(localTransaction);
   }

   /**
    * Same comment as for {@link #prepare(javax.transaction.xa.Xid)} applies for commit.
    */
   public void commit(Xid externalXid, boolean isOnePhase) throws XAException {
      Xid xid = convertXid(externalXid);
      LocalXaTransaction localTransaction = getLocalTransactionAndValidate(xid);

      if (trace) log.trace("Committing transaction %s", localTransaction.getGlobalTransaction());
      try {
         txCoordinator.commit(localTransaction, isOnePhase);
         forgetSuccessfullyCompletedTransaction(recoveryManager, xid, localTransaction);
      } finally {
         cleanupImpl(localTransaction, txTable, icc);
      }
   }

   /**
    * Same comment as for {@link #prepare(javax.transaction.xa.Xid)} applies for commit.
    */   
   public void rollback(Xid externalXid) throws XAException {
      Xid xid = convertXid(externalXid);
      LocalXaTransaction localTransaction1 = getLocalTransactionAndValidateImpl(xid, txTable);
      txCoordinator.rollback(localTransaction1);
      forgetSuccessfullyCompletedTransaction(recoveryManager, xid, localTransaction1);
   }

   public void start(Xid externalXid, int i) throws XAException {
      Xid xid = convertXid(externalXid);
      //transform in our internal format in order to be able to serialize
      localTransaction.setXid(xid);
      txTable.addLocalTransactionMapping(localTransaction);
      if (trace) log.trace("start called on tx " + this.localTransaction.getGlobalTransaction());
   }

   public void end(Xid externalXid, int i) throws XAException {
      if (trace) log.trace("end called on tx " + this.localTransaction.getGlobalTransaction());
   }

   public void forget(Xid externalXid) throws XAException {
      Xid xid = convertXid(externalXid);
      if (trace) log.trace("forget called for xid %s", xid);
      try {
         recoveryManager.removeRecoveryInformation(null, xid, true);
      } catch (Exception e) {
         log.warn("Exception removing recovery information: ", e);
         throw new XAException(XAException.XAER_RMERR);
      }
   }

   public int getTransactionTimeout() throws XAException {
      if (trace) log.trace("start called");
      return txTimeout;
   }

   public boolean isSameRM(XAResource xaResource) throws XAException {
      if (!(xaResource instanceof TransactionXaAdapter)) {
         return false;
      }
      TransactionXaAdapter other = (TransactionXaAdapter) xaResource;
      return other.equals(this);
   }

   public Xid[] recover(int flag) throws XAException {
      if (!configuration.isTransactionRecoveryEnabled()) {
         log.warn("Recovery call will be ignored as recovery is disabled. More on recovery: http://community.jboss.org/docs/DOC-16646");
         return RecoveryManager.RecoveryIterator.NOTHING;
      }
      if (trace) log.trace("recover called: " + flag);

      if (isFlag(flag, TMSTARTRSCAN)) {
         recoveryIterator = recoveryManager.getPreparedTransactionsFromCluster();
         if (trace) log.trace("Fetched a new recovery iterator: %s" , recoveryIterator);
      }
      if (isFlag(flag, TMENDRSCAN)) {
         if (log.isTraceEnabled()) log.trace("Flushing the iterator");
         return recoveryIterator.all();
      } else {
         //as per the spec: "TMNOFLAGS this flag must be used when no other flags are specified."
         if (!isFlag(flag, TMSTARTRSCAN) && !isFlag(flag, TMNOFLAGS))
            throw new IllegalArgumentException("TMNOFLAGS this flag must be used when no other flags are specified." +
                                                     " Received " + flag);
         return recoveryIterator.hasNext() ? recoveryIterator.next() : RecoveryManager.RecoveryIterator.NOTHING;
      }
   }

   private boolean isFlag(int value, int flag) {
      return (value & flag) != 0;
   }

   public boolean setTransactionTimeout(int i) throws XAException {
      this.txTimeout = i;
      return true;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof TransactionXaAdapter)) return false;
      TransactionXaAdapter that = (TransactionXaAdapter) o;
      return this.localTransaction.equals(that.localTransaction);
   }

   @Override
   public int hashCode() {
      return localTransaction.getGlobalTransaction().hashCode();
   }

   @Override
   public String toString() {
      return "TransactionXaAdapter{" +
            "localTransaction=" + localTransaction +
            '}';
   }

   private static void cleanupImpl(LocalXaTransaction localTransaction, TransactionTable txTable, InvocationContextContainer icc) {
      txTable.removeLocalTransaction(localTransaction);
      icc.suspend();
   }

   private void forgetSuccessfullyCompletedTransaction(RecoveryManager recoveryManager, Xid xid, LocalXaTransaction localTransaction) {
      if (configuration.isTransactionRecoveryEnabled()) {
         recoveryManager.removeRecoveryInformation(localTransaction.getRemoteLocksAcquired(), xid, false);
      }
   }

   private LocalXaTransaction getLocalTransactionAndValidate(Xid xid) throws XAException {
      return getLocalTransactionAndValidateImpl(xid, txTable);
   }

   private static LocalXaTransaction getLocalTransactionAndValidateImpl(Xid xid, XaTransactionTable txTable) throws XAException {
      LocalXaTransaction localTransaction = txTable.getLocalTransaction(xid);
      if  (localTransaction == null) {
         if (trace) log.trace("no tx found for %s", xid);
         throw new XAException(XAException.XAER_NOTA);
      }
      return localTransaction;
   }

   public LocalXaTransaction getLocalTransaction() {
      return localTransaction;
   }

   /**
    * Only does the conversion if recovery is enabled.
    */
   private Xid convertXid(Xid externalXid) {
      if (configuration.isTransactionRecoveryEnabled() && (!(externalXid instanceof SerializableXid))) {
         return new SerializableXid(externalXid);
      } else {
         return externalXid;
      }
   }
}
