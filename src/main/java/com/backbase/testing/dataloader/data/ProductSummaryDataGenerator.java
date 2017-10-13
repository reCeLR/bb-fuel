package com.backbase.testing.dataloader.data;

import com.backbase.integration.arrangement.rest.spec.v2.arrangements.DebitCard;
import com.backbase.testing.dataloader.utils.CommonHelpers;
import com.backbase.testing.dataloader.utils.ParserUtil;
import com.backbase.integration.arrangement.rest.spec.v2.arrangements.ArrangementsPostRequestBody;
import com.backbase.integration.arrangement.rest.spec.v2.arrangements.ArrangementsPostRequestBodyParent;
import com.backbase.integration.product.rest.spec.v2.products.ProductsPostRequestBody;
import com.github.javafaker.Faker;
import org.iban4j.CountryCode;
import org.iban4j.Iban;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

import static com.backbase.testing.dataloader.data.CommonConstants.PRODUCTS_JSON;

public class ProductSummaryDataGenerator {

    private Faker faker = new Faker();
    private Random random = new Random();
    private static final List<CountryCode> COUNTRY_CODES;
    static {
        List<String> allowed = Arrays.asList("AT", "BE", "BG", "CH", "CY", "CZ", "DE", "DK", "EE", "ES", "FI", "FR", "GB", "GI", "GR", "HR", "HU", "IE", "IS", "IT", "LI", "LT", "LU", "LV", "MC", "MT", "NL", "NO", "PL", "PT", "RO", "SE", "SI", "SK", "SM");
        COUNTRY_CODES = new ArrayList<>();
        CountryCode[] values = CountryCode.values();
        for (CountryCode code : values) {
            if (allowed.contains(code.name())) {
                COUNTRY_CODES.add(code);
            }
        }
    }

    private String generateRandomIban() {
        return Iban.random(COUNTRY_CODES.get(random.nextInt(COUNTRY_CODES.size()))).toString();
    }

    public ProductsPostRequestBody[] generateProductsPostRequestBodies() throws IOException {
        return ParserUtil.convertJsonToObject(PRODUCTS_JSON, ProductsPostRequestBody[].class);
    }

    public ArrangementsPostRequestBody generateArrangementsPostRequestBody(String externalLegalEntityId) {
        ArrangementsPostRequestBodyParent.AccountHolderCountry[] accountHolderCountries = ArrangementsPostRequestBodyParent.AccountHolderCountry.values();
        int productId = CommonHelpers.generateRandomNumberInRange(1, 7);
        boolean debitCreditAccountIndicator = false;
        final HashSet<DebitCard> debitCards = new HashSet<>();

        if (productId == 1 || productId == 2) {
            debitCreditAccountIndicator = true;
        }

        if (productId == 1) {
            for (int i = 0; i < CommonHelpers.generateRandomNumberInRange(3, 10); i++) {
                debitCards.add(new DebitCard().withNumber(String.format("%s", CommonHelpers.generateRandomNumberInRange(1111, 9999)))
                        .withExpiryDate(faker.business()
                                .creditCardExpiry()));
            }
        }

        return new ArrangementsPostRequestBody().withId(UUID.randomUUID().toString())
                .withLegalEntityId(externalLegalEntityId)
                .withProductId(String.format("%s", productId))
                .withName(faker.lorem().sentence(3, 0).replace(".", ""))
                .withAlias(faker.lorem().characters(10))
                .withBookedBalance(CommonHelpers.generateRandomAmountInRange(10000L, 9999999L))
                .withAvailableBalance(CommonHelpers.generateRandomAmountInRange(10000L, 9999999L))
                .withCreditLimit(CommonHelpers.generateRandomAmountInRange(10000L, 999999L))
                .withIBAN(generateRandomIban())
                .withBBAN(faker.lorem().characters(20).toUpperCase())
                .withCurrency(ArrangementsPostRequestBodyParent.Currency.EUR)
                .withExternalTransferAllowed(true)
                .withUrgentTransferAllowed(true)
                .withAccruedInterest(BigDecimal.valueOf(random.nextInt(10)))
                .withNumber(String.format("%s", random.nextInt(9999)))
                .withPrincipalAmount(CommonHelpers.generateRandomAmountInRange(10000L, 999999L))
                .withCurrentInvestmentValue(CommonHelpers.generateRandomAmountInRange(10000L, 999999L))
                .withDebitAccount(debitCreditAccountIndicator)
                .withCreditAccount(debitCreditAccountIndicator)
                .withDebitCards(debitCards)
                .withAccountHolderName(faker.name().fullName())
                .withAccountHolderAddressLine1(faker.address().streetAddress())
                .withAccountHolderAddressLine2(faker.address().secondaryAddress())
                .withAccountHolderAddressLine3(faker.address().cityName())
                .withAccountHolderAddressLine4(faker.address().zipCode())
                .withAccountHolderCountry(accountHolderCountries[CommonHelpers.generateRandomNumberInRange(0, accountHolderCountries.length - 1)]);
    }
}
