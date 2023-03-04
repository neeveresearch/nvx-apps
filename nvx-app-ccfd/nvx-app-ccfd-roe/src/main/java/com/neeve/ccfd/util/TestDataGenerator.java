package com.neeve.ccfd.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Random;

import com.eaio.uuid.UUID;
import com.eaio.uuid.UUIDGen;
import com.neeve.ccfd.messages.NewCardHolderMessage;
import com.neeve.ccfd.messages.NewMerchantMessage;
import com.neeve.ccfd.messages.NewMerchantStoreDTO;
import com.neeve.ccfd.messages.PaymentTransactionDTO;
import com.neeve.ccfd.messages.TransformedPaymentTransactionDTO;
import com.neeve.ci.XRuntime;
import com.neeve.lang.XString;
import com.neeve.trace.Tracer;
import com.neeve.trace.Tracer.Level;

/**
 * Generates sample data set. Fields in the set take values from Gaussian distribution.
 */
public class TestDataGenerator {
    private static final Tracer tracer = Tracer.get("ccdfd");
    private static final int txnDataLoadCount = XRuntime.getValue("ccfd.txnDataLoadCount", 64 * 1024);
    private static final float[][] txnData;
    private static final String fieldNames[];

    private static final String CARD_NUMBER_BANK_ID = "123";
    private static final int CARD_NUMBER_LENGTH = 16;
    private static final Random random = new Random();
    //    private static final String[] COUNTRY_CODES = { "US" };
    //    private static final String[] ZIP_CODES = { "85001", "99501" };
    public static final String DEFAULT_COUNTRY_CODE = "US";
    public static final String DEFAULT_POSTAL_CODE = "99501";
    public static final String DEFAULT_MERCHANT_NAME = "ACME Testing inc.";
    public static final String DEFAULT_STORE_NAME = "Super Testing Discounts";
    public static final String DEFAULT_STREET_ADDRESS = "Testington Square";

    static {
        tracer.log("Loading transaction data...", Level.INFO);
        InputStream sourceDataStream = TestDataGenerator.class.getResourceAsStream("/normalized-txn-data.csv");
        if (sourceDataStream == null) {
            throw new RuntimeException("'/normalized-txn-data.csv' containing normalized not found on classpath!");
        }
        BufferedReader sourceTransactions = new BufferedReader(new InputStreamReader(sourceDataStream));
        try {
            String line = sourceTransactions.readLine();
            fieldNames = line.split(",");
            txnData = new float[txnDataLoadCount][fieldNames.length];
            int i = 0;
            int fraudulent = 0;

            for (; i < txnDataLoadCount; i++) {
                line = sourceTransactions.readLine();
                if (line == null) {
                    break;
                }
                String[] features = line.split(",");
                for (int f = 0; f < txnData[i].length; f++) {
                    txnData[i][f] = Float.valueOf(features[f]);
                }

                //Last column is the Fraud column, 1 if actually fraudulent
                if (i > 0 && txnData[i][fieldNames.length - 1] > 0.1) {
                    fraudulent++;
                }
                if (tracer.debug) tracer.log("Loaded " + line, Level.DEBUG);
            }
            tracer.log("Loaded " + i + " transactions (" + fraudulent + " fraudulent - " + String.format("%.3fpct", (fraudulent / (float)i)) + ")...", Level.INFO);
        }
        catch (Exception e) {
            throw new RuntimeException("Unable to load transaction data: " + e.getMessage(), e);
        }
        finally {
            try {
                sourceTransactions.close();
            }
            catch (IOException e) {
                tracer.log("Error closing transaction data stream [" + e.getMessage() + "]", Level.WARNING);
            }
        }
    }

    /**
     * Constructs generator instance. 
     *  
     * @param fieldsCount Number of fields used in analysis.
     */
    public TestDataGenerator(final int fieldsCount) {

    }

    static final ThreadLocal<UUID> uuidGenerator = new ThreadLocal<UUID>();
    static final ThreadLocal<XString> stringIdGenerator = new ThreadLocal<XString>();

    /**
     * Generates String id field from UUID.
     */
    public static final XString generateIdTo(XString target) {
        UUID uuid = uuidGenerator.get();
        if (uuid == null) {
            uuid = new UUID(0, 0);
            uuidGenerator.set(uuid);
        }
        uuid.time = UUIDGen.newTime();
        uuid.clockSeqAndNode = UUIDGen.getClockSeqAndNode();
        target.reset();
        uuid.toAppendable(target);
        return target;
    }

    /**
     * Gets a thread local XString for temporary use. 
     */
    public static final XString tempIdHolder() {
        XString id = stringIdGenerator.get();
        if (id == null) {
            id = XString.create(32, true, true);
            stringIdGenerator.set(id);
        }
        return id;
    }

