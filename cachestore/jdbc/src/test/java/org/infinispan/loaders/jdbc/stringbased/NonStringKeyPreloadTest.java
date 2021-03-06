package org.infinispan.loaders.jdbc.stringbased;

import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.CacheException;
import org.infinispan.config.CacheLoaderManagerConfig;
import org.infinispan.config.Configuration;
import org.infinispan.eviction.EvictionStrategy;
import org.infinispan.loaders.CacheLoaderException;
import org.infinispan.loaders.jdbc.TableManipulation;
import org.infinispan.loaders.jdbc.connectionfactory.ConnectionFactory;
import org.infinispan.loaders.jdbc.connectionfactory.ConnectionFactoryConfig;
import org.infinispan.loaders.jdbc.connectionfactory.PooledConnectionFactory;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.test.AbstractInfinispanTest;
import org.infinispan.test.fwk.TestCacheManagerFactory;
import org.infinispan.test.fwk.UnitTestDatabaseManager;
import org.testng.annotations.Test;

import java.sql.Connection;

import static junit.framework.Assert.assertEquals;

/**
 * Tester for https://jira.jboss.org/browse/ISPN-579.
 *
 * @author Mircea.Markus@jboss.com
 * @since 4.1
 */
@Test(groups = "functional", testName = "loaders.jdbc.stringbased.AbstractInfinispanTest")
public class NonStringKeyPreloadTest extends AbstractInfinispanTest {

   public void testPreloadWithKey2StringMapper() throws Exception {
      String mapperName = PersonKey2StringMapper.class.getName();
      Configuration cfg = createCacheStoreConfig(mapperName, false, true);

      EmbeddedCacheManager cacheManager = TestCacheManagerFactory.createCacheManager(cfg);
      try {
         cacheManager.getCache();
         assert false : " Preload with Key2StringMapper is not supported. Specify an TwoWayKey2StringMapper if you want to support it (or disable preload).";
      } catch (CacheException ce) {
         //expected
      } finally {
         cacheManager.stop();
      }
   }

   public void testPreloadWithTwoWayKey2StringMapper() throws Exception {
      String mapperName = TwoWayPersonKey2StringMapper.class.getName();
      Configuration config = createCacheStoreConfig(mapperName, true, true);
      EmbeddedCacheManager cm = TestCacheManagerFactory.createCacheManager(config);
      Cache<Object, Object> cache = cm.getCache();
      Person mircea = new Person("Markus", "Mircea", 30);
      cache.put(mircea, "me");
      Person dan = new Person("Dan", "Dude", 30);
      cache.put(dan, "mate");
      cache.stop();
      cm.stop();

      cm = TestCacheManagerFactory.createCacheManager(config);
      try {
         cache = cm.getCache();
         assert cache.containsKey(mircea);
         assert cache.containsKey(dan);
      } finally {
         cache.stop();
         cm.stop();
      }
   }
   public void testPreloadWithTwoWayKey2StringMapperAndBoundedCache() throws Exception {
      String mapperName = TwoWayPersonKey2StringMapper.class.getName();
      Configuration config = createCacheStoreConfig(mapperName, true, true);
      config.setEvictionStrategy(EvictionStrategy.FIFO);
      config.setEvictionMaxEntries(3);
      EmbeddedCacheManager cm = TestCacheManagerFactory.createCacheManager(config);
      AdvancedCache<Object, Object> cache = cm.getCache().getAdvancedCache();
      for (int i = 0; i < 10; i++) {
         Person p = new Person("name" + i, "surname" + i, 30);
         cache.put(p, "" + i);
      }
      cache.stop();
      cm.stop();

      cm = TestCacheManagerFactory.createCacheManager(config);
      try {
         cache = cm.getCache().getAdvancedCache();
         assertEquals(3, cache.size());
         int found = 0;
         for (int i = 0; i < 10; i++) {
            Person p = new Person("name" + i, "surname" + i, 30);
            if (cache.getDataContainer().containsKey(p)) {
               found++;
            }
         }
         assertEquals(3, found);
      } finally {
         cache.stop();
         cm.stop();
      }
   }

   static Configuration createCacheStoreConfig(String mapperName, boolean wrap, boolean preload) {
      ConnectionFactoryConfig connectionFactoryConfig = UnitTestDatabaseManager.getUniqueConnectionFactoryConfig();
      if (wrap) {
         connectionFactoryConfig.setConnectionFactoryClass(SharedConnectionFactory.class.getName());
      }
      TableManipulation tm = UnitTestDatabaseManager.buildDefaultTableManipulation();
      JdbcStringBasedCacheStoreConfig csConfig = new JdbcStringBasedCacheStoreConfig(connectionFactoryConfig, tm);
      csConfig.setFetchPersistentState(true);
      csConfig.setKey2StringMapperClass(mapperName);

      CacheLoaderManagerConfig cacheLoaders = new CacheLoaderManagerConfig();
      cacheLoaders.setPreload(preload);
      cacheLoaders.addCacheLoaderConfig(csConfig);
      Configuration cfg = TestCacheManagerFactory.getDefaultConfiguration(false);
      cfg.setCacheLoaderManagerConfig(cacheLoaders);
      return cfg;
   }

   public static class SharedConnectionFactory extends ConnectionFactory {
      static PooledConnectionFactory sharedFactory;
      static boolean started = false;

      @Override
      public void start(ConnectionFactoryConfig config) throws CacheLoaderException {
         if (!started) {
            sharedFactory = new PooledConnectionFactory();
            sharedFactory.start(config);
         }
      }

      @Override
      public void stop() {
         //ignore
      }

      @Override
      public Connection getConnection() throws CacheLoaderException {
         return sharedFactory.getConnection();
      }

      @Override
      public void releaseConnection(Connection conn) {
         sharedFactory.releaseConnection(conn);
      }
   }
}
