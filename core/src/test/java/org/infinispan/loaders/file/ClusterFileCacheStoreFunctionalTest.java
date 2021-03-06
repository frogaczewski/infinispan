package org.infinispan.loaders.file;

import org.infinispan.Cache;
import org.infinispan.atomic.AtomicMap;
import org.infinispan.atomic.AtomicMapLookup;
import org.infinispan.config.CacheLoaderManagerConfig;
import org.infinispan.config.Configuration;
import org.infinispan.config.GlobalConfiguration;
import org.infinispan.loaders.CacheStoreConfig;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.test.MultipleCacheManagersTest;
import org.infinispan.test.TestingUtil;
import org.infinispan.test.fwk.TestCacheManagerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import javax.transaction.TransactionManager;
import java.io.File;
import java.lang.reflect.Method;

/**
 * // TODO: Document this
 *
 * @author Galder Zamarreño
 * @since 4.1
 */
@Test(groups = "unit", testName = "loaders.file.ClusterFileCacheStoreFunctionalTest")
public class ClusterFileCacheStoreFunctionalTest extends MultipleCacheManagersTest {

   // createCacheManager executes before any @BeforeClass defined in the class, so simply use standard tmp folder.
   private String tmpDirectory = TestingUtil.tmpDirectory("/tmp", this);

   private Cache cache1, cache2;
   
   @AfterClass(alwaysRun = true)
   protected void clearTempDir() {
      TestingUtil.recursiveFileRemove(tmpDirectory);
      new File(tmpDirectory).mkdirs();
   }

   @Override
   protected void createCacheManagers() throws Throwable {
      EmbeddedCacheManager cacheManager1 = TestCacheManagerFactory.createCacheManager(GlobalConfiguration.getClusteredDefault(), new Configuration(), true);
      EmbeddedCacheManager cacheManager2 = TestCacheManagerFactory.createCacheManager(GlobalConfiguration.getClusteredDefault(), new Configuration(), true);
      registerCacheManager(cacheManager1, cacheManager2);

      Configuration config1 = getDefaultClusteredConfig(Configuration.CacheMode.REPL_SYNC);
      CacheLoaderManagerConfig clMngrConfig = new CacheLoaderManagerConfig();
      clMngrConfig.addCacheLoaderConfig(createCacheStoreConfig(1));
      config1.setCacheLoaderManagerConfig(clMngrConfig);

      Configuration config2 = getDefaultClusteredConfig(Configuration.CacheMode.REPL_SYNC);
      CacheLoaderManagerConfig clMngrConfig2 = new CacheLoaderManagerConfig();
      clMngrConfig2.addCacheLoaderConfig(createCacheStoreConfig(2));
      config2.setCacheLoaderManagerConfig(clMngrConfig2);

      cacheManager1.defineConfiguration("clusteredFileCacheStore", config1);
      cacheManager2.defineConfiguration("clusteredFileCacheStore", config2);
      cache1 = cache(0, "clusteredFileCacheStore");
      cache2 = cache(1, "clusteredFileCacheStore");
   }

   public void testRestoreTransactionalAtomicMap(Method m) throws Exception{
      TransactionManager tm = cache1.getAdvancedCache().getTransactionManager();
      tm.begin();
      final AtomicMap<String, String> map = AtomicMapLookup.getAtomicMap(cache1, m.getName());
      map.put("a", "b");
      tm.commit();

      //evict from memory
      cache1.evict(m.getName());

      // now re-retrieve the map and make sure we see the diffs
      assert AtomicMapLookup.getAtomicMap(cache1, m.getName()).get("a").equals("b");
      assert AtomicMapLookup.getAtomicMap(cache2, m.getName()).get("a").equals("b");

      cache2.evict(m.getName());
      assert AtomicMapLookup.getAtomicMap(cache1, m.getName()).get("a").equals("b");
      assert AtomicMapLookup.getAtomicMap(cache2, m.getName()).get("a").equals("b");
   }

   protected CacheStoreConfig createCacheStoreConfig(int index) throws Exception {
      FileCacheStoreConfig cfg = new FileCacheStoreConfig();
      cfg.setLocation(tmpDirectory + "/" + index);
      cfg.setPurgeSynchronously(true); // for more accurate unit testing
      return cfg;
   }

}
