package com.backbase.ct.bbfuel.client.user;

import static org.apache.http.HttpStatus.SC_OK;

import com.backbase.ct.bbfuel.client.common.RestClient;
import com.backbase.ct.bbfuel.client.legalentity.LegalEntityPresentationRestClient;
import com.backbase.ct.bbfuel.config.BbFuelConfiguration;
import com.backbase.presentation.legalentity.rest.spec.v2.legalentities.LegalEntityByExternalIdGetResponseBody;
import com.backbase.presentation.user.rest.spec.v2.users.IdentitiesPostRequestBody;
import com.backbase.integration.user.rest.spec.v2.users.UsersPostRequestBody;
import com.backbase.presentation.user.rest.spec.v2.users.LegalEntityByUserGetResponseBody;
import com.backbase.presentation.user.rest.spec.v2.users.UserGetResponseBody;

import javax.annotation.PostConstruct;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserPresentationRestClient extends RestClient {

    private final BbFuelConfiguration config;

    private final LegalEntityPresentationRestClient legalEntityPresentationRestClient;

    private static final String SERVICE_VERSION = "v2";
    private static final String ENDPOINT_USERS = "/users";
    private static final String ENDPOINT_EXTERNAL_ID_LEGAL_ENTITIES = ENDPOINT_USERS + "/externalId/%s/legalentities";
    private static final String ENDPOINT_USER_BY_EXTERNAL_ID = ENDPOINT_USERS + "/externalId/%s";
    private static final String ENDPOINT_USERS_IDENTITIES = ENDPOINT_USERS + "/identities";

    @PostConstruct
    public void init() {
        setBaseUri(config.getPlatform().getGateway());
        setVersion(SERVICE_VERSION);
        setInitialPath(config.getDbsServiceNames().getUser() + "/" + CLIENT_API);
    }

    public LegalEntityByUserGetResponseBody retrieveLegalEntityByExternalUserId(String externalUserId) {
        return requestSpec()
                .get(String.format(getPath(ENDPOINT_EXTERNAL_ID_LEGAL_ENTITIES), externalUserId))
                .then()
                .statusCode(SC_OK)
                .extract()
                .as(LegalEntityByUserGetResponseBody.class);
    }

    public UserGetResponseBody getUserByExternalId(String userExternalId) {
        return requestSpec()
                .get(String.format(getPath(ENDPOINT_USER_BY_EXTERNAL_ID), userExternalId))
                .then()
                .statusCode(SC_OK)
                .extract()
                .as(UserGetResponseBody.class);
    }

    public Response ingestUserWithIdentity(UsersPostRequestBody body) {
        IdentitiesPostRequestBody identityPost = new IdentitiesPostRequestBody();
        LegalEntityByExternalIdGetResponseBody le = legalEntityPresentationRestClient.retrieveLegalEntityByExternalId(body.getLegalEntityExternalId());

        identityPost
                .withLegalEntityInternalId(le.getId())
                .withEmailAddress("custom@backbase.com")
                .withExternalId(body.getExternalId())
                .withFamilyName(body.getFullName())
                .withGivenName(body.getFullName())
                .withMobileNumber("07700900414")
                .withFullName(body.getFullName());

        return requestSpec()
                .contentType(ContentType.JSON)
                .body(identityPost)
                .post(getPath(ENDPOINT_USERS_IDENTITIES));
    }

}
