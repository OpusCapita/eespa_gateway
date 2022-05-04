package com.opuscapita.eespagateway.outbound;

import java.security.GeneralSecurityException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import com.helger.commons.ws.TrustManagerTrustAll;
import com.helger.http.tls.ETLSVersion;
import com.helger.httpclient.HttpClientSettings;
import com.helger.phase4.CAS4;
import com.helger.phase4.CAS4Version;

/**
 * Special {@link HttpClientSettings} with better defaults for EESPA.
 */
public class Phase4EESPAHttpClientSettings extends HttpClientSettings
{
  public static final int DEFAULT_CEF_CONNECTION_REQUEST_TIMEOUT_MS = 1_000;
  public static final int DEFAULT_CEF_CONNECTION_TIMEOUT_MS = 5_000;
  public static final int DEFAULT_CEF_SOCKET_TIMEOUT_MS = 100_000;

  public Phase4EESPAHttpClientSettings () throws GeneralSecurityException
  {
    // EESPA requires TLS v1.2
    final SSLContext aSSLContext = SSLContext.getInstance (ETLSVersion.TLS_12.getID (), ETLSVersion.TLS_13.getID ());
    // But we're basically trusting all hosts - the exact list is hard to
    // determine
    aSSLContext.init (null, new TrustManager [] { new TrustManagerTrustAll (false) }, null);
    setSSLContext (aSSLContext);

    setConnectionRequestTimeoutMS (DEFAULT_CEF_CONNECTION_REQUEST_TIMEOUT_MS);
    setConnectionTimeoutMS (DEFAULT_CEF_CONNECTION_TIMEOUT_MS);
    setSocketTimeoutMS (DEFAULT_CEF_SOCKET_TIMEOUT_MS);

    // Set an explicit user agent
    setUserAgent (CAS4.LIB_NAME + "/" + CAS4Version.BUILD_VERSION + " " + CAS4.LIB_URL);
  }
}
