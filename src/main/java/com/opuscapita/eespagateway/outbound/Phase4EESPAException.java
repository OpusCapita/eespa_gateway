package com.opuscapita.eespagateway.outbound;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.phase4.util.Phase4Exception;

/**
 * Generic exception to be thrown from the phase4 EESPA sender.
 */
public class Phase4EESPAException extends Phase4Exception
{
  /**
   * @param sMessage
   *        Error message
   */
  public Phase4EESPAException (@Nonnull final String sMessage)
  {
    super (sMessage);
  }

  /**
   * @param aCause
   *        Optional causing exception
   * @since 0.13.0
   */
  public Phase4EESPAException (@Nullable final Throwable aCause)
  {
    super (aCause);
  }

  /**
   * @param sMessage
   *        Error message
   * @param aCause
   *        Optional causing exception
   */
  public Phase4EESPAException (@Nonnull final String sMessage, @Nullable final Throwable aCause)
  {
    super (sMessage, aCause);
  }
}
