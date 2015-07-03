package com.bufarini.reminders.collaboration;

import java.security.Provider;
import java.security.Security;
import java.util.Properties;

import javax.mail.Session;
import javax.mail.URLName;

import com.sun.mail.imap.IMAPSSLStore;
import com.sun.mail.imap.IMAPStore;
import com.sun.mail.smtp.SMTPTransport;

/**
 * Performs OAuth2 authentication.
 *
 * <p>Before using this class, you must call {@code initialize} to install the
 * OAuth2 SASL provider.
 */
public class OAuth2Authenticator
{
	private final static String EMPTY_PASSWORD = "";
	
	public static final class OAuth2Provider extends Provider {
		private static final long serialVersionUID = 1L;

		public OAuth2Provider() {
			super("Google OAuth2 Provider", 1.0, "Provides the XOAUTH2 SASL Mechanism");
			put("SaslClientFactory.XOAUTH2",
					"com.bufarini.reminders.collaboration.OAuth2SaslClientFactory");
		}
	}
	
	public static final OAuth2Provider OAUTH2_PROVIDER = new OAuth2Provider();

    /**
	* Installs the OAuth2 SASL provider. This must be called exactly once
	* before calling other methods on this class.
	*/
	public static void initialize() {
		Security.addProvider(OAUTH2_PROVIDER);
	}

  /**
   * Connects and authenticates to an IMAP server with OAuth2. You must have
   * called {@code initialize}.
   *
   * @param host Hostname of the imap server, for example {@code
   *     imap.googlemail.com}.
   * @param port Port of the imap server, for example 993.
   * @param userEmail Email address of the user to authenticate, for example
   *     {@code oauth@gmail.com}.
   * @param oauthToken The user's OAuth token.
   * @param debug Whether to enable debug logging on the IMAP connection.
   *
   * @return An authenticated IMAPStore that can be used for IMAP operations.
   */
	public static IMAPStore connectToImap(String host, int port, String userEmail,
			String oauthToken, boolean debug) throws Exception
	{
		Properties props = new Properties();
		props.put("mail.imaps.sasl.enable", "true");
		props.put("mail.imaps.sasl.mechanisms", "XOAUTH2");
		props.put(OAuth2SaslClientFactory.OAUTH_TOKEN_PROP, oauthToken);
		Session session = Session.getInstance(props);
		session.setDebug(debug);

		final URLName unusedUrlName = null;
		IMAPSSLStore store = new IMAPSSLStore(session, unusedUrlName);
		store.connect(host, port, userEmail, EMPTY_PASSWORD);
		
		return store;
	}

	public static class SMTPTransportInfo {
		public SMTPTransport smtpTransport;
		public Session session;
	}
	
  /**
   * Connects and authenticates to an SMTP server with OAuth2. You must have
   * called {@code initialize}.
   *
   * @param host Hostname of the smtp server, for example {@code
   *     smtp.googlemail.com}.
   * @param port Port of the smtp server, for example 587.
   * @param userEmail Email address of the user to authenticate, for example
   *     {@code oauth@gmail.com}.
   * @param oauthToken The user's OAuth token.
   * @param debug Whether to enable debug logging on the connection.
   *
   * @return An authenticated SMTPTransport that can be used for SMTP
   *     operations.
   */
	public static SMTPTransportInfo connectToSmtp(String host, int port, String userEmail,
			String oauthToken, boolean debug) throws Exception
	{
		Properties props = new Properties();
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.starttls.required", "true");
		props.put("mail.smtp.sasl.enable", "true");
		props.put("mail.smtp.sasl.mechanisms", "XOAUTH2");
		props.put(OAuth2SaslClientFactory.OAUTH_TOKEN_PROP, oauthToken);
		Session session = Session.getInstance(props);
		session.setDebug(debug);

		final URLName unusedUrlName = null;
		SMTPTransport transport = new SMTPTransport(session, unusedUrlName);
		// If the password is non-null, SMTP tries to do AUTH LOGIN.
		transport.connect(host, port, userEmail, EMPTY_PASSWORD);
		
		SMTPTransportInfo transportInfo = new SMTPTransportInfo();
		transportInfo.session = session;
		transportInfo.smtpTransport = transport;
		return transportInfo;
	}
}