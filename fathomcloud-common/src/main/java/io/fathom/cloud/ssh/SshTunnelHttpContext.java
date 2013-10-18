package io.fathom.cloud.ssh;

import javax.inject.Provider;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.scheme.SchemeSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;

public class SshTunnelHttpContext implements Provider<HttpClient> {
    final HttpClient defaultHttpClient;

    public SshTunnelHttpContext(SshContext sshContext) {
        this.defaultHttpClient = buildHttpClient(sshContext);
    }

    @Override
    public HttpClient get() {
        return defaultHttpClient;
    }

    static DefaultHttpClient buildHttpClient(SshContext sshContext) {
        // SSLContext sslcontext;
        // try {
        // sslcontext = SSLContext.getInstance("TLS");
        // sslcontext.init(null, null, null);
        // } catch (GeneralSecurityException e) {
        // throw new IllegalStateException("Error initializing SSL", e);
        // }

        // SSLSocketFactory ssf = new SSLSocketFactory(sslcontext,
        // SSLSocketFactory.STRICT_HOSTNAME_VERIFIER);

        SchemeRegistry sr = new SchemeRegistry();
        // sr.register(new Scheme("https", 443, ssf));
        sr.register(new Scheme("http", 80, buildSslSocketFactory(sshContext)));

        ClientConnectionManager ccm = new PoolingClientConnectionManager(sr);
        // ClientConnectionManager ccm = new PoolingClientConnectionManager();
        DefaultHttpClient httpClient = new DefaultHttpClient(ccm);

        // HttpParams httpParams = httpClient.getParams();

        // HttpProtocolParams.setVersion(httpParams, HttpVersion.HTTP_1_1);
        // HttpProtocolParams.setContentCharset(httpParams, "UTF-8");
        // HttpProtocolParams.setUseExpectContinue(httpParams, true);

        // HttpConnectionParams.setConnectionTimeout(httpParams, 10000);
        // HttpConnectionParams.setSoTimeout(httpParams, 10000);
        //
        // HttpConnectionParams.setSocketBufferSize(httpParams, 8192);

        // configureProxy(httpClient, configuration);

        return httpClient;
    }

    private static SchemeSocketFactory buildSslSocketFactory(SshContext sshContext) {
        return new SshTunnelSchemeSocketFactory(sshContext);
    }
}
