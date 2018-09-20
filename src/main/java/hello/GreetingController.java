package hello;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.microsoft.azure.management.resources.samples.*;
import java.util.HashMap;
import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.credentials.ApplicationTokenCredentials;

@Controller
public class GreetingController {
    

    @GetMapping("/greeting")
    public String greeting(@RequestParam(name="name", required=false, defaultValue="World") String name, Model model) {
        model.addAttribute("name", name);
        return "greeting";
    }
    
    @GetMapping("/result")
    public String push(@RequestParam(name="vmname", required=true) String vmname, Model model, @RequestParam(name="azrgname", required=true) String azrgname, @RequestParam(name="azsrgname", required=true) String azsrgname, @RequestParam(name="saname", required=true) String saname) {
        model.addAttribute("vmname", vmname);
        model.addAttribute("azrgname", azrgname);
        model.addAttribute("azsrgname", azsrgname);
        model.addAttribute("saname", saname);
        
        final String armEndpoint = System.getenv("ARM_ENDPOINT");
        final String location = System.getenv("RESOURCE_LOCATION");
        final HashMap<String, String> settings = ManageResourceGroup.getActiveDirectorySettings(armEndpoint);

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

        //     // Authenticate to AzureStack using Service principal creds
            ApplicationTokenCredentials credentials = (ApplicationTokenCredentials) new ApplicationTokenCredentials(
                    client, tenant, key, AZURE_STACK).withDefaultSubscriptionId(subscriptionId);
            MyAzure azureStack = MyAzure.configure().withLogLevel(com.microsoft.rest.LogLevel.BASIC)
                    .authenticate(credentials, subscriptionId);

        //     // Manage resource groups
            ManageResourceGroup.runSample(azureStack, location);
        return "greeting";
    }
}
