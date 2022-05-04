package com.opuscapita.eespagateway.outbound;

import java.security.cert.X509Certificate;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.state.ESuccess;
import com.helger.commons.string.StringHelper;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.peppolid.factory.SimpleIdentifierFactory;
import com.helger.phase4.dynamicdiscovery.AS4EndpointDetailProviderBDXR;
import com.helger.phase4.dynamicdiscovery.AS4EndpointDetailProviderBDXR2;
import com.helger.phase4.dynamicdiscovery.AS4EndpointDetailProviderConstant;
import com.helger.phase4.dynamicdiscovery.IAS4EndpointDetailProvider;
import com.helger.phase4.sender.AbstractAS4UserMessageBuilderMIMEPayload;
import com.helger.phase4.util.Phase4Exception;
import com.helger.smpclient.bdxr1.IBDXRServiceMetadataProvider;
import com.helger.smpclient.bdxr2.IBDXR2ServiceMetadataProvider;
import com.helger.smpclient.url.BDXLURLProvider;
import com.helger.smpclient.url.IBDXLURLProvider;

/**
 * This class contains all the specifics to send AS4 messages with the EESPA
 * profile. See <code>sendAS4Message</code> as the main method to trigger the
 * sending, with all potential customization.
 */
@Immutable
public final class Phase4EESPASender
{
  public static final SimpleIdentifierFactory IF = SimpleIdentifierFactory.INSTANCE;
  public static final IBDXLURLProvider URL_PROVIDER = BDXLURLProvider.INSTANCE;

  private static final Logger LOGGER = LoggerFactory.getLogger (Phase4EESPASender.class);

  private Phase4EESPASender ()
  {}

  /**
   * @return Create a new Builder for AS4 messages if the payload is present.
   *         Never <code>null</code>.
   */
  @Nonnull
  public static EESPAUserMessageBuilder builder ()
  {
    return new EESPAUserMessageBuilder ();
  }

