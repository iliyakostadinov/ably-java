package io.ably.test.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;

import io.ably.rest.AblyRest;
import io.ably.rest.Auth.AuthMethod;
import io.ably.rest.Auth.AuthOptions;
import io.ably.rest.Auth.TokenCallback;
import io.ably.rest.Auth.TokenDetails;
import io.ably.rest.Auth.TokenParams;
import io.ably.rest.Auth.TokenRequest;
import io.ably.test.rest.RestSetup.TestVars;
import io.ably.test.util.TokenServer;
import io.ably.types.AblyException;
import io.ably.types.ClientOptions;
import io.ably.types.Param;

import org.json.JSONObject;
import org.junit.Test;

public class RestAuth {

	/**
	 * Init library with a key only
	 */
	@Test
	public void authinit0() {
		try {
			TestVars testVars = RestSetup.getTestVars();
			AblyRest ably = new AblyRest(testVars.keys[0].keyStr);
			assertEquals("Unexpected Auth method mismatch", ably.auth.getAuthMethod(), AuthMethod.basic);
		} catch (AblyException e) {
			e.printStackTrace();
			fail("authinit0: Unexpected exception instantiating library");
		}
	}

	/**
	 * Check that an exception is thrown if a basic Auth request is made over HTTP (RSA1)
	 */
	@Test
	public void auth_basic_request_over_http() {
		try {
			ClientOptions opts = new ClientOptions("sample:key");
			opts.tls = false;
			AblyRest ably = new AblyRest(opts);
			
			try {
				ably.time();
			} catch (AblyException e) {
				return;
			}
			fail("auth_basic_request_over_http: Basic Auth must not be usable over non-TLS connections.");
		} catch (AblyException e) {
			e.printStackTrace();
			fail("auth_basic_request_over_http: Unexpected exception instantiating library");
		}
	}
	
	/**
	 * Init library with a token only
	 */
	@Test
	public void authinit1() {
		try {
			ClientOptions opts = new ClientOptions();
			opts.token = "this_is_not_really_a_token";
			AblyRest ably = new AblyRest(opts);
			assertEquals("Unexpected Auth method mismatch", ably.auth.getAuthMethod(), AuthMethod.token);
		} catch (AblyException e) {
			e.printStackTrace();
			fail("authinit1: Unexpected exception instantiating library");
		}
	}

	/**
	 * Init library with a token callback
	 */
	private boolean authinit2_cbCalled;
	@Test
	public void authinit2() {
		try {
			TestVars testVars = RestSetup.getTestVars();
			ClientOptions opts = new ClientOptions();
			opts.restHost = testVars.host;
			opts.port = testVars.port;
			opts.tlsPort = testVars.tlsPort;
			opts.tls = testVars.tls;
			opts.authCallback = new TokenCallback() {
				@Override
				public String getTokenRequest(TokenParams params) throws AblyException {
					authinit2_cbCalled = true;
					return "this_is_not_really_a_token_request";
				}};
			AblyRest ably = new AblyRest(opts);
			/* make a call to trigger token request */
			try {
				ably.stats(null);
			} catch(Throwable t) {}
			assertEquals("Unexpected Auth method mismatch", ably.auth.getAuthMethod(), AuthMethod.token);
			assertTrue("Token callback not called", authinit2_cbCalled);
		} catch (AblyException e) {
			e.printStackTrace();
			fail("authinit2: Unexpected exception instantiating library");
		}
	}

	/**
	 * Init library with a key and clientId; expect token auth to be chosen
	 */
	@Test
	public void authinit3() {
		try {
			TestVars testVars = RestSetup.getTestVars();
			ClientOptions opts = new ClientOptions(testVars.keys[0].keyStr);
			opts.clientId = "testClientId";
			AblyRest ably = new AblyRest(opts);
			assertEquals("Unexpected Auth method mismatch", ably.auth.getAuthMethod(), AuthMethod.token);
		} catch (AblyException e) {
			e.printStackTrace();
			fail("authinit3: Unexpected exception instantiating library");
		}
	}

	/**
	 * Init library with a token
	 */
	@Test
	public void authinit4() {
		try {
			TestVars testVars = RestSetup.getTestVars();
			ClientOptions optsForToken = new ClientOptions(testVars.keys[0].keyStr);
			optsForToken.restHost = testVars.host;
			optsForToken.port = testVars.port;
			optsForToken.tlsPort = testVars.tlsPort;
			optsForToken.tls = testVars.tls;
			AblyRest ablyForToken = new AblyRest(optsForToken);
			TokenDetails tokenDetails = ablyForToken.auth.requestToken(null, null);
			assertNotNull("Expected token value", tokenDetails.token);
			ClientOptions opts = new ClientOptions();
			opts.token = tokenDetails.token;
			opts.restHost = testVars.host;
			opts.port = testVars.port;
			opts.tls = testVars.tls;
			AblyRest ably = new AblyRest(opts);
			assertEquals("Unexpected Auth method mismatch", ably.auth.getAuthMethod(), AuthMethod.token);
		} catch (AblyException e) {
			e.printStackTrace();
			fail("authinit3: Unexpected exception instantiating library");
		}
	}

