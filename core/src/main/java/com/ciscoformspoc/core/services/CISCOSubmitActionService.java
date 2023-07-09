/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2023 Adobe
 ~
 ~ Licensed under the Apache License, Version 2.0 (the "License");
 ~ you may not use this file except in compliance with the License.
 ~ You may obtain a copy of the License at
 ~
 ~     http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing, software
 ~ distributed under the License is distributed on an "AS IS" BASIS,
 ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ~ See the License for the specific language governing permissions and
 ~ limitations under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/

package com.ciscoformspoc.core.services;

import com.adobe.aemds.guide.common.GuideValidationResult;
import com.adobe.aemds.guide.model.FormSubmitInfo;
import com.adobe.aemds.guide.service.FormSubmitActionService;
import com.adobe.aemds.guide.utils.GuideConstants;
import com.adobe.aemds.guide.utils.GuideSubmitErrorCause;
import com.adobe.aemds.guide.utils.GuideSubmitUtils;
import com.adobe.aemds.guide.utils.GuideUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.osgi.services.HttpClientBuilderFactory;
import org.apache.http.util.EntityUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * CISCO REST Submit Action Service for Adaptive Forms.
 */
@Component(service={FormSubmitActionService.class},
        immediate = true)
public class CISCOSubmitActionService implements FormSubmitActionService {
    private static final String serviceName = "CISCO_REST";

    protected static Logger logger = LoggerFactory.getLogger(CISCOSubmitActionService.class);

    @Reference
    private HttpClientBuilderFactory httpClientBuilderFactory;

    public String getServiceName() {
        return serviceName;
    }

    public Map<String, Object> submit(FormSubmitInfo formSubmitInfo) {
        Map<String, Object> result = new HashMap<>();
        result.put(GuideConstants.FORM_SUBMISSION_COMPLETE, Boolean.FALSE);
        Resource formContainerResource = formSubmitInfo.getFormContainerResource();
        ResourceResolver resourceResolver = formContainerResource.getResourceResolver();
        String formContainerResourcePath = formContainerResource.getPath();

        HttpClientBuilder httpClientBuilder = httpClientBuilderFactory.newBuilder();
        try (CloseableHttpClient httpclient = httpClientBuilder.build()) {
            ValueMap properties = formContainerResource.getValueMap();
            if (properties.get("ciscoRestEndpointPostUrl", (String) null) != null) {
                String postUrl = properties.get("ciscoRestEndpointPostUrl", (String) null);
                HttpPost httppost = new HttpPost(postUrl);
                MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
                ObjectMapper objectMapper = new ObjectMapper();
                TypeReference<HashMap<String,String>> typeRef = new TypeReference<HashMap<String, String>>() {};
                HashMap<String, String> map = objectMapper.readValue(formSubmitInfo.getData(),typeRef);
                for (String key : map.keySet()) {
                    StringBody value = new StringBody(map.get(key), ContentType.MULTIPART_FORM_DATA);
                    multipartEntityBuilder.addPart(key, value);
                }
                httppost.setEntity(multipartEntityBuilder.build());
                HttpResponse resp = httpclient.execute(httppost);
                int status = resp.getStatusLine().getStatusCode();
                if (status == HttpStatus.SC_OK) {
                    result.put(GuideConstants.FORM_SUBMISSION_COMPLETE, Boolean.TRUE);
                    if (resp.getFirstHeader("Content-Type") != null &&
                            resp.getFirstHeader("Content-Type").getValue().contains(URLEncodedUtils.CONTENT_TYPE)) {
                        String responseString = EntityUtils.toString(resp.getEntity());
                        Map<String, String> resultMap = new HashMap<>();
                        GuideUtils.putQueryParamsToRedirectRequest(responseString, resultMap);
                        result.putAll(resultMap);
                    } else {
                        logger.debug("[AF] [Submit] [CISCO] RESTSubmitActionService: Content type is either is null or does not contain application/x-www-form-urlencoded for form {}", formContainerResourcePath);
                    }
                } else {
                    // if response is not successful, return the status code of rest URL invoked
                    String errorMessage = resp.getStatusLine().getReasonPhrase();
                    GuideValidationResult guideValidationResult = null;
                    if ((resp.getEntity() != null) && (resp.getEntity().getContent() != null)) {
                        errorMessage = IOUtils.toString(resp.getEntity().getContent(), "UTF-8");
                        guideValidationResult = GuideSubmitUtils.getGuideValidationResultFromString(errorMessage, Integer.toString(status));
                    }

                    result.put(GuideConstants.FORM_SUBMISSION_ERROR, guideValidationResult);
                    logger.error("[AF] [Submit] [CISCO] Couldn't post data to {} for form {}", postUrl, formContainerResourcePath);
                    logger.error("[AF] [Submit] [CISCO] HTTP Status code: {}, reason phrase is :{} for form {}", status, resp.getStatusLine().getReasonPhrase(), formContainerResourcePath);
                    logger.error("[AF] [Submit] [CISCO] The content received from RestEndPoint is : {} for form {}", errorMessage, formContainerResourcePath);
                }
            } else {
                logger.error("[CISCO] [AF] [Submit] CISCOSubmitActionService: Rest end point post URL is not set in form {}", formContainerResourcePath);
            }
        } catch (Exception e) {
            GuideSubmitUtils.addValidationErrorToResult(result, GuideSubmitErrorCause.FORM_SUBMISSION,
                    StringUtils.isEmpty(e.getMessage()) ? "Failed to make REST call" : e.getMessage(),
                    Integer.toString(HttpServletResponse.SC_INTERNAL_SERVER_ERROR));
            logger.error("[CISCO] [AF] [Submit] Failed to make REST call in form {}", formContainerResourcePath, e);
        } finally {
            resourceResolver.close();
        }
        return result;
    }
}
