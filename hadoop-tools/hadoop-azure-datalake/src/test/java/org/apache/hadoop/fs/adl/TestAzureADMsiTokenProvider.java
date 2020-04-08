/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
    PowerMockito.when(AzureADAuthenticator
        .getTokenFromMsi(
            ArgumentMatchers.isNull(),
            ArgumentMatchers.isNull(),
            ArgumentMatchers.eq(false)))
        .thenReturn(dummyToken);

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
    PowerMockito.when(AzureADAuthenticator
        .getTokenFromMsi(
            ArgumentMatchers.matches("dummyTenant"),
            ArgumentMatchers.matches("dummyClient"),
            ArgumentMatchers.eq(false)))
        .thenReturn(dummyToken);

    URI uri = new URI("adl://localhost:8080");
    AdlFileSystem fileSystem = new AdlFileSystem();
    fileSystem.initialize(uri, conf);
    AccessTokenProvider tokenProvider = fileSystem.getTokenProvider();
    Assert.assertEquals(dummyToken, tokenProvider.getToken());
  }
}
