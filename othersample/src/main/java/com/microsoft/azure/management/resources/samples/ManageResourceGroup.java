/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.management.resources.samples;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.arm.utils.SdkContext;
import com.microsoft.azure.credentials.ApplicationTokenCredentials;
import com.microsoft.azure.management.resources.v2018_02_01.ResourceGroup;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

/**
 * Azure Resource sample for managing resource groups - - Create a resource
 * group - Update a resource group - Create another resource group - List
 * resource groups - Delete a resource group.
 */

public final class ManageResourceGroup {
    /**
     * Main function which runs the actual sample.
     * 
     * @param azure instance of the azure client
     * @return true if sample runs successfully
     */
    public static boolean runSample(MyAzure azure, String location) {
        final String rgName = SdkContext.randomResourceName("rgRSMA", 24);
        final String rgName2 = SdkContext.randomResourceName("rgRSMA", 24);
        final String resourceTagName = SdkContext.randomResourceName("rgRSTN", 24);
        final String resourceTagValue = SdkContext.randomResourceName("rgRSTV", 24);
        try {

            // =============================================================
            // Create resource group.

            System.out.println("Creating a resource group with name: " + rgName);

            ResourceGroup resourceGroup = azure.resourceGroups().define(rgName).withExistingSubscription()
                    .withLocation(location).create();

            System.out.println("Created a resource group with name: " + rgName);

            // =============================================================
            // Update the resource group.

            System.out.println("Updating the resource group with name: " + rgName);
            Map<String, String> tags = new HashMap<String, String>();
            {
                tags.put(resourceTagName, resourceTagValue);
            }

            resourceGroup.update().withTags(tags).apply();

            System.out.println("Updated the resource group with name: " + rgName);

            // =============================================================
            // Create another resource group.

            System.out.println("Creating another resource group with name: " + rgName2);

            azure.resourceGroups().define(rgName2).withExistingSubscription().withLocation(location).create();

            System.out.println("Created another resource group with name: " + rgName2);

            // =============================================================
            // List resource groups.

            System.out.println("Listing all resource groups");

            for (com.microsoft.azure.management.resources.v2018_02_01.implementation.ResourceGroupInner rGroup : azure
                    .resourceGroups().inner().list()) {
                System.out.println("Resource group: " + rGroup.name());
            }

            // =============================================================
            // Delete a resource group.

            System.out.println("Deleting resource group: " + rgName2);

            azure.resourceGroups().deleteAsync(rgName2);
            return true;
        } catch (Exception f) {

            System.out.println(f.getMessage());
            f.printStackTrace();

        } finally {

            try {
                System.out.println("Deleting Resource Group: " + rgName);
                azure.resourceGroups().deleteAsync(rgName);
            } catch (NullPointerException npe) {
                System.out.println("Did not create any resources in Azure. No clean up is necessary");
            } catch (Exception g) {
                g.printStackTrace();
            }
        }
        return false;
    }

    public static HashMap<String, String> getActiveDirectorySettings(String armEndpoint) {
        HashMap<String, String> adSettings = new HashMap<String, String>();

        try {
            // create HTTP Client
            HttpClient httpClient = HttpClientBuilder.create().build();

            // Create new getRequest with below mentioned URL
            HttpGet getRequest = new HttpGet(String.format("%s/metadata/endpoints?api-version=1.0", armEndpoint));

            // Add additional header to getRequest which accepts application/xml data
            getRequest.addHeader("accept", "application/xml");

            // Execute request and catch response
            HttpResponse response = httpClient.execute(getRequest);

            // Check for HTTP response code: 200 = success
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : " + response.getStatusLine().getStatusCode());
            }
            String responseStr = EntityUtils.toString(response.getEntity());
            JSONObject responseJson = new JSONObject(responseStr);
            adSettings.put("galleryEndpoint", responseJson.getString("galleryEndpoint"));
            JSONObject authentication = (JSONObject) responseJson.get("authentication");
            String audience = authentication.get("audiences").toString().split("\"")[1];
            adSettings.put("login_endpoint", authentication.getString("loginEndpoint"));
            adSettings.put("audience", audience);
            adSettings.put("graphEndpoint", responseJson.getString("graphEndpoint"));

        } catch (ClientProtocolException e) {
            e.printStackTrace();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return adSettings;
    }

    /**
     * Main entry point.
     *
     * @param args the parameters
     */
    public static void main(String[] args) {
        try {
            // =================================================================
            // Authenticate

            // Get the ARM Endpoint
            final String armEndpoint = System.getenv("ARM_ENDPOINT");
            final String location = System.getenv("RESOURCE_LOCATION");
            final HashMap<String, String> settings = getActiveDirectorySettings(armEndpoint);

            // Get AzureStack cloud endpoints
            AzureEnvironment AZURE_STACK = new AzureEnvironment(new HashMap<String, String>() {
                {
                    put("managementEndpointUrl", settings.get("audience"));
                    put("resourceManagerEndpointUrl", armEndpoint);
                    put("galleryEndpointUrl", settings.get("galleryEndpoint"));
                    put("activeDirectoryEndpointUrl", settings.get("login_endpoint"));
                    put("activeDirectoryResourceId", settings.get("audience"));
                    put("activeDirectoryGraphResourceId", settings.get("graphEndpoint"));
                    put("storageEndpointSuffix", armEndpoint.substring(armEndpoint.indexOf('.')));
                    put("keyVaultDnsSuffix", ".adminvault" + armEndpoint.substring(armEndpoint.indexOf('.')));
                }
            });

            String client = System.getenv("CLIENT_ID");
            String tenant = System.getenv("TENANT_ID");
            String key = System.getenv("CLIENT_SECRET");
            String subscriptionId = System.getenv("SUBSCRIPTION_ID");

            // Authenticate to AzureStack using Service principal creds
            ApplicationTokenCredentials credentials = (ApplicationTokenCredentials) new ApplicationTokenCredentials(
                    client, tenant, key, AZURE_STACK).withDefaultSubscriptionId(subscriptionId);
            MyAzure azureStack = MyAzure.configure().withLogLevel(com.microsoft.rest.LogLevel.BASIC)
                    .authenticate(credentials, subscriptionId);

            // Manage resource groups
            runSample(azureStack, location);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }
}
