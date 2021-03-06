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
package org.infinispan.loaders.bdbje;

import java.io.File;

import org.infinispan.loaders.BaseCacheStoreFunctionalTest;
import org.infinispan.loaders.CacheStoreConfig;
import org.infinispan.test.TestingUtil;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

@Test(groups = "unit", enabled = true, testName = "loaders.bdbje.BdbjeCacheStoreFunctionalIntegrationTest")
public class BdbjeCacheStoreFunctionalIntegrationTest extends BaseCacheStoreFunctionalTest {

   private String tmpDirectory;

   @BeforeTest
   @Parameters({"basedir"})
   protected void setUpTempDir(@Optional("/tmp") String basedir) {
      tmpDirectory = TestingUtil.tmpDirectory(basedir, this);
   }
   
   @AfterClass
   protected void clearTempDir() {
      TestingUtil.recursiveFileRemove(tmpDirectory);
      new File(tmpDirectory).mkdirs();
   }
   
   @Override
   protected CacheStoreConfig createCacheStoreConfig() throws Exception {
      BdbjeCacheStoreConfig cfg = new BdbjeCacheStoreConfig();
      cfg.setLocation(tmpDirectory);
      cfg.setPurgeSynchronously(true);
      return cfg;
   }

}