	/**
	 * Init token server
	 */
	@Test
	public void auth_start_tokenserver() {
		try {
			TestVars testVars = RestSetup.getTestVars();
			ClientOptions opts = testVars.createOptions(testVars.keys[0].keyStr);
			AblyRest ably = new AblyRest(opts);
			tokenServer = new TokenServer(ably, 8982);
			tokenServer.start();
		} catch (IOException e) {
			e.printStackTrace();
			fail("auth_start_tokenserver: Unexpected exception starting server");
		} catch (AblyException e) {
			e.printStackTrace();
			fail("auth_start_tokenserver: Unexpected exception starting server");
		}
	}

	/**
	 * Verify authURL called and handled when returning token request
	 */
	@Test
	public void auth_authURL_tokenrequest() {
		try {
			TestVars testVars = RestSetup.getTestVars();
			ClientOptions opts = new ClientOptions();
			opts.restHost = testVars.host;
			opts.port = testVars.port;
			opts.tlsPort = testVars.tlsPort;
			opts.tls = testVars.tls;
			opts.authUrl = "http://localhost:8982/get-token-request";
			AblyRest ably = new AblyRest(opts);
			/* make a call to trigger token request */
			try {
				TokenDetails tokenDetails = ably.auth.requestToken(null, null);
				assertNotNull("Expected token value", tokenDetails.token);
			} catch (AblyException e) {
				e.printStackTrace();
				fail("auth_authURL_tokenrequest: Unexpected exception requesting token");
			}
		} catch (AblyException e) {
			e.printStackTrace();
			fail("auth_authURL_tokenrequest: Unexpected exception instantiating library");
		}
	}

	/**
	 * Verify authURL called and handled when returning token
	 */
	@Test
	public void auth_authURL_token() {
		try {
			TestVars testVars = RestSetup.getTestVars();
			ClientOptions opts = new ClientOptions();
			opts.restHost = testVars.host;
			opts.port = testVars.port;
			opts.tlsPort = testVars.tlsPort;
			opts.tls = testVars.tls;
			opts.authUrl = "http://localhost:8982/get-token";
			AblyRest ably = new AblyRest(opts);
			/* make a call to trigger token request */
			try {
				TokenDetails tokenDetails = ably.auth.requestToken(null, null);
				assertNotNull("Expected token value", tokenDetails.token);
			} catch (AblyException e) {
				e.printStackTrace();
				fail("auth_authURL_token: Unexpected exception requesting token");
			}
		} catch (AblyException e) {
			e.printStackTrace();
			fail("auth_authURL_token: Unexpected exception instantiating library");
		}
	}

	/**
	 * Verify authURL called and handled when returning error
	 */
	@Test
	public void auth_authURL_err() {
		try {
			TestVars testVars = RestSetup.getTestVars();
			ClientOptions opts = new ClientOptions();
			opts.restHost = testVars.host;
			opts.port = testVars.port;
			opts.tlsPort = testVars.tlsPort;
			opts.tls = testVars.tls;
			opts.authUrl = "http://localhost:8982/404";
			AblyRest ably = new AblyRest(opts);
			/* make a call to trigger token request */
			try {
				ably.auth.requestToken(null, null);
				fail("auth_authURL_err: Unexpected success requesting token");
			} catch (AblyException e) {
				assertEquals("Expected forwarded error code", e.errorInfo.code, 40170);
			}
		} catch (AblyException e) {
			e.printStackTrace();
			fail("auth_authURL_token: Unexpected exception instantiating library");
		}
	}

	/**
	 * Verify authURL is passed specified params
	 */
	@Test
	public void auth_authURL_params() {
		try {
			TestVars testVars = RestSetup.getTestVars();
			ClientOptions opts = new ClientOptions();
			opts.restHost = testVars.host;
			opts.port = testVars.port;
			opts.tlsPort = testVars.tlsPort;
			opts.tls = testVars.tls;
			opts.authUrl = "http://localhost:8982/echo-params";
			opts.authParams = new Param[]{new Param("test-param", "test-value")};
			AblyRest ably = new AblyRest(opts);
			/* make a call to trigger token request */
			try {
				ably.auth.requestToken(null, null);
				fail("auth_authURL_params: Unexpected success requesting token");
			} catch (AblyException e) {
				assertEquals("Expected forwarded error code", e.errorInfo.code, 40170);
				JSONObject jsonParams = e.errorInfo.getRawJSON();
				assertTrue("Expected JSON error info", jsonParams != null);
				assertEquals("Expected echoed param", "test-value", jsonParams.optString("test-param"));
			}
		} catch (AblyException e) {
			e.printStackTrace();
			fail("auth_authURL_params: Unexpected exception instantiating library");
		}
	}

