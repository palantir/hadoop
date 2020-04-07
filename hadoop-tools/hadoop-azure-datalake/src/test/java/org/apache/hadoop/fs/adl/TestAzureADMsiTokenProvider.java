package org.apache.hadoop.fs.adl;

import com.microsoft.azure.datalake.store.oauth2.AccessTokenProvider;
import com.microsoft.azure.datalake.store.oauth2.AzureADAuthenticator;
import com.microsoft.azure.datalake.store.oauth2.AzureADToken;
import org.apache.hadoop.conf.Configuration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;

import static org.apache.hadoop.fs.adl.AdlConfKeys.*;
import static org.apache.hadoop.fs.adl.TokenProviderType.MSI;

/*
    Test MSI token provider.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(AzureADAuthenticator.class)
public class TestAzureADMsiTokenProvider {

    private final AzureADToken dummyToken = new AzureADToken();

    @Before
    public void setUp() {
        dummyToken.accessToken = "dummyAccessToken";
        dummyToken.expiry = new Date(System.currentTimeMillis());
    }

    @Test
    public void testMSITokenProviderMissingTenantIdAndClientId()
            throws IOException, URISyntaxException {
        Configuration conf = new Configuration();
        conf.setEnum(AZURE_AD_TOKEN_PROVIDER_TYPE_KEY, MSI);

        PowerMockito.mockStatic(AzureADAuthenticator.class);
        PowerMockito.when(AzureADAuthenticator.getTokenFromMsi(ArgumentMatchers.isNull(), ArgumentMatchers.isNull(), ArgumentMatchers.eq(false))).thenReturn(dummyToken);

        URI uri = new URI("adl://localhost:8080");
        AdlFileSystem fileSystem = new AdlFileSystem();
        fileSystem.initialize(uri, conf);
        AccessTokenProvider tokenProvider = fileSystem.getTokenProvider();
        Assert.assertEquals(dummyToken, tokenProvider.getToken());
    }

    @Test
    public void testMSITokenProviderWithTenantIdAndClientId()
            throws IOException, URISyntaxException {
        Configuration conf = new Configuration();
        conf.setEnum(AZURE_AD_TOKEN_PROVIDER_TYPE_KEY, MSI);

        conf.set(MSI_AZURE_AD_TENANT_ID_KEY, "dummyTenant");
        conf.set(AZURE_AD_CLIENT_ID_KEY, "dummyClient");

        PowerMockito.mockStatic(AzureADAuthenticator.class);
        PowerMockito.when(AzureADAuthenticator.getTokenFromMsi(ArgumentMatchers.matches("dummyTenant"), ArgumentMatchers.matches("dummyClient"), ArgumentMatchers.eq(false))).thenReturn(dummyToken);

        URI uri = new URI("adl://localhost:8080");
        AdlFileSystem fileSystem = new AdlFileSystem();
        fileSystem.initialize(uri, conf);
        AccessTokenProvider tokenProvider = fileSystem.getTokenProvider();
        Assert.assertEquals(dummyToken, tokenProvider.getToken());
    }
}
