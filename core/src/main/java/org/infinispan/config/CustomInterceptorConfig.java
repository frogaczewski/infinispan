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
package org.infinispan.config;

import org.infinispan.interceptors.base.CommandInterceptor;
import org.infinispan.util.TypedProperties;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import java.util.Locale;
import java.util.Properties;

/**
 * Holds information about the custom interceptors defined in the configuration file.
 *
 *
 * @author Mircea.Markus@jboss.com
 * @author Vladimir Blagojevic
 * @since 4.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name="interceptor")
@ConfigurationDoc(name="interceptor")
public class CustomInterceptorConfig extends AbstractNamedCacheConfigurationBean {

   /** The serialVersionUID */
   private static final long serialVersionUID = 6206233611032238190L;

   @XmlTransient
   protected CommandInterceptor interceptor;

   @XmlTransient
   protected boolean isFirst;

   @XmlTransient
   protected boolean isLast;

   @XmlAttribute
   @ConfigurationDocRef(name="class", bean=CustomInterceptorConfig.class,targetElement="setIndex")
   protected Integer index = -1;
   
   @XmlAttribute
   @ConfigurationDocRef(name="class", bean=CustomInterceptorConfig.class,targetElement="setAfterInterceptor")
   protected String after;

   @XmlAttribute
   @ConfigurationDocRef(name="class", bean=CustomInterceptorConfig.class,targetElement="setBeforeInterceptor")
   protected String before;

   @XmlAttribute
   @ConfigurationDocRef(name="class", bean=CustomInterceptorConfig.class,targetElement="setPosition")
   protected Position position;   
   
   @XmlAttribute(name="class")
   @ConfigurationDocRef(name="class", bean=CustomInterceptorConfig.class,targetElement="setClassName")
   protected String className;

   @XmlElement
   private TypedProperties properties = EMPTY_PROPERTIES;

   public CustomInterceptorConfig() {
      super();
      this.isFirst = true;
      overriddenConfigurationElements.add("isFirst");
   }

   /**
    * Builds a custom interceptor configuration.
    *
    * @param interceptor interceptor instance, already initialized with all attributes specified in the configuration
    * @param first       true if you wan this to be the first interceptor in the chain
    * @param last        true if you wan this to be the last interceptor in the chain
    * @param index       an absolute position within the interceptor chain
    * @param after       if you want this interceptor immediately after the specified class in the chain
    * @param before      immediately before the specified class in the chain
    */
   public CustomInterceptorConfig(CommandInterceptor interceptor, boolean first, boolean last, int index,
                                  String after, String before) {
      this.interceptor = interceptor;
      isFirst = first;
      isLast = last;
      this.index = index;
      this.after = after;
      this.before = before;
      if (interceptor != null) overriddenConfigurationElements.add("interceptor");

      // No way to tell here, unfortunately...
      overriddenConfigurationElements.add("isFirst");
      overriddenConfigurationElements.add("isLast");

      if (index > -1) overriddenConfigurationElements.add("index");

      if (after != null && after.length() > 0) overriddenConfigurationElements.add("after");
      if (before != null && before.length() > 0) overriddenConfigurationElements.add("before");
   }

   /**
    * Builds a custom interceptor configuration.
    *
    * @param interceptor interceptor instance, already initialized with all attributes specified in the configuration
    * @param first       true if you wan this to be the first interceptor in the chain
    * @param last        true if you wan this to be the last interceptor in the chain
    * @param index       an absolute position within the interceptor chain
    * @param after       if you want this interceptor immediately after the specified class in the chain
    * @param before      immediately before the specified class in the chain
    */
   public CustomInterceptorConfig(CommandInterceptor interceptor, boolean first, boolean last, int index,
                                  Class<? extends CommandInterceptor> after, Class<? extends CommandInterceptor> before) {
      this.interceptor = interceptor;
      isFirst = first;
      isLast = last;
      this.index = index;
      if (interceptor != null) overriddenConfigurationElements.add("interceptor");

      // No way to tell here, unfortunately...
      overriddenConfigurationElements.add("isFirst");
      overriddenConfigurationElements.add("isLast");

      if (index > -1) overriddenConfigurationElements.add("index");

      if (after != null) {
         this.after = after.getName();
         overriddenConfigurationElements.add("after");
      }

      if (before != null) {
         this.before = before.getName();
         overriddenConfigurationElements.add("before");
      }
   }

   /**
    * Constructs an interceptor config based on the supplied interceptor instance.
    *
    * @param interceptor
    */
   public CustomInterceptorConfig(CommandInterceptor interceptor) {
      this();
      this.interceptor = interceptor;
      overriddenConfigurationElements.add("interceptor");
   }
   
   public Properties getProperties() {
      return properties;
   }
   
   public void setProperties(Properties properties) {
      this.properties = toTypedProperties(properties);
      testImmutability("properties");
   }

   public Position getPosition() {
      return position;
   }

   /**
    * A position at which to place this interceptor in the chain. FIRST is the first interceptor
    * encountered when an invocation is made on the cache, LAST is the last interceptor before the
    * call is passed on to the data structure. Note that this attribute is mutually exclusive with
    * 'before', 'after' and 'index'.
    * 
    * @param position
    */
   public void setPosition(Position position) {
      this.position = position;
      testImmutability("position");
   }

   public String getClassName() {
      return className;
   }

   /**
    * Fully qualified interceptor class name which must extend org.infinispan.interceptors.base.CommandInterceptor.
    * @param className
    */
   public void setClassName(String className) {
      this.className = className;
      testImmutability("className");
   }

   /**
    * Shall this interceptor be the first one in the chain?
    */

   public void setFirst(boolean first) {
      testImmutability("first");
      isFirst = first;
   }

   /**
    * Shall this interceptor be the last one in the chain?
    */
   public void setLast(boolean last) {
      testImmutability("last");
      isLast = last;
   }
   
   /**
    * A position at which to place this interceptor in the chain. FIRST is the first interceptor
    * encountered when an invocation is made on the cache, LAST is the last interceptor before the
    * call is passed on to the data structure. Note that this attribute is mutually exclusive with
    * 'before', 'after' and 'index'.
    * 
    * @param pos
    */
   public void setPosition(String pos) {
      setPosition(Position.valueOf(uc(pos)));
   }

   
   /**
    * A position at which to place this interceptor in the chain, with 0 being the first position.
    * Note that this attribute is mutually exclusive with 'position', 'before' and 'after'."
    * 
    * @param index
    */
   public void setIndex(int index) {
      testImmutability("index");
      this.index = index;
   }

   
   /**
    * Places the new interceptor directly after the instance of the named interceptor which is
    * specified via its fully qualified class name. Note that this attribute is mutually exclusive
    * with 'position', 'before' and 'index'.
    * 
    * @param afterClass
    */
   public void setAfterInterceptor(String afterClass) {
      testImmutability("after");
      this.after = afterClass;
   }

   /**
    * Places the new interceptor directly after the instance of the named interceptor which is
    * specified via its fully qualified class name. Note that this attribute is mutually exclusive
    * with 'position', 'before' and 'index'.
    * 
    * @param interceptorClass
    */
   public void setAfterInterceptor(Class<? extends CommandInterceptor> interceptorClass) {
      setAfterInterceptor(interceptorClass.getName());
   }

   /**
    * Places the new interceptor directly before the instance of the named interceptor which is
    * specified via its fully qualified class name.. Note that this attribute is mutually exclusive
    * with 'position', 'after' and 'index'."
    * 
    * @param beforeClass
    */
   public void setBeforeInterceptor(String beforeClass) {
      testImmutability("before");
      this.before = beforeClass;
   }

   /**
    * Places the new interceptor directly before the instance of the named interceptor which is
    * specified via its fully qualified class name.. Note that this attribute is mutually exclusive
    * with 'position', 'after' and 'index'."
    * 
    * @param interceptorClass
    */
   public void setBeforeInterceptor(Class<? extends CommandInterceptor> interceptorClass) {
      setBeforeInterceptor(interceptorClass.getName());
   }

   /**
    * Returns a the interceptor that we want to add to the chain.
    */
   public CommandInterceptor getInterceptor() {
      return interceptor;
   }
   
   /**
    * Returns a the interceptor that we want to add to the chain.
    */
   public void setInterceptor(CommandInterceptor interceptor) {
      testImmutability("interceptor");
      this.interceptor = interceptor;
   }

   /**
    * @see #setFirst(boolean)
    */
   public boolean isFirst() {
      return isFirst;
   }

   /**
    * @see #setLast(boolean)
    */
   public boolean isLast() {
      return isLast;
   }

   /**
    * @see #getIndex()
    */
   public int getIndex() {
      return index;
   }

   /**
    * @see #getAfter()
    */
   public String getAfter() {
      return after;
   }

   /**
    * @see #getBefore()
    */
   public String getBefore() {
      return before;
   }

   public String toString() {
      return "CustomInterceptorConfig{" +
            "interceptor='" + interceptor + '\'' +
            ", isFirst=" + isFirst +
            ", isLast=" + isLast +
            ", index=" + index +
            ", after='" + after + '\'' +
            ", before='" + before + '\'' +
            '}';
   }

   public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof CustomInterceptorConfig)) return false;

      CustomInterceptorConfig that = (CustomInterceptorConfig) o;

      if (index != null && !index.equals(that.index)) return false;
      if (isFirst != that.isFirst) return false;
      if (isLast != that.isLast) return false;
      if (after != null ? !after.equals(that.after) : that.after != null) return false;
      if (before != null ? !before.equals(that.before) : that.before != null) return false;
      if (interceptor != null ? !interceptor.equals(that.interceptor) : that.interceptor != null)
         return false;
      return true;
   }

   public int hashCode() {
      int result;
      result = (interceptor != null ? interceptor.hashCode() : 0);
      result = 31 * result + (isFirst ? 1 : 0);
      result = 31 * result + (isLast ? 1 : 0);
      result = 31 * result + index;
      result = 31 * result + (after != null ? after.hashCode() : 0);
      result = 31 * result + (before != null ? before.hashCode() : 0);
      return result;
   }

   @Override
   public CustomInterceptorConfig clone() throws CloneNotSupportedException {
      CustomInterceptorConfig dolly = (CustomInterceptorConfig) super.clone();
      if (properties != null) dolly.properties = (TypedProperties) properties.clone();
      return dolly;
   }
   
   protected String uc(String s) {
      return s == null ? null : s.toUpperCase(Locale.ENGLISH);
   }
   
   enum Position {
      FIRST,LAST
   }

   public void accept(ConfigurationBeanVisitor v) {
      v.visitCustomInterceptorConfig(this);
   }
}