	/**
	 * Verify authURL is passed specified headers
	 */
	@Test
	public void auth_authURL_headers() {
		try {
			TestVars testVars = RestSetup.getTestVars();
			ClientOptions opts = new ClientOptions();
			opts.restHost = testVars.host;
			opts.port = testVars.port;
			opts.tlsPort = testVars.tlsPort;
			opts.tls = testVars.tls;
			opts.authUrl = "http://localhost:8982/echo-headers";
			opts.authHeaders = new Param[]{new Param("test-header", "test-value")};
			AblyRest ably = new AblyRest(opts);
			/* make a call to trigger token request */
			try {
				ably.auth.requestToken(null, null);
				fail("auth_authURL_headers: Unexpected success requesting token");
			} catch (AblyException e) {
				assertEquals("Expected forwarded error code", e.errorInfo.code, 40170);
				JSONObject jsonParams = e.errorInfo.getRawJSON();
				assertTrue("Expected JSON error info", jsonParams != null);
				assertEquals("Expected echoed header", "test-value", jsonParams.optString("test-header"));
			}
		} catch (AblyException e) {
			e.printStackTrace();
			fail("auth_authURL_headers: Unexpected exception instantiating library");
		}
	}
	
	/**
	 * Verify Auth#createTokenRequest signature and argument usage (RSA9h)		
	 */
	@Test
	public void auth_createtokenrequest_sig_args() {
		TestVars testVars = RestSetup.getTestVars();
		try {
			ClientOptions opts = testVars.createOptions("myKey:1");
			opts.clientId = "cid";
			AblyRest ably = new AblyRest(opts);
			
			//test optional params
			ably.auth.createTokenRequest(null, null);
			
			//test param and option overwriting
			AuthOptions authOpt = new AuthOptions();
			authOpt.key = "myNewKey:2";
			
			TokenParams params = new TokenParams();
			params.clientId = "cid_2";
			params.ttl = 64L;
			
			TokenRequest tokenRequest = ably.auth.createTokenRequest(params, authOpt);
			assertEquals(
					"The keyName in the returned token is different than the one supplied in the AuthOptions argument",
					"myNewKey", tokenRequest.keyName);
			assertEquals(
					"The clientId in the returned token is different than the one supplied in the TokenParams argument",
					"cid_2", tokenRequest.clientId);
			assertEquals(
					"The ttl in the returned token is different than the one supplied in the TokenParams argument",
					64L, tokenRequest.ttl);
		} catch (AblyException e) {
			e.printStackTrace();
			fail("unexpected exception while verifying Auth#createTokenRequest method's signature");
		}
	}

	/**
	 * Verify that Auth#createTokenRequest generates a 16+ character nonce (RSA9c)
	 */
	@Test
	public void auth_createtokenrequest_nonce() {
		TestVars testVars = RestSetup.getTestVars();
		try {
			ClientOptions opts = testVars.createOptions(testVars.keys[0].keyStr);
			AblyRest ably = new AblyRest(opts);
			TokenRequest tokenRequest = ably.auth.createTokenRequest(null, new TokenParams());
			if (tokenRequest.nonce.length() < 16) {
				fail("generated nonce is less than 16 characters");
			}
		} catch (AblyException e) {
			e.printStackTrace();
			fail("unexpected exception during auth_createtokenrequest_nonce");
		}
	}
	
	/**
	 * Kill token server
	 */
	@Test
	public void auth_stop_tokenserver() {
		if(tokenServer != null)
			tokenServer.stop();
	}

	/**
	 * Verify authCallback called and handled when returning token request
	 */
	@Test
	public void auth_authcallback_tokenrequest() {
		try {
			final TestVars testVars = RestSetup.getTestVars();

			/* implement callback, using Ably instance with key */
			TokenCallback authCallback = new TokenCallback() {
				private AblyRest ably = new AblyRest(testVars.createOptions(testVars.keys[0].keyStr));
				@Override
				public Object getTokenRequest(TokenParams params) throws AblyException {
					return ably.auth.createTokenRequest(null, params);
				}
			};

			/* create Ably instance without key */
			ClientOptions opts = testVars.createOptions();
			opts.authCallback = authCallback;
			AblyRest ably = new AblyRest(opts);

			/* make a call to trigger token request */
			try {
				TokenDetails tokenDetails = ably.auth.requestToken(null, null);
				assertNotNull("Expected token value", tokenDetails.token);
			} catch (AblyException e) {
				e.printStackTrace();
				fail("auth_authURL_tokenrequest: Unexpected exception requesting token");
			}
		} catch (AblyException e) {
			e.printStackTrace();
			fail("auth_authURL_tokenrequest: Unexpected exception instantiating library");
		}
	}

