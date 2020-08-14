package com.backbase.ct.bbfuel.client.user;

import static com.backbase.ct.bbfuel.util.ResponseUtils.isBadRequestException;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_OK;

import com.backbase.ct.bbfuel.client.common.RestClient;
import com.backbase.ct.bbfuel.client.legalentity.LegalEntityPresentationRestClient;
import com.backbase.ct.bbfuel.config.BbFuelConfiguration;
import com.backbase.integration.user.rest.spec.v2.users.UsersPostRequestBody;
import com.backbase.presentation.user.rest.spec.v2.users.IdentitiesPostRequestBody;
import com.backbase.presentation.user.rest.spec.v2.users.LegalEntityByUserGetResponseBody;
import com.backbase.presentation.user.rest.spec.v2.users.UserGetResponseBody;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserPresentationRestClient extends RestClient {

    private final BbFuelConfiguration config;
    private final LegalEntityPresentationRestClient legalEntityPresentationRestClient;


    private static final String SERVICE_VERSION = "v2";
    private static final String ENDPOINT_USERS = "/users";
    private static final String ENDPOINT_EXTERNAL_ID_LEGAL_ENTITIES = ENDPOINT_USERS + "/externalId/%s/legalentities";
    private static final String ENDPOINT_USER_BY_EXTERNAL_ID = ENDPOINT_USERS + "/externalId/%s";
    private static final String ENDPOINT_IDENTITIES = ENDPOINT_USERS + "/identities";

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

    public void createIdentityUserAndLogResponse(UsersPostRequestBody user){

        Response response = createIdentity(user);


        if (isBadRequestException(response, "User already exists")) {
            log.info("User [{}] already exists, skipped ingesting this user", user.getExternalId());
        } else if (response.statusCode() == SC_CREATED) {
            log.info("Identity User [{}] ingested under legal entity [{}]",
                user.getExternalId(), user.getLegalEntityExternalId());
        } else {
            response.then()
                .statusCode(SC_CREATED);
        }


    }

    private Response createIdentity(UsersPostRequestBody user){

        IdentitiesPostRequestBody createUserBody = new IdentitiesPostRequestBody();

        createUserBody
            .withExternalId(user.getExternalId())
            .withLegalEntityInternalId(legalEntityPresentationRestClient.retrieveLegalEntityByExternalId(user.getLegalEntityExternalId()).getId())
            .withFullName(user.getFullName())
            .withEmailAddress(user.getExternalId() + "@email.com");

        return requestSpec()
            .contentType(ContentType.JSON)
            .body(createUserBody)
            .post(getPath(ENDPOINT_IDENTITIES));
    }

}
