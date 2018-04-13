package edu.kit.scc.dei.ecplean;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.Proxy;
import java.net.ProxySelector;
import java.util.List;
import java.util.Observable;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ParseException;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public abstract class ECPAuthenticatorBase extends Observable {

    protected static Logger LOG = Logger.getLogger(ECPAuthenticatorBase.class
        .getName());
    protected ECPAuthenticationInfo authInfo;
    protected HttpClientBuilder clientBuilder;
    protected DocumentBuilderFactory documentBuilderFactory;
    protected XPathFactory xpathFactory;
    protected NamespaceResolver namespaceResolver;
    protected TransformerFactory transformerFactory;

    public ECPAuthenticatorBase(HttpClientBuilder clientBuilder) {
        this.clientBuilder = clientBuilder;
        
        documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);

        xpathFactory = XPathFactory.newInstance();
        namespaceResolver = new NamespaceResolver();
        namespaceResolver.addNamespace("ecp",
            "urn:oasis:names:tc:SAML:2.0:profiles:SSO:ecp");
        namespaceResolver.addNamespace("S",
            "http://schemas.xmlsoap.org/soap/envelope/");
        namespaceResolver.addNamespace("paos", "urn:liberty:paos:2003-08");

        transformerFactory = TransformerFactory.newInstance();
    }

    protected Document authenticateIdP(Document idpRequest)
        throws ECPAuthenticationException
    {
        if (isFine()) {
            LOG.fine("Sending initial IdP Request to "
                + authInfo.getIdpEcpEndpoint());
        }
        // PFS-2070
        HttpPost httpPost = new HttpPost(authInfo.getIdpEcpEndpoint().toString());
        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY,
            new UsernamePasswordCredentials(authInfo.getUsername(), authInfo.getPassword()));

        // PFC-2958:
        httpPost.setHeader("Content-Type", "text/xml; charset=UTF-8");

        HttpHost targetHost = new HttpHost(authInfo.getIdpEcpEndpoint().getHost(), authInfo.getIdpEcpEndpoint().getPort(), 
                authInfo.getIdpEcpEndpoint().getScheme());
        AuthCache authCache = new BasicAuthCache();
        BasicScheme basicAuth = new BasicScheme();
        authCache.put(targetHost, basicAuth);

        // Add AuthCache to the execution context
        HttpClientContext context = HttpClientContext.create();
        context.setCredentialsProvider(credentialsProvider);
        context.setAuthCache(authCache);

        HttpResponse httpResponse;
        try {
            String idpRequestString = documentToString(idpRequest);
            httpPost.setEntity(new StringEntity(idpRequestString));
            httpResponse = getHttpClientForIDP().execute(targetHost, httpPost, context);
            
            if (httpResponse.getStatusLine()
                .getStatusCode() != HttpStatus.SC_OK)
            {
                throw new ECPUnauthorizedException("User not authorized: " + httpResponse);
            }
        } catch (IOException | TransformerException e) {
            LOG.warning("Could not submit PAOS request to IdP. " + e);
            throw new ECPAuthenticationException(e);
        }

        String responseBody;
        try {
            responseBody = EntityUtils.toString(httpResponse.getEntity());
            return buildDocumentFromString(responseBody);
        } catch (ParseException | IOException | SAXException
            | ParserConfigurationException e)
        {
            LOG.warning("Could not read response from IdP" + e);
            throw new ECPAuthenticationException(e);
        }
    }

    protected Document buildDocumentFromString(String input)
        throws IOException, ParserConfigurationException, SAXException
    {
        DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
        return builder.parse(new InputSource(new StringReader(input)));
    }

    protected Object queryDocument(Document xmlDocument, String expression,
        QName returnType) throws XPathException
    {
        XPath xpath = xpathFactory.newXPath();
        xpath.setNamespaceContext(namespaceResolver);
        XPathExpression xPathExpression = xpath.compile(expression);
        return xPathExpression.evaluate(xmlDocument, returnType);
    }

    protected String documentToString(Document xmlDocument)
        throws TransformerConfigurationException, TransformerException
    {
        Transformer transformer = transformerFactory.newTransformer();

        StreamResult result = new StreamResult(new StringWriter());
        DOMSource source = new DOMSource(xmlDocument);
        transformer.transform(source, result);

        return result.getWriter().toString();
    }

    /**
     * Gets and lazy initializes 
     * @return the http client to use
     */
    protected synchronized HttpClient getHttpClient() {
        if (client == null && authInfo != null) {
            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();

            HttpHost idpHost = new HttpHost(authInfo
                    .getIdpEcpEndpoint().getHost(), authInfo.getIdpEcpEndpoint()
                    .getPort());
            credentialsProvider.setCredentials(new AuthScope(idpHost),
                    new UsernamePasswordCredentials(authInfo.getUsername(),
                            authInfo.getPassword()));

            String proxyHost = System.getProperty("http.proxyHost");
            if (proxyHost != null && !proxyHost.trim().isEmpty()) {
                int proxyPort = Integer.parseInt(System.getProperty("http.proxyPort"));
                
                HttpHost proxy = new HttpHost(proxyHost, proxyPort);
                clientBuilder.setProxy(proxy);

                if (authInfo.getProxyUsername() != null
                    && !authInfo.getProxyUsername().isEmpty())
                {
                    Credentials credentials = new UsernamePasswordCredentials(
                        authInfo.getProxyUsername(),
                        authInfo.getProxyPassword());
                    AuthScope authScope = new AuthScope(proxy);
                    credentialsProvider.setCredentials(authScope, credentials);
                }
            }

            clientBuilder.setDefaultCredentialsProvider(credentialsProvider);
            client = clientBuilder.build();
        }
        return client;
    }

    /**
     * The http client to use against hostname
     * @return the http client to use
     */
    protected HttpClient getHttpClientForIDP() {
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();

        HttpClientBuilder clientBuilder = HttpClientBuilder.create();
        clientBuilder.useSystemProperties();

        String proxyHost = System.getProperty("https.proxyHost");
        List<Proxy> proxies = ProxySelector.getDefault().select(authInfo
                .getIdpEcpEndpoint());
        if (proxies.isEmpty() || proxies.get(0).type() == Proxy.Type.DIRECT) {
            proxyHost = null;
        }

        HttpHost idpHost = new HttpHost(authInfo
                .getIdpEcpEndpoint().getHost(), authInfo.getIdpEcpEndpoint()
                .getPort());
        credentialsProvider.setCredentials(new AuthScope(idpHost),
                new UsernamePasswordCredentials(authInfo.getUsername(),
                        authInfo.getPassword()));

        if (proxyHost != null && !proxyHost.trim().isEmpty()) {
            int proxyPort = Integer.parseInt(System.getProperty("http.proxyPort"));

            HttpHost proxy = new HttpHost(proxyHost, proxyPort);
            clientBuilder.setProxy(proxy);

            if (authInfo.getProxyUsername() != null
                    && !authInfo.getProxyUsername().isEmpty()) {
                Credentials credentials = new UsernamePasswordCredentials(
                        authInfo.getProxyUsername(),
                        authInfo.getProxyPassword());
                AuthScope authScope = new AuthScope(proxy);
                credentialsProvider.setCredentials(authScope, credentials);
            }
        } else {
            clientBuilder.setProxy(null);
        }

        clientBuilder.setDefaultCredentialsProvider(credentialsProvider);
        return clientBuilder.build();
    }


    protected boolean isFine() {
        return LOG.isLoggable(Level.FINE);
    }

    protected boolean isInfo() {
        return LOG.isLoggable(Level.INFO);
    }
}