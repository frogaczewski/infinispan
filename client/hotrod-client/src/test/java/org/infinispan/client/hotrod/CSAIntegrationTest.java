package org.infinispan.client.hotrod;

import org.infinispan.Cache;
import org.infinispan.client.hotrod.impl.transport.tcp.TcpTransport;
import org.infinispan.client.hotrod.impl.transport.tcp.TcpTransportFactory;
import org.infinispan.config.Configuration;
import org.infinispan.container.DataContainer;
import org.infinispan.distribution.DistributionManager;
import org.infinispan.lifecycle.ComponentStatus;
import org.infinispan.manager.CacheContainer;
import org.infinispan.marshall.Marshaller;
import org.infinispan.marshall.jboss.GenericJBossMarshaller;
import org.infinispan.server.hotrod.HotRodServer;
import org.infinispan.test.TestingUtil;
import org.infinispan.util.ByteArrayKey;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

/**
 * @author Mircea.Markus@jboss.com
 * @since 4.1
 */
@Test(groups = "functional", testName = "client.hotrod.CSAIntegrationTest")
public class CSAIntegrationTest extends HitsAwareCacheManagersTest {

   private HotRodServer hotRodServer1;
   private HotRodServer hotRodServer2;
   private HotRodServer hotRodServer3;
   private RemoteCacheManager remoteCacheManager;
   private RemoteCache<Object, Object> remoteCache;
   private TcpTransportFactory tcpConnectionFactory;

   private static final Log log = LogFactory.getLog(CSAIntegrationTest.class);

   private Marshaller m;

   @BeforeTest
   public void createMarshaller() {
      m = new GenericJBossMarshaller();
   }

   @AfterTest   
   public void destroyMarshaller() {
      m = null;
   }

   @AfterMethod
   @Override
   protected void clearContent() throws Throwable {
   }

   @Override
   protected void createCacheManagers() throws Throwable {
      Configuration config = getDefaultClusteredConfig(Configuration.CacheMode.DIST_SYNC);
      config.setNumOwners(1);
      config.setUnsafeUnreliableReturnValues(true);
      addClusterEnabledCacheManager(config);
      addClusterEnabledCacheManager(config);
      addClusterEnabledCacheManager(config);

      hotRodServer1 = TestHelper.startHotRodServer(manager(0));
      hrServ2CacheManager.put(new InetSocketAddress(hotRodServer1.getHost(), hotRodServer1.getPort()), manager(0));
      hotRodServer2 = TestHelper.startHotRodServer(manager(1));
      hrServ2CacheManager.put(new InetSocketAddress(hotRodServer2.getHost(), hotRodServer2.getPort()), manager(1));
      hotRodServer3 = TestHelper.startHotRodServer(manager(2));
      hrServ2CacheManager.put(new InetSocketAddress(hotRodServer3.getHost(), hotRodServer3.getPort()), manager(2));

      assert manager(0).getCache() != null;
      assert manager(1).getCache() != null;
      assert manager(2).getCache() != null;

      TestingUtil.blockUntilViewReceived(manager(0).getCache(), 3, 10000);
      TestingUtil.blockUntilCacheStatusAchieved(manager(0).getCache(), ComponentStatus.RUNNING, 10000);
      TestingUtil.blockUntilCacheStatusAchieved(manager(1).getCache(), ComponentStatus.RUNNING, 10000);
      TestingUtil.blockUntilCacheStatusAchieved(manager(2).getCache(), ComponentStatus.RUNNING, 10000);

      manager(0).getCache().put("k", "v");
      manager(0).getCache().get("k").equals("v");
      manager(1).getCache().get("k").equals("v");
      manager(2).getCache().get("k").equals("v");

      log.info("Local replication test passed!");

      //Important: this only connects to one of the two servers!
      Properties props = new Properties();
      props.put("infinispan.client.hotrod.server_list", "localhost:" + hotRodServer2.getPort() + ";localhost:" + hotRodServer2.getPort());
      props.put("infinispan.client.hotrod.ping_on_startup", "false");
      remoteCacheManager = new RemoteCacheManager(props);
      remoteCache = remoteCacheManager.getCache();

      tcpConnectionFactory = (TcpTransportFactory) TestingUtil.extractField(remoteCacheManager, "transportFactory");
   }

   @AfterClass
   @Override
   protected void destroy() {
      super.destroy();
      remoteCacheManager.stop();
   }

   public void testHashInfoRetrieved() throws InterruptedException {
      assert tcpConnectionFactory.getServers().size() == 1;
      for (int i = 0; i < 10; i++) {
         remoteCache.put("k", "v");
         if (tcpConnectionFactory.getServers().size() == 3) break;
         Thread.sleep(1000);
      }
      assertEquals(3, tcpConnectionFactory.getServers().size());
      assertNotNull(tcpConnectionFactory.getConsistentHash());
   }

   @Test(dependsOnMethods = "testHashInfoRetrieved")
   public void testCorrectSetup() {
      remoteCache.put("k", "v");
      assert remoteCache.get("k").equals("v");
   }


   @Test(dependsOnMethods = "testCorrectSetup")
   public void testHashFunctionReturnsSameValues() {
      for (int i = 0; i < 1000; i++) {
         byte[] key = generateKey(i);
         TcpTransport transport = (TcpTransport) tcpConnectionFactory.getTransport(key);
         InetSocketAddress serverAddress = transport.getServerAddress();
         CacheContainer cacheContainer = hrServ2CacheManager.get(serverAddress);
         assertNotNull("For server address " + serverAddress + " found " + cacheContainer + ". Map is: " + hrServ2CacheManager, cacheContainer);
         DistributionManager distributionManager = cacheContainer.getCache().getAdvancedCache().getDistributionManager();
         assert distributionManager.getLocality(key).isLocal();
         tcpConnectionFactory.releaseTransport(transport);
      }
   }

   @Test(dependsOnMethods = "testHashFunctionReturnsSameValues")
   public void testRequestsGoToExpectedServer() throws Exception {
      addInterceptors();
      List<byte[]> keys = new ArrayList<byte[]>();
      for (int i = 0; i < 500; i++) {
         byte[] key = generateKey(i);
         keys.add(key);
         String keyStr = new String(key);
         remoteCache.put(keyStr, "value");
         byte[] keyBytes = m.objectToByteBuffer(keyStr);
         TcpTransport transport = (TcpTransport) tcpConnectionFactory.getTransport(keyBytes);
         assertCacheContainsKey(transport.getServerAddress(), keyBytes);
         tcpConnectionFactory.releaseTransport(transport);
      }

      log.info("Right before first get.");

      for (byte[] key : keys) {
         resetStats();
         String keyStr = new String(key);
         assert remoteCache.get(keyStr).equals("value");
         byte[] keyBytes = m.objectToByteBuffer(keyStr);
         TcpTransport transport = (TcpTransport) tcpConnectionFactory.getTransport(keyBytes);
         assertOnlyServerHit(transport.getServerAddress());
         tcpConnectionFactory.releaseTransport(transport);
      }
   }

   private void assertCacheContainsKey(InetSocketAddress serverAddress, byte[] keyBytes) {
      CacheContainer cacheContainer = hrServ2CacheManager.get(serverAddress);
      Cache<Object, Object> cache = cacheContainer.getCache();
      DataContainer dataContainer = cache.getAdvancedCache().getDataContainer();
      assert dataContainer.keySet().contains(new ByteArrayKey(keyBytes));
   }

   private byte[] generateKey(int i) {
      Random r = new Random();
      byte[] result = new byte[i];
      r.nextBytes(result);
      return result;
   }
}
