package com.backbase.testing.dataloader.setup;

import com.backbase.presentation.accessgroup.rest.spec.v2.accessgroups.function.FunctionAccessGroupsGetResponseBody;
import com.backbase.presentation.user.rest.spec.v2.users.LegalEntityByUserGetResponseBody;
import com.backbase.testing.dataloader.clients.accessgroup.AccessGroupPresentationRestClient;
import com.backbase.testing.dataloader.clients.common.LoginRestClient;
import com.backbase.testing.dataloader.clients.user.UserPresentationRestClient;
import com.backbase.testing.dataloader.configurators.AccessGroupsConfigurator;
import com.backbase.testing.dataloader.configurators.ContactsConfigurator;
import com.backbase.testing.dataloader.configurators.LegalEntitiesAndUsersConfigurator;
import com.backbase.testing.dataloader.configurators.PermissionsConfigurator;
import com.backbase.testing.dataloader.configurators.ProductSummaryConfigurator;
import com.backbase.testing.dataloader.configurators.TransactionsConfigurator;
import com.backbase.testing.dataloader.dto.ArrangementId;
import com.backbase.testing.dataloader.utils.GlobalProperties;
import com.backbase.testing.dataloader.utils.ParserUtil;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.backbase.testing.dataloader.data.CommonConstants.EXTERNAL_ROOT_LEGAL_ENTITY_ID;
import static com.backbase.testing.dataloader.data.CommonConstants.PROPERTY_INGEST_CONTACTS;
import static com.backbase.testing.dataloader.data.CommonConstants.PROPERTY_INGEST_NOTIFICATIONS;
import static com.backbase.testing.dataloader.data.CommonConstants.PROPERTY_INGEST_TRANSACTIONS;
import static com.backbase.testing.dataloader.data.CommonConstants.PROPERTY_USERS_JSON_LOCATION;
import static com.backbase.testing.dataloader.data.CommonConstants.PROPERTY_USERS_WITHOUT_PERMISSIONS;
import static com.backbase.testing.dataloader.data.CommonConstants.USERS_JSON_EXTERNAL_USER_IDS_FIELD;
import static com.backbase.testing.dataloader.data.CommonConstants.USER_ADMIN;
import static org.apache.http.HttpStatus.SC_OK;

public class UsersSetup {

    private GlobalProperties globalProperties = GlobalProperties.getInstance();
    private LoginRestClient loginRestClient = new LoginRestClient();
    private UserPresentationRestClient userPresentationRestClient = new UserPresentationRestClient();
    private ProductSummaryConfigurator productSummaryConfigurator = new ProductSummaryConfigurator();
    private AccessGroupPresentationRestClient accessGroupPresentationRestClient = new AccessGroupPresentationRestClient();
    private AccessGroupsConfigurator accessGroupsConfigurator = new AccessGroupsConfigurator();
    private PermissionsConfigurator permissionsConfigurator = new PermissionsConfigurator();
    private TransactionsConfigurator transactionsConfigurator = new TransactionsConfigurator();
    private LegalEntitiesAndUsersConfigurator legalEntitiesAndUsersConfigurator = new LegalEntitiesAndUsersConfigurator();
    private ContactsConfigurator contactsConfigurator = new ContactsConfigurator();

    public void setupUsersWithAndWithoutFunctionDataGroupsPrivileges() throws IOException {
        List<HashMap<String, List<String>>> userLists = ParserUtil.convertJsonToObject(globalProperties.getString(PROPERTY_USERS_JSON_LOCATION), new TypeReference<List<HashMap<String, List<String>>>>() {
        });
        List<HashMap<String, List<String>>> usersWithoutPermissionsLists = ParserUtil.convertJsonToObject(globalProperties.getString(PROPERTY_USERS_WITHOUT_PERMISSIONS), new TypeReference<List<HashMap<String, List<String>>>>() {
        });

        for (Map<String, List<String>> userList : userLists) {
            List<String> externalUserIds = userList.get(USERS_JSON_EXTERNAL_USER_IDS_FIELD);

            setupUsersWithAllFunctionDataGroupsAndPrivilegesUnderNewLegalEntity(externalUserIds);
        }

        for (Map<String, List<String>> usersWithoutPermissionsList : usersWithoutPermissionsLists) {
            List<String> externalUserIds = usersWithoutPermissionsList.get(USERS_JSON_EXTERNAL_USER_IDS_FIELD);

            legalEntitiesAndUsersConfigurator.ingestUsersUnderNewLegalEntity(externalUserIds, EXTERNAL_ROOT_LEGAL_ENTITY_ID);
        }
    }

