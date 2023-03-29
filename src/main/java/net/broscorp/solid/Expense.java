package net.broscorp.solid;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.inrupt.client.solid.Metadata;
import com.inrupt.client.solid.SolidRDFSource;
import com.inrupt.rdf.wrapping.commons.RDFFactory;
import com.inrupt.rdf.wrapping.commons.TermMappings;
import com.inrupt.rdf.wrapping.commons.ValueMappings;
import com.inrupt.rdf.wrapping.commons.WrapperIRI;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDFTerm;
import java.util.*;
/**
 * Part 1
 * Note: extends SolidRDFSource
 * To model the Expense class as an RDF resource, the Expense class extends SolidRDFSource.
 * <p>
 * The @JsonIgnoreProperties annotation is added to ignore the RDF-related fields
 * ("metadata", "graph", "graphNames", "entity", "contentType") when serializing Expense data as JSON.
 */
@JsonIgnoreProperties(value = { "metadata", "graph", "graphNames", "entity", "contentType"})
public class Expense extends SolidRDFSource {

    /**
     * Note 2a: Predicate Definitions
     * The following constants define the Predicates used in our triple statements.
     */
    static IRI RDF_TYPE = rdf.createIRI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
    static IRI SCHEMA_DATE = rdf.createIRI("https://schema.org/Date");
    static IRI SCHEMA_PROVIDER = rdf.createIRI("https://schema.org/provider");
    static IRI SCHEMA_DESCRIPTION = rdf.createIRI("https://schema.org/description");
    static IRI SCHEMA_TOTAL_PRICE = rdf.createIRI("https://schema.org/totalPrice");
    static IRI SCHEMA_CURRENCY = rdf.createIRI("https://schema.org/priceCurrency");
    static IRI SCHEMA_CATEGORY = rdf.createIRI("https://schema.org/category");

    // Added predicate for receipts
    static IRI SCHEMA_IMAGE = rdf.createIRI("https://schema.org/image");

    /**
     * Note 2b: Value Definition
     * The following constant define the value for the predicate RDF_TYPE.
     */
    static URI MY_RDF_TYPE_VALUE = URI.create("https://schema.org/Invoice");

    /**
     * Note 3: Node class
     * The Node class is an inner class (defined below) that handles the mapping between expense data and RDF triples.
     * The subject contains the expense data.
     */
    private final Node subject;

    /**
     * Note 4: Constructors
     * Expense constructors to handle SolidResource fields:
     * - identifier: The destination URI of the resource; e.g., https://myPod.example.com/myPod/expense1
     * - dataset: The org.apache.commons.rdf.api.Dataset that corresponding to the resource. Can be null when instantiating the class.
     * - metadata:  The com.inrupt.client.solid.Metadata that contains information from response headers. Can be null when instantiating the class.
     * <p>
     * In addition, the subject field is initialized.
     */

    public Expense(final URI identifier, final Dataset dataset, final Metadata metadata) {
        super(identifier, dataset, metadata);
        this.subject = new Node(rdf.createIRI(identifier.toString()), getGraph());
    }

    public Expense(final URI identifier) {
        this(identifier, null, null);
    }

    @JsonCreator
    public Expense(@JsonProperty("identifier") final URI identifier,
        @JsonProperty("merchantProvider") String merchantProvider,
        @JsonProperty("expenseDate") Date expenseDate,
        @JsonProperty("description") String description,
        @JsonProperty("amount") BigDecimal amount,
        @JsonProperty("currency") String currency,
        @JsonProperty("category") String category,
        @JsonProperty("receipts") String[] receipts) {
        this(identifier);
        this.setRdfType(MY_RDF_TYPE_VALUE);
        this.setMerchantProvider(merchantProvider);
        this.setExpenseDate(expenseDate);
        this.setDescription(description);
        this.setAmount(amount);
        this.setCurrency(currency);
        this.setCategory(category);
        this.setReceipts(receipts);
    }

    /**
     * Note 5: Various getters/setters.
     * The getters and setters reference the subject's methods.
     */

    public URI getRDFType() {
        return subject.getRDFType();
    }

    public void setRdfType(URI rdfType) {
        subject.setRDFType(rdfType);
    }

    public String getMerchantProvider() {
        return subject.getMerchantProvider();
    }

    public void setMerchantProvider(String merchantProvider) {
        subject.setMerchantProvider(merchantProvider);
    }

    public Date getExpenseDate() {
        return subject.getExpenseDate();
    }

    public void setExpenseDate(Date expenseDate) {
        subject.setExpenseDate(expenseDate);
    }

    public String getDescription() {
        return subject.getDescription();
    }

