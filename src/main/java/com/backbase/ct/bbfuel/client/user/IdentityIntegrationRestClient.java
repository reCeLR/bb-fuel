package com.backbase.ct.bbfuel.client.user;


import static com.backbase.ct.bbfuel.util.ResponseUtils.isBadRequestException;
import static com.backbase.ct.bbfuel.util.ResponseUtils.isConflictException;
import static org.apache.http.HttpStatus.SC_CREATED;


import com.backbase.ct.bbfuel.client.common.RestClient;
import com.backbase.ct.bbfuel.config.BbFuelConfiguration;
import com.backbase.integration.user.rest.spec.v2.users.UsersPostRequestBody;
import com.backbase.presentation.user.rest.spec.v2.users.IdentitiesPostRequestBody;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.Arrays;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class IdentityIntegrationRestClient extends RestClient {

    private final BbFuelConfiguration config;
    private final UserPresentationRestClient userPresentationRestClient;
    private final UserIntegrationRestClient userIntegrationRestClient;
    private static final String SERVICE_VERSION = "v1";
    private static final String ENDPOINT_USERS_BY_EXTERNAL_ID = "/backbase/users?username=%s";


    @PostConstruct
    public void init() {
        setBaseUri(config.getPlatform().getIdentity());
        setVersion(SERVICE_VERSION);
    }


    public void ingestUserWithIdentityAndLogResponse(UsersPostRequestBody user) {
        Response response;

        if(isInIdentity(user)){
            UsersPostRequestBody importBody = new UsersPostRequestBody();
            importBody
                .withExternalId(user.getExternalId())
                .withLegalEntityExternalId(user.getLegalEntityExternalId());

            response = userIntegrationRestClient.importUserIdentity(importBody);

        }else{
            IdentitiesPostRequestBody createUserBody = new IdentitiesPostRequestBody();

            createUserBody
                .withExternalId(user.getExternalId())
                .withLegalEntityInternalId(userPresentationRestClient.retrieveLegalEntityByExternalUserId(user.getExternalId()).getId())
                .withFullName(user.getFullName())
                .withEmailAddress(user.getExternalId() + "@email.com");

            response = userPresentationRestClient.CreateIdentityUser(createUserBody);

        }

        if (isBadRequestException(response, "User already exists") || isConflictException(response, "Conflict Exception")) {
            log.info("User [{}] already exists, skipped ingesting this user", user.getExternalId());
        } else if (response.statusCode() == SC_CREATED) {
            log.info("User [{}] ingested under legal entity [{}]",
                user.getExternalId(), user.getLegalEntityExternalId());
        } else {
            response.then()
                .statusCode(SC_CREATED);
        }
    }


    /* TODO:
        requests array of users with a partial match to the username/externalID
        searches for an exact match
        if found return true (user is present in identity)
        if not return false (user not present in identity)
     */
    private boolean isInIdentity(UsersPostRequestBody body) {

        Response users = requestSpec()
            .contentType(ContentType.JSON)
            .body(body)
            .get(String.format(getPath(ENDPOINT_USERS_BY_EXTERNAL_ID),body.getExternalId()));

        return Arrays.asList(users).contains(body.getExternalId());

    }


}