  /**
   * Abstract EESPA UserMessage builder class with sanity methods
   *
   * @author Philip Helger
   * @param <IMPLTYPE>
   *        The implementation type
   */
  @NotThreadSafe
  public abstract static class AbstractEESPAUserMessageBuilder <IMPLTYPE extends AbstractEESPAUserMessageBuilder <IMPLTYPE>> extends
                                                               AbstractAS4UserMessageBuilderMIMEPayload <IMPLTYPE>
  {
    protected IParticipantIdentifier m_aReceiverID;
    protected IDocumentTypeIdentifier m_aDocTypeID;
    protected IProcessIdentifier m_aProcessID;
    protected IAS4EndpointDetailProvider m_aEndpointDetailProvider;
    protected Consumer <X509Certificate> m_aCertificateConsumer;
    protected Consumer <String> m_aAPEndointURLConsumer;

    protected AbstractEESPAUserMessageBuilder ()
    {
      // Override default values
      try
      {
        httpClientFactory (new Phase4EESPAHttpClientSettings ());
      }
      catch (final Exception ex)
      {
        throw new IllegalStateException ("Failed to init AS4 Client builder", ex);
      }
    }

    /**
     * Set the receiver participant ID of the message. The participant ID must
     * be provided prior to sending. This ends up in the "finalRecipient"
     * UserMessage property.
     *
     * @param a
     *        The receiver participant ID. May not be <code>null</code>.
     * @return this for chaining
     */
    @Nonnull
    public final IMPLTYPE receiverParticipantID (@Nullable final IParticipantIdentifier a)
    {
      m_aReceiverID = a;
      return thisAsT ();
    }

    /**
     * Set the document type ID to be send. The document type must be provided
     * prior to sending.
     *
     * @param a
     *        The document type ID to be used. May not be <code>null</code>.
     * @return this for chaining
     */
    @Nonnull
    public final IMPLTYPE documentTypeID (@Nullable final IDocumentTypeIdentifier a)
    {
      m_aDocTypeID = a;
      return action (a == null ? null : a.getURIEncoded ());
    }

    /**
     * Set the process ID to be send. The process ID must be provided prior to
     * sending.
     *
     * @param a
     *        The process ID to be used. May not be <code>null</code>.
     * @return this for chaining
     */
    @Nonnull
    public final IMPLTYPE processID (@Nullable final IProcessIdentifier a)
    {
      m_aProcessID = a;
      return service (a == null ? null : a.getScheme (), a == null ? null : a.getValue ());
    }

    /**
     * Set the "from party ID". This is mandatory
     *
     * @param a
     *        The from party ID. May be <code>null</code>.
     * @return this for chaining
     */
    @Nonnull
    public final IMPLTYPE fromPartyID (@Nullable final IParticipantIdentifier a)
    {
      return fromPartyIDType (a == null ? null : a.getScheme ()).fromPartyID (a == null ? null : a.getValue ());
    }

    /**
     * Set the "to party ID". This is mandatory
     *
     * @param a
     *        The to party ID. May be <code>null</code>.
     * @return this for chaining
     */
    @Nonnull
    public final IMPLTYPE toPartyID (@Nullable final IParticipantIdentifier a)
    {
      return toPartyIDType (a == null ? null : a.getScheme ()).toPartyID (a == null ? null : a.getValue ());
    }

    /**
     * Set the abstract endpoint detail provider to be used. This can be an SMP
     * lookup routine or in certain test cases a predefined certificate and
     * endpoint URL.
     *
     * @param aEndpointDetailProvider
     *        The endpoint detail provider to be used. May be <code>null</code>.
     * @return this for chaining
     * @see #smpClient(IBDXRServiceMetadataProvider)
     */
    @Nonnull
    public final IMPLTYPE endpointDetailProvider (@Nullable final IAS4EndpointDetailProvider aEndpointDetailProvider)
    {
      m_aEndpointDetailProvider = aEndpointDetailProvider;
      return thisAsT ();
    }

    /**
     * Set the SMP v1 client to be used. This is the point where e.g. the
     * differentiation between SMK and SML can be done. This must be set prior
     * to sending.
     *
     * @param aSMPClient
     *        The SMP v1 client to be used. May not be <code>null</code>.
     * @return this for chaining
     * @see #endpointDetailProvider(IAS4EndpointDetailProvider)
     */
    @Nonnull
    public final IMPLTYPE smpClient (@Nonnull final IBDXRServiceMetadataProvider aSMPClient)
    {
      return endpointDetailProvider (new AS4EndpointDetailProviderBDXR (aSMPClient));
    }

    /**
     * Set the SMP v2 client to be used. This is the point where e.g. the
     * differentiation between SMK and SML can be done. This must be set prior
     * to sending.
     *
     * @param aSMPClient
     *        The SMP v2 client to be used. May not be <code>null</code>.
     * @return this for chaining
     * @see #endpointDetailProvider(IAS4EndpointDetailProvider)
     * @since 0.10.6
     */
    @Nonnull
    public final IMPLTYPE smpClient (@Nonnull final IBDXR2ServiceMetadataProvider aSMPClient)
    {
      return endpointDetailProvider (new AS4EndpointDetailProviderBDXR2 (aSMPClient));
    }

    @Nonnull
    public final IMPLTYPE receiverEndpointDetails (@Nonnull final X509Certificate aCert, @Nonnull @Nonempty final String sDestURL)
    {
      return endpointDetailProvider (new AS4EndpointDetailProviderConstant (aCert, sDestURL));
    }

    /**
     * Set an optional Consumer for the retrieved certificate, independent of
     * its usability.
     *
     * @param aCertificateConsumer
     *        The consumer to be used. May be <code>null</code>.
     * @return this for chaining
     */
    @Nonnull
    public final IMPLTYPE certificateConsumer (@Nullable final Consumer <X509Certificate> aCertificateConsumer)
    {
      m_aCertificateConsumer = aCertificateConsumer;
      return thisAsT ();
    }

    /**
     * Set an optional Consumer for the destination AP address, independent of
     * its usability.
     *
     * @param aAPEndointURLConsumer
     *        The consumer to be used. May be <code>null</code>.
     * @return this for chaining
     */
    @Nonnull
    public final IMPLTYPE endointURLConsumer (@Nullable final Consumer <String> aAPEndointURLConsumer)
    {
      m_aAPEndointURLConsumer = aAPEndointURLConsumer;
      return thisAsT ();
    }

    protected final boolean isEndpointDetailProviderUsable ()
    {
      if (m_aEndpointDetailProvider instanceof AS4EndpointDetailProviderConstant)
        return true;

      if (m_aReceiverID == null)
        return false;
      if (m_aDocTypeID == null)
        return false;
      if (m_aProcessID == null)
        return false;
      if (m_aEndpointDetailProvider == null)
        return false;

      return true;
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    protected ESuccess finishFields () throws Phase4Exception
    {
      if (!isEndpointDetailProviderUsable ())
      {
        LOGGER.error ("At least one mandatory field for endpoint discovery is not set and therefore the AS4 message cannot be send.");
        return ESuccess.FAILURE;
      }

      // e.g. SMP lookup (may throw an exception)
      m_aEndpointDetailProvider.init (m_aDocTypeID, m_aProcessID, m_aReceiverID);

      // Certificate from e.g. SMP lookup (may throw an exception)
      final X509Certificate aReceiverCert = m_aEndpointDetailProvider.getReceiverAPCertificate ();
      if (m_aCertificateConsumer != null)
        m_aCertificateConsumer.accept (aReceiverCert);
      receiverCertificate (aReceiverCert);

      // URL from e.g. SMP lookup (may throw an exception)
      final String sReceiverEndpointURL = m_aEndpointDetailProvider.getReceiverAPEndpointURL ();
      if (m_aAPEndointURLConsumer != null)
        m_aAPEndointURLConsumer.accept (sReceiverEndpointURL);
      endpointURL (sReceiverEndpointURL);

      // Call at the end
      return super.finishFields ();
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public boolean isEveryRequiredFieldSet ()
    {
      if (!super.isEveryRequiredFieldSet ())
        return false;

      if (m_aReceiverID == null)
      {
        LOGGER.warn ("The field 'receiverID' is not set");
        return false;
      }
      if (m_aDocTypeID == null && StringHelper.hasNoText (m_sAction))
      {
        LOGGER.warn ("Neither the field 'docTypeID' nor the field 'action' is set");
        return false;
      }
      if (m_aProcessID == null && StringHelper.hasNoText (m_sService))
      {
        LOGGER.warn ("Neither the field 'processID' nor the field 'service' is set");
        return false;
      }
      if (m_aEndpointDetailProvider == null)
      {
        LOGGER.warn ("The field 'endpointDetailProvider' is not set");
        return false;
      }
      // m_aCertificateConsumer is optional
      // m_aAPEndointURLConsumer is optional

      return true;
    }
  }

  /**
   * The builder class for sending AS4 messages using CEF profile specifics. Use
   * {@link #sendMessage()} or {@link #sendMessageAndCheckForReceipt()} to
   * trigger the main transmission.
   *
   * @author Philip Helger
   */
  @NotThreadSafe
  public static class EESPAUserMessageBuilder extends AbstractEESPAUserMessageBuilder <EESPAUserMessageBuilder>
  {
    public EESPAUserMessageBuilder ()
    {}
  }
}
