package com.realtek.DLNA_DMP_1p5;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AUTH;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.MalformedChallengeException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.auth.DigestScheme;
import org.apache.http.impl.client.DefaultHttpClient;

import android.util.Log;


public class HttpRequest {

	private static String TAG = "HttpRequest";

	public static String sentGetMethod(String url) {
		DefaultHttpClient httpclient = new DefaultHttpClient();
		HttpGet httpget = new HttpGet(url);
		HttpResponse response = null;
		String responseBody = null;
		try {
			 response= httpclient.execute(httpget);
			 StringBuffer sb = new StringBuffer();
			 HttpEntity entity = response.getEntity();
			 InputStream is = entity.getContent();
			 BufferedReader br = new BufferedReader(new InputStreamReader(is,"Shift_JIS"));
			 String data = "";
			 while ((data = br.readLine()) != null) {
					sb.append(data);
				}
			responseBody = sb.toString();
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		
		return responseBody;
	}

	public static String sentGetMethod(String url, String userName,
			String password) {
		if(userName == null || password == null || userName.equals("") || password.equals(""))
			return null;
		DefaultHttpClient httpclient = new DefaultHttpClient();
		DefaultHttpClient httpclient2 = new DefaultHttpClient();
		HttpGet httpget = new HttpGet(url);
		String responseBody = null;
		try {
			// Initial request without credentials returns
			// "HTTP/1.1 401 Unauthorized"
			HttpResponse response = httpclient.execute(httpget);
			System.out.println(response.getStatusLine());
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
				// Get current current "WWW-Authenticate" header from response
				// WWW-Authenticate: Digest realm="realsil", algorithm="MD5",
				// qop="auth", nonce="264498dbb62a26ce5ea989f9531ef611"
				Header authHeader = response.getFirstHeader(AUTH.WWW_AUTH);
				// System.out.println("authHeader = " + authHeader);

				DigestScheme digestScheme = new DigestScheme();

				// Parse realm, nonce sent by server.
				digestScheme.processChallenge(authHeader);

				UsernamePasswordCredentials creds = new UsernamePasswordCredentials(
						userName, password);
				httpget.addHeader(digestScheme.authenticate(creds, httpget));

				response = httpclient2.execute(httpget);
				Log.e("HttpRequest", ""+response.getStatusLine());
				if(response.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED){
					return null;
				}
				HttpEntity entity = response.getEntity();
				InputStream is = entity.getContent();
				BufferedReader br = new BufferedReader(new InputStreamReader(is,"Shift_JIS"));
				String data = "";
				StringBuffer sb = new StringBuffer();
				while ((data = br.readLine()) != null) {
						sb.append(data);
				}
				responseBody = sb.toString();
			}
		} catch (MalformedChallengeException e) {
			e.printStackTrace();
			return null;
		} catch (AuthenticationException e) {
			e.printStackTrace();
			return null;
		} catch (ClientProtocolException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} finally {
			httpclient.getConnectionManager().shutdown();
			httpclient2.getConnectionManager().shutdown();
		}

		return responseBody;
	}

	}

