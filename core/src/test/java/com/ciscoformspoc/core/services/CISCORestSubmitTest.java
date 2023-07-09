/*
 *  Copyright 2023 Adobe Systems Incorporated
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.ciscoformspoc.core.services;

import com.adobe.aemds.guide.model.FormSubmitInfo;
import com.adobe.aemds.guide.service.FormSubmitActionService;
import com.adobe.aemds.guide.utils.GuideConstants;
import com.ciscoformspoc.core.testcontext.AppAemContext;
import com.day.cq.wcm.api.Page;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.osgi.services.HttpClientBuilderFactory;
import org.apache.sling.api.resource.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.osgi.framework.Constants;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;

@ExtendWith({AemContextExtension.class, MockitoExtension.class})
class CISCORestSubmitTest {

    private final AemContext context = AppAemContext.newAemContext();

    private Resource formContainerResource;

    private CISCOSubmitActionService ciscoSubmitActionService = new CISCOSubmitActionService();

    @Mock
    private CloseableHttpResponse httpResponse;

    @Mock
    CloseableHttpClient httpClient;

    @Mock
    StatusLine statusLine;

    @Mock
    HttpClientBuilderFactory httpClientBuilderFactory;

    @Mock
    HttpClientBuilder httpClientBuilder;

    @BeforeEach
    public void setup() throws Exception {

        // preparing the test url
        lenient().when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        lenient().when(httpResponse.getStatusLine()).thenReturn(statusLine);
        lenient().when(httpClient.execute(ArgumentMatchers.any())).thenReturn(httpResponse);
        lenient().when(httpClientBuilderFactory.newBuilder()).thenReturn(httpClientBuilder);
        lenient().when(httpClientBuilder.build()).thenReturn(httpClient);
        context.registerService(HttpClientBuilderFactory.class, httpClientBuilderFactory);

        //preparing test action service
        context.registerInjectActivateService(ciscoSubmitActionService);

        // prepare a page with a test resource
        Page page = context.create().page("/content/mypage");
        Map<String, Object> properties = new HashMap<>();
        properties.put("sling:resourceType", "ciscoformspoc/components/adaptiveForm/formcontainer");
        properties.put("ciscoRestEndpointPostUrl", "https://www.mytest.com");
        formContainerResource = context.create().resource(page, "guideContainer", properties);
    }

    @Test
    void ciscoRestTest(AemContext context) {
        context.build().resource("/content/test", "jcr:title", "resource title").commit();
        context.currentResource("/content/test");

        FormSubmitInfo formSubmitInfo = new FormSubmitInfo();
        formSubmitInfo.setFormContainerResource(formContainerResource);
        formSubmitInfo.setContentType("application/json");
        formSubmitInfo.setData("{\"name\": \"x\"}");
        Map<String, Object> result = context.getService(FormSubmitActionService.class).submit(formSubmitInfo);

        assertEquals(result.get(GuideConstants.FORM_SUBMISSION_COMPLETE), Boolean.TRUE, "Succesful submission should happen.");
    }
}
