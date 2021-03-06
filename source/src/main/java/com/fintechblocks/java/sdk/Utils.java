package com.fintechblocks.java.sdk;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

public final class Utils {

  public static void setFormUrlParameters(HttpURLConnection connection, HashMap<String, String> params)
      throws UnsupportedEncodingException {
    try {
      StringBuilder urlParamsStr = new StringBuilder();
      boolean first = true;
      for (Map.Entry<String, String> entry : params.entrySet()) {
        if (first) {
          first = false;
        } else {
          urlParamsStr.append("&");
        }
        urlParamsStr.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
        urlParamsStr.append("=");
        urlParamsStr.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
      }
      byte[] postData = urlParamsStr.toString().getBytes(StandardCharsets.UTF_8);
      try (DataOutputStream wr = new DataOutputStream(connection.getOutputStream())) {
        wr.write(postData);
      }
    } catch (Exception e) {
      throw new RuntimeException("Unexpected error while try to set url parameters.", e);
    }
  }

  @SuppressWarnings("deprecation")
  public static String createJWT(String privateKeyStr, Map<String, Object> claims) throws Exception {
    PrivateKey privateKey = generatePublicKeyFromString(privateKeyStr, "RSA");
    return Jwts.builder().addClaims(claims).signWith(SignatureAlgorithm.RS256, privateKey).compact();
  }

  @SuppressWarnings("deprecation")
  public static String sign(String privateKeyStr, String payload, Map<String, Object> headers) throws Exception {
    PrivateKey privateKey = generatePublicKeyFromString(privateKeyStr, "RSA");
    return Jwts.builder().setHeader(headers).setPayload(payload).signWith(SignatureAlgorithm.RS256, privateKey)
        .compact();
  }

  public static Claims decodeJwt(String jwt) {
    try {
      return Jwts.parser().base64UrlDecodeWith(io.jsonwebtoken.io.Decoders.BASE64).parseClaimsJws(jwt).getBody();
    } catch (Exception e) {
      throw new RuntimeException("Unexpected error while try to decode jwt.", e);
    }
  }

  private static PrivateKey generatePublicKeyFromString(String keyStr, String algorithm) throws Exception {
    PrivateKey privateKey = null;
    if (keyStr.length() > 1) {
      keyStr = keyStr.replace("-----BEGIN RSA PRIVATE KEY-----", "").replace("-----END RSA PRIVATE KEY-----", "")
          .replaceAll("\\s+", "").replaceAll("\\r+", "").replaceAll("\\n+", "");
      byte[] data = Base64.getDecoder().decode(keyStr);
      ASN1EncodableVector v = new ASN1EncodableVector();
      v.add(new ASN1Integer(0));
      ASN1EncodableVector v2 = new ASN1EncodableVector();
      v2.add(new ASN1ObjectIdentifier(PKCSObjectIdentifiers.rsaEncryption.getId()));
      v2.add(DERNull.INSTANCE);
      v.add(new DERSequence(v2));
      v.add(new DEROctetString(data));
      ASN1Sequence seq = new DERSequence(v);
      byte[] privKey = seq.getEncoded("DER");

      PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(privKey);
      KeyFactory fact = KeyFactory.getInstance("RSA");
      PrivateKey key = fact.generatePrivate(spec);
      privateKey = key;
    }
    return privateKey;
  }

  public static JsonNode stringToJson(String json) {
    try {
      ObjectMapper mapper = new ObjectMapper();
      JsonNode actualObj = mapper.readTree(json);
      return actualObj;
    } catch (IOException e) {
      throw new RuntimeException("Unexpected error while try to parse string to json.", e);
    }
  }

  public static String responseToString(HttpURLConnection connection) throws IOException {
    try {
      return streamToString(connection.getInputStream());
    } catch (IOException e) {
      String errorStream = streamToString(connection.getErrorStream());
      if(isValidJson(errorStream)) {
          JsonNode errorJson = Utils.stringToJson(errorStream);
          if(errorJson.has("error_description")) {
              throw new RuntimeException("Unexpected error description: " + errorJson.get("error_description").asText(), e);
          }
      }
      throw new RuntimeException("Unexpected error stream: " + errorStream, e);
    } catch (Exception ex) {
      throw new RuntimeException("Unexpected error while get response content.", ex);
    }
  }

  public static boolean isValidJson(String json) {
      boolean valid = false;
      try {
          JsonParser parser = new ObjectMapper().getJsonFactory().createJsonParser(json);
          valid = true;
      } catch (JsonParseException jpe) {
      } catch (IOException ioe) {
      }

      return valid;
  }
  
  public static String streamToString(InputStream inputStream) throws IOException {
	BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, Charset.forName("UTF-8")));
      String inputLine;
      StringBuffer content = new StringBuffer();
      while ((inputLine = reader.readLine()) != null) {
        content.append(inputLine);
      }
      reader.close();
      return content.toString();
  }

  public static String fileToString(File file) {
    try {
      String str, result = "";
      BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
      while ((str = bufferedReader.readLine()) != null) {
        result += str + "\n";
      }
      bufferedReader.close();
      return result;
    } catch (Exception e) {
      throw new RuntimeException("Unexpected error while convert file to string.", e);
    }
  }
  
  public static long createExpirationDate(int min) {
    Date now = new Date();
    return Math.round((now.getTime() + (min * 60000)) / 1000);
  }
}
