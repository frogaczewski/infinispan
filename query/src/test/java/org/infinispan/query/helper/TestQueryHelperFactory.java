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
package org.infinispan.query.helper;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.util.Version;
import org.infinispan.Cache;
import org.infinispan.query.CacheQuery;
import org.infinispan.query.QueryFactory;
import org.infinispan.query.backend.QueryHelper;

import java.util.Properties;

/**
 * Creates a test query helper
 *
 * @author Manik Surtani
 * @author Sanne Grinovero
 * @since 4.0
 */
public class TestQueryHelperFactory {
   
   public static final Analyzer STANDARD_ANALYZER = new StandardAnalyzer(getLuceneVersion());
   
   public static QueryHelper createTestQueryHelperInstance(Cache<?, ?> cache, Class... classes) {
      if (cache == null) throw new NullPointerException("Cache should not be null!");
      Properties p = new Properties();
      p.setProperty("hibernate.search.default.directory_provider", "org.hibernate.search.store.RAMDirectoryProvider");
      return new QueryHelper(cache, p, classes);
   }
   
   public static QueryParser createQueryParser(String defaultFieldName) {
      return new QueryParser(getLuceneVersion(), defaultFieldName, STANDARD_ANALYZER);
   }
   
   public static Version getLuceneVersion() {
      return Version.LUCENE_30; //Change as needed
   }

   public static CacheQuery createCacheQuery(Cache m_cache, QueryHelper m_queryHelper, String fieldName, String searchString) throws ParseException {
      QueryFactory queryFactory = new QueryFactory(m_cache, m_queryHelper);
      CacheQuery cacheQuery = queryFactory.getBasicQuery(fieldName, searchString, getLuceneVersion());
      return cacheQuery;
   }
   
}