    /**
     * Generates a random card number.
     * 
     * Adapted from:
     * https://www.journaldev.com/1449/credit-card-check-digit-generator-java-program
     * 
     */
    public static final String generateCardNumber() {
        // The number of random digits that we need to generate is equal to the
        // total length of the card number minus the start digits given by the
        // user, minus the check digit at the end.
        int randomNumberLength = CARD_NUMBER_LENGTH - (CARD_NUMBER_BANK_ID.length() + 1);

        StringBuilder builder = new StringBuilder(CARD_NUMBER_BANK_ID);
        for (int i = 0; i < randomNumberLength; i++) {
            int digit = random.nextInt(10);
            builder.append(digit);
        }

        // Do the Luhn algorithm to generate the check digit.
        int checkDigit = getCheckDigit(builder.toString());
        builder.append(checkDigit);

        return builder.toString();
    }

    /**
     * Generates the check digit required to make the given credit card number
     * valid (i.e. pass the Luhn check)
     *
     * @param number
     *            The credit card number for which to generate the check digit.
     * @return The check digit required to make the given credit card number
     *         valid.
     */
    private static int getCheckDigit(String number) {

        // Get the sum of all the digits, however we need to replace the value
        // of the first digit, and every other digit, with the same digit
        // multiplied by 2. If this multiplication yields a number greater
        // than 9, then add the two digits together to get a single digit
        // number.
        //
        // The digits we need to replace will be those in an even position for
        // card numbers whose length is an even number, or those is an odd
        // position for card numbers whose length is an odd number. This is
        // because the Luhn algorithm reverses the card number, and doubles
        // every other number starting from the second number from the last
        // position.
        int sum = 0;
        for (int i = 0; i < number.length(); i++) {

            // Get the digit at the current position.
            int digit = Integer.parseInt(number.substring(i, (i + 1)));

            if ((i % 2) == 0) {
                digit = digit * 2;
                if (digit > 9) {
                    digit = (digit / 10) + (digit % 10);
                }
            }
            sum += digit;
        }

        // The check digit is the number required to make the sum a multiple of
        // 10.
        int mod = sum % 10;
        return ((mod == 0) ? 0 : 10 - mod);
    }

    /**
     * Generate random fields for data set. 
     *  
     * @param isFraud If true - generate fraudulent transaction. 
     *  
     * @param fraudIndicators indexes of fields that will deviate and indicate fraudulent behavior. 
     *  
     * @param transaction transaction which to fill with random values
     */
    public void populateNormalizedFeatureFields(final boolean isFraud, final int[] fraudIndicators, final TransformedPaymentTransactionDTO transaction) {
        float[] features = txnData[random.nextInt(txnData.length)];

        int f = 0;
        transaction.setTimeNormal(features[f++]);
        transaction.setV1(features[f++]);
        transaction.setV2(features[f++]);
        transaction.setV3(features[f++]);
        transaction.setV4(features[f++]);
        transaction.setV5(features[f++]);
        transaction.setV6(features[f++]);
        transaction.setV7(features[f++]);
        transaction.setV9(features[f++]);
        transaction.setV10(features[f++]);
        transaction.setV11(features[f++]);
        transaction.setV12(features[f++]);
        transaction.setV14(features[f++]);
        transaction.setV16(features[f++]);
        transaction.setV17(features[f++]);
        transaction.setV18(features[f++]);
        transaction.setV19(features[f++]);
        transaction.setV21(features[f++]);
        transaction.setAmountNormal(features[f++]);
        transaction.setAmountMaxFraudNormal(features[f++]);
        transaction.setV1_(features[f++]);
        transaction.setV2_(features[f++]);
        transaction.setV3_(features[f++]);
        transaction.setV4_(features[f++]);
        transaction.setV5_(features[f++]);
        transaction.setV6_(features[f++]);
        transaction.setV7_(features[f++]);
        transaction.setV9_(features[f++]);
        transaction.setV10_(features[f++]);
        transaction.setV11_(features[f++]);
        transaction.setV12_(features[f++]);
        transaction.setV14_(features[f++]);
        transaction.setV16_(features[f++]);
        transaction.setV17_(features[f++]);
        transaction.setV18_(features[f++]);
        transaction.setV19_(features[f++]);
        transaction.setV21_(features[f++]);
    }

    /**
     * Generates data for new merchant. 
     *  
     * @param storesCount Desired number of stores under merchant. 
     *  
     * @return Merchant data with stores
     */
    public NewMerchantMessage generateNewMerchantMessage(final int storesCount) {
        // TODO improve by generating name sequences in string fields
        NewMerchantMessage merchantMessage = NewMerchantMessage.create();
        merchantMessage.setMerchantIdFrom(generateIdTo(tempIdHolder()));
        merchantMessage.setName(DEFAULT_MERCHANT_NAME);
        merchantMessage.setAddress(DEFAULT_STREET_ADDRESS);
        merchantMessage.setCountryCode(DEFAULT_COUNTRY_CODE);
        merchantMessage.setPostOrZip(DEFAULT_POSTAL_CODE);

        for (int i = 0; i < storesCount; i++) {
            NewMerchantStoreDTO merchantStore = NewMerchantStoreDTO.create();
            merchantStore.setStoreIdFrom(generateIdTo(tempIdHolder()));
            merchantStore.setName(DEFAULT_STORE_NAME);
            merchantStore.setAddress(DEFAULT_STREET_ADDRESS);
            merchantStore.setCountryCode(DEFAULT_COUNTRY_CODE);
            merchantStore.setPostOrZip(DEFAULT_POSTAL_CODE);

            merchantMessage.addStores(merchantStore);
        }

        return merchantMessage;
    }

