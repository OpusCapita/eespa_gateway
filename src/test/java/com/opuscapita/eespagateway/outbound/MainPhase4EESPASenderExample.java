package com.opuscapita.eespagateway.outbound;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.io.file.SimpleFileIO;
import com.helger.peppol.sml.ISMLInfo;
import com.helger.peppol.sml.SMLInfo;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.simple.participant.SimpleParticipantIdentifier;
import com.helger.phase4.attachment.Phase4OutgoingAttachment;
import com.helger.phase4.client.IAS4ClientBuildMessageCallback;
import com.helger.phase4.dump.AS4DumpManager;
import com.helger.phase4.dump.AS4IncomingDumperFileBased;
import com.helger.phase4.dump.AS4OutgoingDumperFileBased;
import com.helger.phase4.messaging.domain.AS4UserMessage;
import com.helger.phase4.messaging.domain.AbstractAS4Message;
import com.helger.phase4.profile.eespa.EESPAPMode;
import com.helger.phase4.sender.AbstractAS4UserMessageBuilder.ESimpleUserMessageSendResult;
import com.helger.servlet.mock.MockServletContext;
import com.helger.smpclient.bdxr1.BDXRClientReadOnly;
import com.helger.web.scope.mgr.WebScopeManager;
import com.helger.web.scope.mgr.WebScoped;
import com.opuscapita.eespagateway.outbound.Phase4EESPASender.EESPAUserMessageBuilder;

/**
 * Example for sending something
 */
public final class MainPhase4EESPASenderExample
{
  private static final Logger LOGGER = LoggerFactory.getLogger (MainPhase4EESPASenderExample.class);

  // The SML information for EESPA
  private static final ISMLInfo SMK_EESPA = new SMLInfo ("eespa",
                                                         "SMK EESPA",
                                                         "eespa.acc.edelivery.tech.ec.europa.eu.",
                                                         "https://acc.edelivery.tech.ec.europa.eu/edelivery-sml",
                                                         true);

  public static void main (final String [] args)
  {
    WebScopeManager.onGlobalBegin (MockServletContext.create ());

    // Dump (for debugging purpose only)
    AS4DumpManager.setIncomingDumper (new AS4IncomingDumperFileBased ());
    AS4DumpManager.setOutgoingDumper (new AS4OutgoingDumperFileBased ());

    try (final WebScoped w = new WebScoped ())
    {
      // This is the SBDH test file to be send
      final byte [] aPayloadBytes = SimpleFileIO.getAllFileBytes (new File ("src/test/resources/examples/base-example.xml"));
      if (aPayloadBytes == null)
        throw new IllegalStateException ();

      // Start configuring here
      final IParticipantIdentifier aReceiverID = Phase4EESPASender.IF.createParticipantIdentifier ("iso6523-actorid-upis", "9915:tooptest");
      // This is a demo only
      final IAS4ClientBuildMessageCallback aBuildMessageCallback = new IAS4ClientBuildMessageCallback ()
      {
        public void onAS4Message (final AbstractAS4Message <?> aMsg)
        {
          final AS4UserMessage aUserMsg = (AS4UserMessage) aMsg;
          LOGGER.info ("Sending out AS4 message with message ID '" +
                       aUserMsg.getEbms3UserMessage ().getMessageInfo ().getMessageId () +
                       "'");
          LOGGER.info ("Sending out AS4 message with conversation ID '" +
                       aUserMsg.getEbms3UserMessage ().getCollaborationInfo ().getConversationId () +
                       "'");
        }
      };
      final EESPAUserMessageBuilder aBuilder = Phase4EESPASender.builder ();
      aBuilder
              // EESPA specific - TODO
              .documentTypeID (Phase4EESPASender.IF.createDocumentTypeIdentifier ("toop-doctypeid-qns",
                                                                                  "urn:eu:toop:ns:dataexchange-1p40::Response##urn:eu.toop.response.registeredorganization::1.40"))
              // EESPA specific - TODO
              .processID (Phase4EESPASender.IF.createProcessIdentifier ("toop-procid-agreement", "urn:eu.toop.process.datarequestresponse"))
              // C4 - TODO
              .receiverParticipantID (aReceiverID)
              // C2 - TODO
              .fromPartyID (new SimpleParticipantIdentifier (EESPAPMode.DEFAULT_PARTY_TYPE_ID, "POP000306"))
              // C3 - TODO
              .toPartyID (new SimpleParticipantIdentifier (EESPAPMode.DEFAULT_PARTY_TYPE_ID, "POP000306"))
              // What to send - TODO
              .payload (Phase4OutgoingAttachment.builder ().data (aPayloadBytes).mimeTypeXML ())
              // SMP client lookup
              .smpClient (new BDXRClientReadOnly (Phase4EESPASender.URL_PROVIDER, aReceiverID, SMK_EESPA))
              // Invoke a callback on message building
              .buildMessageCallback (aBuildMessageCallback);

      // Lookup and send
      final ESimpleUserMessageSendResult eResult = aBuilder.sendMessageAndCheckForReceipt ();
      if (eResult.isSuccess ())
      {
        LOGGER.info ("Successfully sent CEF message via AS4");
      }
      else
      {
        LOGGER.error ("Failed to send CEF message via AS4");
      }
    }
    catch (final Exception ex)
    {
      LOGGER.error ("Error sending CEF message via AS4", ex);
    }
    finally
    {
      WebScopeManager.onGlobalEnd ();
    }
  }
}
