/*
 * Copyright (C) 2021-2022 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.opuscapita.eespagateway.inbound;

import java.io.File;
import java.security.KeyStore;

import javax.activation.CommandMap;
import javax.annotation.Nonnull;
import javax.annotation.PreDestroy;
import javax.servlet.ServletContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.helger.commons.debug.GlobalDebug;
import com.helger.commons.exception.InitializationException;
import com.helger.commons.mime.CMimeType;
import com.helger.commons.string.StringHelper;
import com.helger.httpclient.HttpDebugger;
import com.helger.phase4.config.AS4Configuration;
import com.helger.phase4.crypto.AS4CryptoFactoryProperties;
import com.helger.phase4.profile.eespa.AS4EESPAProfileRegistarSPI;
import com.helger.phase4.servlet.AS4ServerInitializer;
import com.helger.phase4.servlet.AS4Servlet;
import com.helger.phase4.servlet.mgr.AS4ProfileSelector;
import com.helger.photon.app.io.WebFileIO;
import com.helger.servlet.ServletHelper;
import com.helger.web.scope.mgr.WebScopeManager;
import com.helger.xservlet.requesttrack.RequestTrackerSettings;

@Configuration
public class ServletConfig
{
  private static final Logger LOGGER = LoggerFactory.getLogger (ServletConfig.class);

  @Bean
  public ServletRegistrationBean <AS4Servlet> servletRegistrationBean (final ServletContext ctx)
  {
    // Must be called BEFORE the servlet is instantiated
    _init (ctx);
    final ServletRegistrationBean <AS4Servlet> bean = new ServletRegistrationBean <> (new AS4Servlet (), true, "/as4");
    bean.setLoadOnStartup (1);
    return bean;
  }

  private void _init (@Nonnull final ServletContext aSC)
  {
    // Do it only once
    if (!WebScopeManager.isGlobalScopePresent ())
    {
      WebScopeManager.onGlobalBegin (aSC);
      _initGlobalSettings (aSC);
      _initAS4 ();
      _initCrypto ();
    }
  }

  private static void _initGlobalSettings (@Nonnull final ServletContext aSC)
  {
    // Logging: JUL to SLF4J
    SLF4JBridgeHandler.removeHandlersForRootLogger ();
    SLF4JBridgeHandler.install ();

    if (GlobalDebug.isDebugMode ())
    {
      RequestTrackerSettings.setLongRunningRequestsCheckEnabled (false);
      RequestTrackerSettings.setParallelRunningRequestsCheckEnabled (false);
    }

    HttpDebugger.setEnabled (false);

    // Sanity check
    if (CommandMap.getDefaultCommandMap ().createDataContentHandler (CMimeType.MULTIPART_RELATED.getAsString ()) == null)
      throw new IllegalStateException ("No DataContentHandler for MIME Type '" +
                                       CMimeType.MULTIPART_RELATED.getAsString () +
                                       "' is available. There seems to be a problem with the dependencies/packaging");

    // Enforce Peppol profile usage
    AS4ProfileSelector.setCustomAS4ProfileID (AS4EESPAProfileRegistarSPI.AS4_PROFILE_ID_ACCEPTANCE);

    // Init the data path
    {
      // Get the ServletContext base path
      final String sServletContextPath = ServletHelper.getServletContextBasePath (aSC);
      // Get the data path
      final String sDataPath = AS4Configuration.getDataPath ();
      if (StringHelper.hasNoText (sDataPath))
        throw new InitializationException ("No data path was provided!");
      final boolean bFileAccessCheck = false;
      // Init the IO layer
      WebFileIO.initPaths (new File (sDataPath).getAbsoluteFile (), sServletContextPath, bFileAccessCheck);
    }
  }

  private static void _initAS4 ()
  {
    AS4ServerInitializer.initAS4Server ();
  }

  private static void _initCrypto ()
  {
    // Check if crypto properties are okay
    final KeyStore aKS = AS4CryptoFactoryProperties.getDefaultInstance ().getKeyStore ();
    if (aKS == null)
      throw new InitializationException ("Failed to load configured Keystore");
    LOGGER.info ("Successfully loaded configured key store from the crypto factory");

    final KeyStore.PrivateKeyEntry aPKE = AS4CryptoFactoryProperties.getDefaultInstance ().getPrivateKeyEntry ();
    if (aPKE == null)
      throw new InitializationException ("Failed to load configured private key");
    LOGGER.info ("Successfully loaded configured private key from the crypto factory");
  }

  @PreDestroy
  public void destroy ()
  {
    if (WebScopeManager.isGlobalScopePresent ())
    {
      AS4ServerInitializer.shutdownAS4Server ();
      WebScopeManager.onGlobalEnd ();
    }
  }
}
