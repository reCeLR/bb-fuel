package com.backbase.ct.bbfuel.configurator;

import static com.backbase.ct.bbfuel.data.CommonConstants.EXTERNAL_ROOT_LEGAL_ENTITY_ID;
import static com.backbase.ct.bbfuel.data.CommonConstants.PROPERTY_IDENTITY_FEATURE_TOGGLE;

import com.backbase.ct.bbfuel.client.accessgroup.UserContextPresentationRestClient;
import com.backbase.ct.bbfuel.client.common.LoginRestClient;
import com.backbase.ct.bbfuel.client.user.IdentityIntegrationRestClient;
import com.backbase.ct.bbfuel.client.user.UserIntegrationRestClient;
import com.backbase.ct.bbfuel.data.LegalEntitiesAndUsersDataGenerator;
import com.backbase.ct.bbfuel.dto.LegalEntityWithUsers;
import com.backbase.ct.bbfuel.dto.User;
import com.backbase.ct.bbfuel.service.LegalEntityService;
import com.backbase.ct.bbfuel.util.GlobalProperties;
import com.backbase.integration.legalentity.rest.spec.v2.legalentities.LegalEntitiesPostRequestBody;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LegalEntitiesAndUsersConfigurator {

    private final LoginRestClient loginRestClient;
    private final UserContextPresentationRestClient userContextPresentationRestClient;
    private final UserIntegrationRestClient userIntegrationRestClient;
    private final IdentityIntegrationRestClient identityIntegrationRestClient;
    private final LegalEntityService legalEntityService;
    private final ServiceAgreementsConfigurator serviceAgreementsConfigurator;
    private final GlobalProperties globalProperties;

    /**
     * Dispatch the creation of legal entity depending whether given legalEntityWithUsers is a root entity.
     */
    public void ingestLegalEntityWithUsers(LegalEntityWithUsers legalEntityWithUsers) {
        if (legalEntityWithUsers.getCategory().isRoot()) {
            ingestRootLegalEntityAndEntitlementsAdmin(legalEntityWithUsers);
        } else {
            ingestLegalEntityAndUsers(legalEntityWithUsers);
        }
    }

    private void ingestRootLegalEntityAndEntitlementsAdmin(LegalEntityWithUsers root) {
        String externalLegalEntityId = this.legalEntityService
            .ingestLegalEntity(LegalEntitiesAndUsersDataGenerator
                .generateRootLegalEntitiesPostRequestBody(EXTERNAL_ROOT_LEGAL_ENTITY_ID));
        this.serviceAgreementsConfigurator
            .updateMasterServiceAgreementWithExternalIdByLegalEntity(externalLegalEntityId);

        User admin = root.getUsers().get(0);
        this.userIntegrationRestClient.ingestUserAndLogResponse(LegalEntitiesAndUsersDataGenerator
            .generateUsersPostRequestBody(admin, EXTERNAL_ROOT_LEGAL_ENTITY_ID));
        this.serviceAgreementsConfigurator
            .setEntitlementsAdminUnderMsa(admin.getExternalId(), EXTERNAL_ROOT_LEGAL_ENTITY_ID);
    }

    private void ingestLegalEntityAndUsers(LegalEntityWithUsers legalEntityWithUsers) {
        final LegalEntitiesPostRequestBody requestBody = LegalEntitiesAndUsersDataGenerator
            .composeLegalEntitiesPostRequestBody(
                legalEntityWithUsers.getLegalEntityExternalId(),
                legalEntityWithUsers.getLegalEntityName(),
                legalEntityWithUsers.getParentLegalEntityExternalId(),
                legalEntityWithUsers.getLegalEntityType());

        this.loginRestClient.loginBankAdmin();
        this.userContextPresentationRestClient.selectContextBasedOnMasterServiceAgreement();

        String externalLegalEntityId = this.legalEntityService.ingestLegalEntity(requestBody);

            legalEntityWithUsers.getUsers().parallelStream()
                .forEach(
                    user -> {
                        if(globalProperties.getBoolean(PROPERTY_IDENTITY_FEATURE_TOGGLE)) {
                            this.identityIntegrationRestClient
                                .ingestUserWithIdentityAndLogResponse(LegalEntitiesAndUsersDataGenerator
                                    .generateUsersPostRequestBody(user,externalLegalEntityId));
                        }else{
                            this.userIntegrationRestClient
                                .ingestUserAndLogResponse(LegalEntitiesAndUsersDataGenerator
                                    .generateUsersPostRequestBody(user, externalLegalEntityId));
                        }
                    }
                );
    }
}