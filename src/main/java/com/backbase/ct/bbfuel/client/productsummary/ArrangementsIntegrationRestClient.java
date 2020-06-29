package com.backbase.ct.bbfuel.client.productsummary;

import static com.backbase.ct.bbfuel.util.ResponseUtils.isBadRequestExceptionWithErrorKey;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_OK;
import static java.util.Arrays.asList;

import com.backbase.ct.bbfuel.client.common.RestClient;
import com.backbase.ct.bbfuel.config.BbFuelConfiguration;
import com.backbase.integration.arrangement.rest.spec.v2.arrangements.ArrangementItemResponseBody;
import com.backbase.integration.arrangement.rest.spec.v2.arrangements.ArrangementsPostRequestBody;
import com.backbase.integration.arrangement.rest.spec.v2.arrangements.ArrangementsPostResponseBody;
import com.backbase.integration.arrangement.rest.spec.v2.balancehistory.BalanceHistoryPostRequestBody;
import com.backbase.integration.arrangement.rest.spec.v2.products.ProductsPostRequestBody;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArrangementsIntegrationRestClient extends RestClient {

    private final BbFuelConfiguration config;

    private static final String SERVICE_VERSION = "v2";
    private static final String ENDPOINT_ARRANGEMENTS = "/arrangements";
    private static final String ENDPOINT_PRODUCTS = "/products";
    private static final String ENDPOINT_BALANCE_HISTORY = "/balance-history";

    @PostConstruct
    public void init() {
        setBaseUri(config.getDbs().getArrangements());
        setVersion(SERVICE_VERSION);
    }

    public ArrangementItemResponseBody getArrangementByExternalId(String id) {
        try {
            List<ArrangementItemResponseBody> items = asList(requestSpec()
                    .contentType(ContentType.JSON)
                    .get(getPath(ENDPOINT_ARRANGEMENTS) + "?ids="+id)
                    .then()
                    .extract()
                    .as(ArrangementItemResponseBody[].class));
            if (items.size() > 0) {
                log.info("More than one arrangement with id [{}] found", id);
            }
            return items.get(0);
        } catch (Exception e) {
            //do nothing
            return null;
        }
    }

    public ArrangementsPostResponseBody ingestArrangement(ArrangementsPostRequestBody body) {
        return requestSpec()
            .contentType(ContentType.JSON)
            .body(body)
            .post(getPath(ENDPOINT_ARRANGEMENTS))
            .then()
            .statusCode(SC_CREATED)
            .extract()
            .as(ArrangementsPostResponseBody.class);
    }

    public void ingestProductAndLogResponse(ProductsPostRequestBody product) {
        Response response = ingestProduct(product);

        if (isBadRequestExceptionWithErrorKey(response, "account.api.product.alreadyExists")) {
            log.info("Product [{}] already exists, skipped ingesting this product", product.getProductKindName());
        } else if (response.statusCode() == SC_CREATED) {
            log.info("Product [{}] ingested", product.getProductKindName());
        } else {
            response.then()
                .statusCode(SC_CREATED);
        }
    }

    public Response ingestBalance(BalanceHistoryPostRequestBody balanceHistoryPostRequestBody) {
        return requestSpec()
            .contentType(ContentType.JSON)
            .body(balanceHistoryPostRequestBody)
            .post(getPath(ENDPOINT_BALANCE_HISTORY));
    }

    private Response ingestProduct(ProductsPostRequestBody body) {
        return requestSpec()
            .contentType(ContentType.JSON)
            .body(body)
            .post(getPath(ENDPOINT_PRODUCTS));
    }
}
