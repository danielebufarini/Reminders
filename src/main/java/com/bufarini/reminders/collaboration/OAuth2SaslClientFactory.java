package com.bufarini.reminders.collaboration;

import android.util.Log;
import myjavax.security.auth.callback.CallbackHandler;
import myjavax.security.sasl.SaslClient;
import myjavax.security.sasl.SaslClientFactory;

import java.util.Map;

/*A SaslClientFactory that returns instances of OAuth2SaslClient.
 *
 * <p>Only the "XOAUTH2" mechanism is supported. The {@code callbackHandler} is
 * passed to the OAuth2SaslClient. Other parameters are ignored.
 * */

public class OAuth2SaslClientFactory implements SaslClientFactory {
	private static final String LOGTAG = OAuth2SaslClientFactory.class.getSimpleName();
	private static final String[] MECHANISM_NAMES = new String[] { "XOAUTH2" };
	
	public static final String OAUTH_TOKEN_PROP = "mail.imaps.sasl.mechanisms.oauth2.oauthToken";

	@Override
	public SaslClient createSaslClient(String[] mechanisms, String authorizationId,
			String protocol, String serverName, Map<String, ?> props,
			CallbackHandler callbackHandler)
	{
		boolean matchedMechanism = false;
		for (int i = 0; i < mechanisms.length; ++i) {
			if ("XOAUTH2".equalsIgnoreCase(mechanisms[i])) {
				matchedMechanism = true;
				break;
			}
		}
		if (!matchedMechanism) {
			Log.i(LOGTAG, "Failed to match any mechanisms");
			return null;
		}
		return new OAuth2SaslClient((String) props.get(OAUTH_TOKEN_PROP), callbackHandler);
	}

	public String[] getMechanismNames(Map<String, ?> props) {
		return MECHANISM_NAMES;
	}
}