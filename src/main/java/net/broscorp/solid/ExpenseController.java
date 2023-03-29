package net.broscorp.solid;

import com.inrupt.client.Request;
import com.inrupt.client.Response;
import com.inrupt.client.auth.Session;
import com.inrupt.client.openid.OpenIdSession;
import com.inrupt.client.solid.SolidSyncClient;
import com.inrupt.client.webid.WebIdProfile;
import java.io.PrintWriter;
import java.net.URI;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RequestMapping("/api")
@RestController
public class ExpenseController {

    /**
     * Note 1: Authenticated Session
     * Using the client credentials, create an authenticated session.
     * MY_SOLID_IDP - https://solidcommunity.net or https://login.inrupt.com. It depends on which provider you use.
     */
    final Session session = OpenIdSession.ofClientCredentials(
        URI.create(System.getenv("MY_SOLID_IDP")),
        System.getenv("MY_SOLID_CLIENT_ID"),
        System.getenv("MY_SOLID_CLIENT_SECRET"),
        System.getenv("MY_AUTH_FLOW"));
    /**
     * Note 2: SolidSyncClient
     * Instantiates a synchronous client for the authenticated session.
     * The client has methods to perform CRUD operations.
     */
    final SolidSyncClient client = SolidSyncClient.getClient().session(session);
    private final PrintWriter printWriter = new PrintWriter(System.out, true);

    /**
     * Note 3: SolidSyncClient.read()
     * Using the SolidSyncClient client.read() method, reads the user's WebID Profile document and returns the Pod URI(s).
     */
    @GetMapping("/pods")
    public Set<URI> getPods(@RequestParam(value = "webid", defaultValue = "") String webID) {
        printWriter.println("ExpenseController:: getPods");
        try (final var profile = client.read(URI.create(webID), WebIdProfile.class)) {
            return profile.getStorage();
        }
    }

    /**
     * Note 4: SolidSyncClient.create()
     * Using the SolidSyncClient client.create() method,
     * - Saves the Expense as an RDF resource to the location specified in the Expense.identifier field.
     */
    @PostMapping(path = "/expenses/create")
    public String createExpense(@RequestBody Expense newExpense) {
        printWriter.println("ExpenseController:: createExpense");
        client.create(newExpense);
        return getResourceAsTurtle(String.valueOf(newExpense.getIdentifier()));
    }

    /**
     * Note 5: SolidSyncClient.read()
     * Using the SolidSyncClient client.read() method,
     * - Reads the RDF resource into the Expense class.
     */
    @GetMapping("/expenses/get")
    public Expense getExpense(@RequestParam(value = "resourceURL", defaultValue = "") String resourceURL) {
        printWriter.println("ExpenseController:: getExpense");
        try (var resource = client.read(URI.create(resourceURL), Expense.class)) {
            return resource;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Note 6: SolidSyncClient.update()
     * Using the SolidSyncClient client.update() method,
     * - Updates the Expense resource.
     */
    @PutMapping("/expenses/update")
    public String updateExpense(@RequestBody Expense expense) {
        printWriter.println("ExpenseController:: updateExpense");
        client.update(expense);
        return getResourceAsTurtle(String.valueOf(expense.getIdentifier()));
    }


    /**
     * Note 7: SolidSyncClient.delete()
     * Using the SolidSyncClient client.delete() method,
     * - Deletes the Expense located in the Expense.identifier field.
     */

    @DeleteMapping("/expenses/delete")
    public void deleteExpense(@RequestParam(value = "resourceURL") String resourceURL) {
        printWriter.println("ExpenseController:: deleteExpense");
        try {
            client.delete(new Expense(URI.create(resourceURL)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Note 8: Build and issue custom GET Request
     * - Use Inrupt's Request builder to create a GET Request to get the resource as MIME-type "text/turtle".
     * - Use SolidSyncClient.send() to send the Request and return the response body.
     */

    @GetMapping("/resource/get")
    public String getResourceAsTurtle(@RequestParam(value = "resourceURL", defaultValue = "") String resourceURL) {
        Request request = Request.newBuilder()
            .uri(URI.create(resourceURL))
            .header("Accept", "text/turtle")
            .GET()
            .build();
        Response<String> response = client.send(
            request,
            Response.BodyHandlers.ofString());
        return response.body();
    }

    /**
     * Note 9: Stores a non-RDF resource to a Pod
     *
     * - Build a PUT request using Request.newBuilder()...build();
     * - Send the request using SolidSyncClient's low-level client.send() method;
     */
    @PutMapping("/resource/nonRDF/add")
    public boolean addNonRDFFile(@RequestParam(value = "destinationURL") String destinationURL,
        @RequestParam(value = "file") MultipartFile file) {
        printWriter.println("In addNonRDFFile:: Save Non-RDF File to Pod.");
        try (final var fileStream = file.getInputStream()) {
            Request request = Request.newBuilder()
                .uri(URI.create(destinationURL))
                .header("Content-Type", file.getContentType())
                .PUT(Request.BodyPublishers.ofInputStream(fileStream))
                .build();
            Response<Void> response = client.send(
                request,
                Response.BodyHandlers.discarding());
            if (response.statusCode() == 201 || response.statusCode() == 200 || response.statusCode() == 204)
                return true;
        } catch (java.io.IOException e1) {
            e1.printStackTrace();
        }

        return false;
    }

    /**
     * Note 10: Stores a non-RDF resource (image of the receipt) to a Pod and Attach to an Expense
     * Using methods defined as part of getting started, addReceiptToExpense:
     * - Calls addNonRDFFile() to store the receipt to a Pod
     * - Calls getExpense() to fetch the associated Expense RDF resource.
     * - Calls the Expense's setter `addReceipt` to add the link to the saved receipt.
     * - Calls updateExpense() to save the updated Expense.
     */
    @PutMapping("/expenses/receipts/add")
    public String addReceiptToExpense(@RequestParam(value = "destinationURL") String destinationURL,
        @RequestParam(value = "file") MultipartFile file,
        @RequestParam(value = "expenseURL") String expenseURL) {
        printWriter.println("In addReceiptToExpense: Save Receipt File to Pod and Update Associated Expense.");
        boolean success = addNonRDFFile(destinationURL, file);
        if (success) {
            Expense expense = getExpense(expenseURL);
            expense.addReceipt(destinationURL);
            return updateExpense(expense);
        } else {
            printWriter.println("Error adding receipt");
            return null;
        }
    }

}
