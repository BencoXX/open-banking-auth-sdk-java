<%@page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@page import="java.io.File"%>
<%@page import="java.io.IOException"%>
<%@page import="java.io.OutputStream"%>
<%@page import="java.io.PrintWriter"%>
<%@page import="java.net.HttpURLConnection"%>
<%@page import="java.net.URL"%>
<%@page import="java.util.HashMap"%>
<%@page import="java.util.Map"%>

<%@page import="javax.servlet.ServletException"%>
<%@page import="javax.servlet.http.HttpServlet"%>
<%@page import="javax.servlet.http.HttpServletRequest"%>
<%@page import="javax.servlet.http.HttpServletResponse"%>

<%@page import="org.apache.commons.lang.RandomStringUtils"%>

<%@page import="com.fasterxml.jackson.databind.JsonNode"%>
<%@page import="com.fintechblocks.java.sdk.OpenBankingAuth"%>
<%@page import="com.fintechblocks.java.sdk.Utils"%>
<%
    String clientId = "beniapp@account-info-1.1";
    String apiUrl = "https://api.sandbox.sberbank.hu/account-info-1.1/open-banking/v3.1/aisp";
    String scope = "accounts";
    String redirectUri = "http://localhost:8080/example/accountinfo_example_sberbank.jsp";
    String tokenEndpointUri =
        "https://api.sandbox.sberbank.hu/auth/realms/ftb-sandbox/protocol/openid-connect/token";
    String authorizationEndpointURI =
        "https://api.sandbox.sberbank.hu/auth/realms/ftb-sandbox/protocol/openid-connect/auth";

    String webRootPath = application.getRealPath("/").replace('\\', '/');
    Boolean code = false;

    File privateKeyFile = new File(webRootPath + "WEB-INF/classes/jwtRS256_2048.key");
    File accountAccessConsentFile =
        new File(webRootPath + "WEB-INF/classes/account-access-consent-sberbank.json");
    String accountAccessConsent = Utils.fileToString(accountAccessConsentFile);

    String privateKey = Utils.fileToString(privateKeyFile);
    String keyID = "";

    OpenBankingAuth accountInfoAuth = new OpenBankingAuth(clientId, privateKey, keyID, redirectUri,
        tokenEndpointUri, authorizationEndpointURI, scope);

    if (request.getParameter("code") == null) {
        try {
            String accessToken = accountInfoAuth.getAccessToken();

            Map<String, String> headers = new HashMap<String, String>();
            headers.put("x-jws-signature", accountInfoAuth.createSignatureHeader(accountAccessConsent,
                "C=UK, ST=England, L=London, O=Acme Ltd."));
            JsonNode accountAccessConsentJson =
                callAPI(accessToken, apiUrl, "account-access-consents", "POST", headers, accountAccessConsent);
            String intentId = accountAccessConsentJson.get("Data").get("ConsentId").asText();
            String state = RandomStringUtils.random(20, true, true);
            String nonce = RandomStringUtils.random(20, true, true);
            String authUrl = accountInfoAuth.generateAuthorizationUrl(intentId, state, nonce);

            response.sendRedirect(authUrl);
        } catch (Exception e) {
            out.println(e.toString());
            e.printStackTrace();
        }
    } else {
        try {
            String newAccessToken;
            if (request.getParameter("token") == null) {
                JsonNode newAccessTokenJson = accountInfoAuth.exchangeToken(request.getParameter("code"));
                newAccessToken = newAccessTokenJson.get("access_token").asText();

                StringBuilder newRequestURL = new StringBuilder(request.getRequestURL().toString());
                String queryString = request.getQueryString();
                newRequestURL.append('?').append(queryString).append("&token=" + newAccessToken);
                response.sendRedirect(newRequestURL.toString());
            } else {
                newAccessToken = request.getParameter("token");
            }

            JsonNode result;

            // Get accounts
//             result = callAPI(newAccessToken, apiUrl, "accounts", "GET", null, null);
//             out.println(result.toString());

            // Get transactions
            result = callAPI(newAccessToken, apiUrl,
                "transactions?fromBookingDateTime=2019-08-01T00:00:00.000Z&toBookingDateTime=2019-09-15T00:00:00.000Z", "GET", null, null);
            out.println(result.toString());


            // Get accountId + transactions
            //       result = callAPI(newAccessToken, apiUrl, "000000000000000000000166/transactions", "GET", null, null);
            //       out.println(result.toString());

        } catch (Exception e) {
            out.println(e.toString());
            e.printStackTrace();
        }
    }
%>
<%!private JsonNode callAPI(String accessToken, String apiUrl, String endpoint, String method,
        Map<String, String> headers, String body) throws IOException {
        URL url = new URL(apiUrl + "/" + endpoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);
        connection.setRequestProperty("Authorization", "Bearer " + accessToken);
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setRequestProperty("Accept", "application/json");
        connection.setDoOutput(true);
        connection.setDoInput(true);

        if (headers != null) {
            for (Map.Entry<String, String> header : headers.entrySet()) {
                connection.setRequestProperty(header.getKey(), header.getValue());
            }
        }

        if (body != null) {
            OutputStream outputStream = connection.getOutputStream();
            outputStream.write(body.getBytes("UTF-8"));
            outputStream.close();
        }

        return Utils.stringToJson(Utils.responseToString(connection));
    }%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Account test</title>
</head>
<body>
</body>
</html>