    public void setDescription(String description) {
        subject.setDescription(description);
    }

    public BigDecimal getAmount() {
        return subject.getAmount();
    }

    public void setAmount(BigDecimal amount) {
        subject.setAmount(amount);
    }

    public String getCurrency() {
        return subject.getCurrency();
    }

    public void setCurrency(String currency) {
        subject.setCurrency(currency);
    }

    public String getCategory() {
        return subject.getCategory();
    }

    public void setCategory(String category) {
        subject.setCategory(category);
    }

    public Set<String> getReceipts() {
        return subject.getReceipts();
    }
    // Note:: The setters first uses the getter, which returns a Set, and adds the receipt to the set.
    public void addReceipt(String receipt) {
        subject.getReceipts().add(receipt);
    }
    public void setReceipts(String[] receipts) {
        subject.getReceipts().addAll(List.of(receipts));
    }

    /**
     * Note 6: Inner class ``Node`` that extends WrapperIRI
     * Node class handles the mapping of the expense data (date, provider,
     * description, category, priceCurrency, total) to RDF triples
     * <subject> <predicate> <object>.
     * <p>
     * Nomenclature Background: A set of RDF triples is called a Graph.
     */
    class Node extends WrapperIRI {

        Node(final RDFTerm original, final Graph graph) {
            super(original, graph);
        }

        URI getRDFType() {
            return anyOrNull(RDF_TYPE, ValueMappings::iriAsUri);
        }

        /**
         * Note 7: In its getters, the ``Node`` class calls WrapperBlankNodeOrIRI
         * method ``anyOrNull`` to return either 0 or 1 value mapped to the predicate.
         * You can use ValueMappings method to convert the value to a specified type.
         * <p>
         * In its setters, the ``Node`` class calls WrapperBlankNodeOrIRI
         * method ``overwriteNullable`` to return either 0 or 1 value mapped to the predicate.
         * You can use TermMappings method to store the value with the specified type information.
         */

        void setRDFType(URI type) {
            overwriteNullable(RDF_TYPE, type, TermMappings::asIri);
        }

        String getMerchantProvider() {
            return anyOrNull(SCHEMA_PROVIDER, ValueMappings::literalAsString);
        }

        void setMerchantProvider(String provider) {
            overwriteNullable(SCHEMA_PROVIDER, provider, TermMappings::asStringLiteral);
        }

        public Date getExpenseDate() {
            Instant expenseInstant = anyOrNull(SCHEMA_DATE, ValueMappings::literalAsInstant);
            if (expenseInstant != null) return Date.from(expenseInstant);
            else return null;
        }

        public void setExpenseDate(Date expenseDate) {
            overwriteNullable(SCHEMA_DATE, expenseDate.toInstant(), TermMappings::asTypedLiteral);
        }

        String getDescription() {
            return anyOrNull(SCHEMA_DESCRIPTION, ValueMappings::literalAsString);
        }

        void setDescription(String description) {
            overwriteNullable(SCHEMA_DESCRIPTION, description, TermMappings::asStringLiteral);
        }

        public BigDecimal getAmount() {
            String priceString = anyOrNull(SCHEMA_TOTAL_PRICE, ValueMappings::literalAsString);
            if (priceString != null) return new BigDecimal(priceString);
            else return null;
        }

        /**
         * Note 8: You can write your own TermMapping helper.
         */
        public void setAmount(BigDecimal totalPrice) {
            overwriteNullable(SCHEMA_TOTAL_PRICE, totalPrice, (final BigDecimal value, final Graph graph) -> {
                Objects.requireNonNull(value, "Value must not be null");
                Objects.requireNonNull(graph, "Graph must not be null");
                return RDFFactory.getInstance().
                    createLiteral(
                        value.toString(),
                        RDFFactory.getInstance().createIRI("http://www.w3.org/2001/XMLSchema#decimal")
                    );
            });
        }

        public String getCurrency() {
            return anyOrNull(SCHEMA_CURRENCY, ValueMappings::literalAsString);
        }

        public void setCurrency(String currency) {
            overwriteNullable(SCHEMA_CURRENCY, currency, TermMappings::asStringLiteral);
        }

        public String getCategory() {
            return anyOrNull(SCHEMA_CATEGORY, ValueMappings::literalAsString);
        }

        public void setCategory(String category) {
            overwriteNullable(SCHEMA_CATEGORY, category, TermMappings::asStringLiteral);
        }

        public Set<String> getReceipts() {
            return objects(SCHEMA_IMAGE, TermMappings::asIri, ValueMappings::iriAsString);
        }

    }
}