    public void setupContactsPerUser() throws IOException {
        if (globalProperties.getBoolean(PROPERTY_INGEST_CONTACTS)) {
            List<HashMap<String, List<String>>> userLists = ParserUtil.convertJsonToObject(globalProperties.getString(PROPERTY_USERS_JSON_LOCATION), new TypeReference<List<HashMap<String, List<String>>>>() {
            });
            for (Map<String, List<String>> userList : userLists) {
                List<String> externalUserIds = userList.get(USERS_JSON_EXTERNAL_USER_IDS_FIELD);

                externalUserIds.forEach(externalUserId -> {
                    loginRestClient.login(externalUserId, externalUserId);
                    contactsConfigurator.ingestContacts();
                });
            }
        }
    }

    private void setupUsersWithAllFunctionDataGroupsAndPrivilegesUnderNewLegalEntity(List<String> externalUserIds) {
        Map<String, LegalEntityByUserGetResponseBody> userLegalEntities = new HashMap<>();
        Set<LegalEntityByUserGetResponseBody> legalEntities = new HashSet<>();

        legalEntitiesAndUsersConfigurator.ingestUsersUnderNewLegalEntity(externalUserIds, EXTERNAL_ROOT_LEGAL_ENTITY_ID);

        for (String externalUserId : externalUserIds) {
            loginRestClient.login(USER_ADMIN, USER_ADMIN);

            userLegalEntities.put(externalUserId, userPresentationRestClient.retrieveLegalEntityByExternalUserId(externalUserId)
                    .then()
                    .statusCode(SC_OK)
                    .extract()
                    .as(LegalEntityByUserGetResponseBody.class));
        }

        legalEntities.addAll(userLegalEntities.values());
        legalEntities.forEach(this::setupFunctionDataGroupsUnderLegalEntity);

        for (Map.Entry<String, LegalEntityByUserGetResponseBody> entry : userLegalEntities.entrySet()) {
            String externalUserId = entry.getKey();
            LegalEntityByUserGetResponseBody legalEntity = entry.getValue();

            permissionsConfigurator.assignAllFunctionDataGroupsOfLegalEntityToUserAndServiceAgreement(legalEntity.getExternalId(), externalUserId, null);
        }
    }

    private void setupFunctionDataGroupsUnderLegalEntity(LegalEntityByUserGetResponseBody legalEntity) {
        List<ArrangementId> arrangementIds = productSummaryConfigurator.ingestArrangementsByLegalEntityAndReturnArrangementIds(legalEntity.getExternalId());
        List<String> internalArrangementIds = new ArrayList<>();

        arrangementIds.forEach(arrangementId -> internalArrangementIds.add(arrangementId.getInternalArrangementId()));

        FunctionAccessGroupsGetResponseBody[] functionGroups = accessGroupPresentationRestClient.retrieveFunctionGroupsByLegalEntity(legalEntity.getId())
                .then()
                .statusCode(SC_OK)
                .extract()
                .as(FunctionAccessGroupsGetResponseBody[].class);

        if (functionGroups.length == 0) {
            accessGroupsConfigurator.ingestFunctionGroupsWithAllPrivilegesForAllFunctions(legalEntity.getExternalId());
        }

        accessGroupsConfigurator.ingestDataGroupForArrangements(legalEntity.getExternalId(), internalArrangementIds);

        if (globalProperties.getBoolean(PROPERTY_INGEST_TRANSACTIONS)) {
            for (ArrangementId arrangementId : arrangementIds) {
                transactionsConfigurator.ingestTransactionsByArrangement(arrangementId.getExternalArrangementId());
            }
        }
    }
}