	/**
	 * Verify authCallback called and handled when returning token
	 */
	@Test
	public void auth_authcallback_token() {
		try {
			final TestVars testVars = RestSetup.getTestVars();

			/* implement callback, using Ably instance with key */
			TokenCallback authCallback = new TokenCallback() {
				private AblyRest ably = new AblyRest(testVars.createOptions(testVars.keys[0].keyStr));
				@Override
				public Object getTokenRequest(TokenParams params) throws AblyException {
					return ably.auth.requestToken(null, params);
				}
			};

			/* create Ably instance without key */
			ClientOptions opts = testVars.createOptions();
			opts.authCallback = authCallback;
			AblyRest ably = new AblyRest(opts);

			/* make a call to trigger token request */
			try {
				TokenDetails tokenDetails = ably.auth.requestToken(null, null);
				assertNotNull("Expected token value", tokenDetails.token);
			} catch (AblyException e) {
				e.printStackTrace();
				fail("auth_authURL_tokenrequest: Unexpected exception requesting token");
			}
		} catch (AblyException e) {
			e.printStackTrace();
			fail("auth_authURL_tokenrequest: Unexpected exception instantiating library");
		}
	}

	/**
	 * Verify authCallback called when token expires
	 */
	@Test
	public void auth_authcallback_token_expire() {
		try {
			final TestVars testVars = RestSetup.getTestVars();
			ClientOptions optsForToken = testVars.createOptions(testVars.keys[0].keyStr);
			final AblyRest ablyForToken = new AblyRest(optsForToken);
			TokenDetails tokenDetails = ablyForToken.auth.requestToken(null, new TokenParams() {{ ttl = 5000L; }});
			assertNotNull("Expected token value", tokenDetails.token);

			/* implement callback, using Ably instance with key */
			final class TokenGenerator implements TokenCallback {
				@Override
				public Object getTokenRequest(TokenParams params) throws AblyException {
					++cbCount;
					return ablyForToken.auth.requestToken(null, params);
				}
				public int getCbCount() { return cbCount; }
				private int cbCount = 0;
			};

			TokenGenerator authCallback = new TokenGenerator();

			/* create Ably instance without key */
			ClientOptions opts = testVars.createOptions();
			opts.token = tokenDetails.token;
			opts.authCallback = authCallback;
			AblyRest ably = new AblyRest(opts);

			/* wait until token expires */
			try {
				Thread.sleep(6000L);
			} catch(InterruptedException ie) {}

			/* make a request that relies on the token */
			try {
				ably.stats(new Param[] { new Param("by", "hour"), new Param("limit", "1") });
			} catch (AblyException e) {
				e.printStackTrace();
				fail("auth_authURL_tokenrequest: Unexpected exception requesting token");
			}

			/* verify that the auth callback was called */
			assertEquals("Expected token generator to be called", 1, authCallback.getCbCount());
		} catch (AblyException e) {
			e.printStackTrace();
			fail("auth_authURL_tokenrequest: Unexpected exception instantiating library");
		}
	}

	/**
	 * Verify authCallback called and handled when returning error
	 */
	@Test
	public void auth_authcallback_err() {
		try {
			final TestVars testVars = RestSetup.getTestVars();

			/* implement callback, using Ably instance with key */
			TokenCallback authCallback = new TokenCallback() {
				@Override
				public Object getTokenRequest(TokenParams params) throws AblyException {
					throw new AblyException("test exception", 404, 0);
				}
			};

			/* create Ably instance without key */
			ClientOptions opts = testVars.createOptions();
			opts.authCallback = authCallback;
			AblyRest ably = new AblyRest(opts);

			/* make a call to trigger token request */
			try {
				ably.auth.requestToken(null, null);
				fail("auth_authURL_err: Unexpected success requesting token");
			} catch (AblyException e) {
				assertEquals("Expected forwarded error code", e.errorInfo.code, 40170);
			}
		} catch (AblyException e) {
			e.printStackTrace();
			fail("auth_authURL_token: Unexpected exception instantiating library");
		}
	}

	private TokenServer tokenServer;
}