    /**
     * Generate new card holder populated with random data 
     *  
     * @param transactions number of transaction for history to hold. Must be divisible by number of cards. 
     *  
     * @param cards number of cards. 
     *  
     * @param merchantId id of merchant where purchases took place 
     *  
     * @param merchantStoreId id of merchant's store where purchases took place 
     *  
     * @return new card holder. 
     *  
     */
    public NewCardHolderMessage generateCardHolderMessage(final int transactions, final int cards, final XString merchantId, final XString merchantStoreId) throws Exception {
        // For testing we can use UUID as card identifier. We just need unique string value
        if (transactions % cards > 0) {
            throw new IllegalArgumentException("Number of transactions must be divisible by number of cards");
        }

        NewCardHolderMessage newCardHolderMessage = NewCardHolderMessage.create();
        newCardHolderMessage.setRequestIdFrom(generateIdTo(tempIdHolder()));
        newCardHolderMessage.setCardHolderIdFrom(generateIdTo(tempIdHolder()));
        int transactionsPerCard = transactions / cards;
        for (int i = 0; i < cards; i++) {
            XString cardNumber = generateIdTo(tempIdHolder());
            newCardHolderMessage.addCardNumbers(cardNumber.getValue());
            for (int j = 0; j < transactionsPerCard; j++) {
                // TODO this should demonstrate merchant lookup, 
                // but we should improve by having different stores per card holder 
                PaymentTransactionDTO newTransaction = generateTransactionMessage(cardNumber, merchantId, merchantStoreId);
                newCardHolderMessage.addHistory(newTransaction);
            }
        }

        return newCardHolderMessage;
    }

    /**
     * Generates transaction message populated with random field values 
     *  
     * @param cardNumber The credit card number
     * @param merchantId The merchant id
     * @param merchantStoreId  The merchant store id.
     *  
     * @return payment transaction message
     */
    public PaymentTransactionDTO generateTransactionMessage(final XString cardNumber,
                                                            final XString merchantId,
                                                            final XString merchantStoreId) {
        if (cardNumber == null || cardNumber.length() == 0) {
            throw new IllegalArgumentException("'cardNumber' cannot be null or empty string.");
        }
        if (merchantId == null || merchantId.length() == 0) {
            throw new IllegalArgumentException("'merchantId' cannot be null or empty string.");
        }

        PaymentTransactionDTO newTransaction = PaymentTransactionDTO.create();
        newTransaction.setCardNumberFrom(cardNumber);
        newTransaction.setMerchantIdFrom(merchantId);
        newTransaction.setMerchantStoreIdFrom(merchantStoreId);

        newTransaction.setAmount(10 + random.nextGaussian() * 1000);

        return newTransaction;
    }

    private static final int fraudIndicators[] = {};

    /**
     * Generates transaction message populated with random field values to represent statistically transformed transaction 
     *  
     * @param transaction to 'transform' 
     *  
     * @return transformed transaction
     */
    public TransformedPaymentTransactionDTO generateTransformedTransactionMessage(final PaymentTransactionDTO transaction) {

        TransformedPaymentTransactionDTO newTransaction = TransformedPaymentTransactionDTO.create();
        newTransaction.setTransactionIdFrom(transaction.getTransactionIdUnsafe());
        newTransaction.setCardNumberFrom(transaction.getCardNumberUnsafe());
        newTransaction.setAmount(transaction.getAmount());
        newTransaction.setMerchantIdFrom(transaction.getMerchantIdUnsafe());
        newTransaction.setMerchantStoreIdFrom(transaction.getMerchantStoreIdUnsafe());
        populateNormalizedFeatureFields(false, fraudIndicators, newTransaction);

        return newTransaction;
    }

    /**
     * Generates transaction message populated with random field values to represent statistically transformed transaction. 
     * It generates all the ID fields. 
     *  
     * @return transformed transaction
     */
    public TransformedPaymentTransactionDTO generateTransformedTransactionMessage() {
        TransformedPaymentTransactionDTO newTransaction = TransformedPaymentTransactionDTO.create();
        newTransaction.setTransactionIdFrom(generateIdTo(tempIdHolder()));
        newTransaction.setCardNumberFrom(generateIdTo(tempIdHolder()));
        newTransaction.setAmount(10 + random.nextGaussian() * 1000);
        newTransaction.setMerchantIdFrom(generateIdTo(tempIdHolder()));
        newTransaction.setMerchantStoreIdFrom(generateIdTo(tempIdHolder()));
        populateNormalizedFeatureFields(false, fraudIndicators, newTransaction);

        return newTransaction;
    }
}
