package com.neeve.ccfd.util;

import java.util.Random;
import java.util.UUID;

import com.neeve.ccfd.messages.NewCardHolderMessage;
import com.neeve.ccfd.messages.NewMerchantMessage;
import com.neeve.ccfd.messages.NewMerchantStoreDTO;
import com.neeve.ccfd.messages.PaymentTransactionDTO;
import com.neeve.ccfd.messages.TransformedPaymentTransactionDTO;

/**
 * Generates sample data set. Fields in the set take values from Gaussian distribution.
 */
public class TestDataGenerator {

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
    private final Random[] transformedFieldGenerators;
    private final Random amountGenerator;
    private final String fieldNames[];

    /**
     * Constructs generator instance. 
     *  
     * @param fieldsCount Number of fields used in analysis.
     */
    public TestDataGenerator(final int fieldsCount) {
        transformedFieldGenerators = new Random[fieldsCount];
        amountGenerator = new Random();
        for (int i = 0; i < transformedFieldGenerators.length; i++) {
            transformedFieldGenerators[i] = new Random();
        }
        fieldNames = new String[fieldsCount];
        for (int i = 0; i < fieldsCount; i++) {
            fieldNames[i] = String.format("value%03d", i);
        }
    }

    /**
     * Generates String id field from UUID.
     */
    public static final String generateId() {
        String retVal = UUID.randomUUID().toString();
        return retVal;
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
    public void generateRandomFields(final boolean isFraud, final int[] fraudIndicators, final TransformedPaymentTransactionDTO transaction) {

        // We could do this with reflection, but this will reduce garbage creation
        transaction.setValue000((float)transformedFieldGenerators[0].nextGaussian());
        transaction.setValue001((float)transformedFieldGenerators[1].nextGaussian());
        transaction.setValue002((float)transformedFieldGenerators[2].nextGaussian());
        transaction.setValue003((float)transformedFieldGenerators[3].nextGaussian());
        transaction.setValue004((float)transformedFieldGenerators[4].nextGaussian());
        transaction.setValue005((float)transformedFieldGenerators[5].nextGaussian());
        transaction.setValue006((float)transformedFieldGenerators[6].nextGaussian());
        transaction.setValue007((float)transformedFieldGenerators[7].nextGaussian());
        transaction.setValue008((float)transformedFieldGenerators[8].nextGaussian());
        transaction.setValue009((float)transformedFieldGenerators[9].nextGaussian());
        transaction.setValue010((float)transformedFieldGenerators[10].nextGaussian());
        transaction.setValue011((float)transformedFieldGenerators[11].nextGaussian());
        transaction.setValue012((float)transformedFieldGenerators[12].nextGaussian());
        transaction.setValue013((float)transformedFieldGenerators[13].nextGaussian());
        transaction.setValue014((float)transformedFieldGenerators[14].nextGaussian());
        transaction.setValue015((float)transformedFieldGenerators[15].nextGaussian());
        transaction.setValue016((float)transformedFieldGenerators[16].nextGaussian());
        transaction.setValue017((float)transformedFieldGenerators[17].nextGaussian());
        transaction.setValue018((float)transformedFieldGenerators[18].nextGaussian());
        transaction.setValue019((float)transformedFieldGenerators[19].nextGaussian());
        transaction.setValue020((float)transformedFieldGenerators[20].nextGaussian());
        transaction.setValue021((float)transformedFieldGenerators[21].nextGaussian());
        transaction.setValue022((float)transformedFieldGenerators[22].nextGaussian());
        transaction.setValue023((float)transformedFieldGenerators[23].nextGaussian());
        transaction.setValue024((float)transformedFieldGenerators[24].nextGaussian());
        transaction.setValue025((float)transformedFieldGenerators[25].nextGaussian());
        transaction.setValue026((float)transformedFieldGenerators[26].nextGaussian());
        transaction.setValue027((float)transformedFieldGenerators[27].nextGaussian());
        transaction.setValue028((float)transformedFieldGenerators[28].nextGaussian());
        transaction.setValue029((float)transformedFieldGenerators[29].nextGaussian());
        transaction.setValue030((float)transformedFieldGenerators[30].nextGaussian());
        transaction.setValue031((float)transformedFieldGenerators[31].nextGaussian());
        transaction.setValue032((float)transformedFieldGenerators[32].nextGaussian());
        transaction.setValue033((float)transformedFieldGenerators[33].nextGaussian());
        transaction.setValue034((float)transformedFieldGenerators[34].nextGaussian());
        transaction.setValue035((float)transformedFieldGenerators[35].nextGaussian());
        transaction.setValue036((float)transformedFieldGenerators[36].nextGaussian());
        transaction.setValue037((float)transformedFieldGenerators[37].nextGaussian());
        transaction.setValue038((float)transformedFieldGenerators[38].nextGaussian());
        transaction.setValue039((float)transformedFieldGenerators[39].nextGaussian());
        transaction.setValue040((float)transformedFieldGenerators[40].nextGaussian());
        transaction.setValue041((float)transformedFieldGenerators[41].nextGaussian());
        transaction.setValue042((float)transformedFieldGenerators[42].nextGaussian());
        transaction.setValue043((float)transformedFieldGenerators[43].nextGaussian());
        transaction.setValue044((float)transformedFieldGenerators[44].nextGaussian());
        transaction.setValue045((float)transformedFieldGenerators[45].nextGaussian());
        transaction.setValue046((float)transformedFieldGenerators[46].nextGaussian());
        transaction.setValue047((float)transformedFieldGenerators[47].nextGaussian());
        transaction.setValue048((float)transformedFieldGenerators[48].nextGaussian());
        transaction.setValue049((float)transformedFieldGenerators[49].nextGaussian());
        transaction.setValue050((float)transformedFieldGenerators[50].nextGaussian());
        transaction.setValue051((float)transformedFieldGenerators[51].nextGaussian());
        transaction.setValue052((float)transformedFieldGenerators[52].nextGaussian());
        transaction.setValue053((float)transformedFieldGenerators[53].nextGaussian());
        transaction.setValue054((float)transformedFieldGenerators[54].nextGaussian());
        transaction.setValue055((float)transformedFieldGenerators[55].nextGaussian());
        transaction.setValue056((float)transformedFieldGenerators[56].nextGaussian());
        transaction.setValue057((float)transformedFieldGenerators[57].nextGaussian());
        transaction.setValue058((float)transformedFieldGenerators[58].nextGaussian());
        transaction.setValue059((float)transformedFieldGenerators[59].nextGaussian());
        transaction.setValue060((float)transformedFieldGenerators[60].nextGaussian());
        transaction.setValue061((float)transformedFieldGenerators[61].nextGaussian());
        transaction.setValue062((float)transformedFieldGenerators[62].nextGaussian());
        transaction.setValue063((float)transformedFieldGenerators[63].nextGaussian());
        transaction.setValue064((float)transformedFieldGenerators[64].nextGaussian());
        transaction.setValue065((float)transformedFieldGenerators[65].nextGaussian());
        transaction.setValue066((float)transformedFieldGenerators[66].nextGaussian());
        transaction.setValue067((float)transformedFieldGenerators[67].nextGaussian());
        transaction.setValue068((float)transformedFieldGenerators[68].nextGaussian());
        transaction.setValue069((float)transformedFieldGenerators[69].nextGaussian());
        transaction.setValue070((float)transformedFieldGenerators[70].nextGaussian());
        transaction.setValue071((float)transformedFieldGenerators[71].nextGaussian());
        transaction.setValue072((float)transformedFieldGenerators[72].nextGaussian());
        transaction.setValue073((float)transformedFieldGenerators[73].nextGaussian());
        transaction.setValue074((float)transformedFieldGenerators[74].nextGaussian());
        transaction.setValue075((float)transformedFieldGenerators[75].nextGaussian());
        transaction.setValue076((float)transformedFieldGenerators[76].nextGaussian());
        transaction.setValue077((float)transformedFieldGenerators[77].nextGaussian());
        transaction.setValue078((float)transformedFieldGenerators[78].nextGaussian());
        transaction.setValue079((float)transformedFieldGenerators[79].nextGaussian());
        transaction.setValue080((float)transformedFieldGenerators[80].nextGaussian());
        transaction.setValue081((float)transformedFieldGenerators[81].nextGaussian());
        transaction.setValue082((float)transformedFieldGenerators[82].nextGaussian());
        transaction.setValue083((float)transformedFieldGenerators[83].nextGaussian());
        transaction.setValue084((float)transformedFieldGenerators[84].nextGaussian());
        transaction.setValue085((float)transformedFieldGenerators[85].nextGaussian());
        transaction.setValue086((float)transformedFieldGenerators[86].nextGaussian());
        transaction.setValue087((float)transformedFieldGenerators[87].nextGaussian());
        transaction.setValue088((float)transformedFieldGenerators[88].nextGaussian());
        transaction.setValue089((float)transformedFieldGenerators[89].nextGaussian());
        transaction.setValue090((float)transformedFieldGenerators[90].nextGaussian());
        transaction.setValue091((float)transformedFieldGenerators[91].nextGaussian());
        transaction.setValue092((float)transformedFieldGenerators[92].nextGaussian());
        transaction.setValue093((float)transformedFieldGenerators[93].nextGaussian());
        transaction.setValue094((float)transformedFieldGenerators[94].nextGaussian());
        transaction.setValue095((float)transformedFieldGenerators[95].nextGaussian());
        transaction.setValue096((float)transformedFieldGenerators[96].nextGaussian());
        transaction.setValue097((float)transformedFieldGenerators[97].nextGaussian());
        transaction.setValue098((float)transformedFieldGenerators[98].nextGaussian());
        transaction.setValue099((float)transformedFieldGenerators[99].nextGaussian());
        // int i = 0;
        //        for (String fieldName : fieldNames) {
        //            // TODO If we are implementing real detection algorithm with TensorFlow - set some of the fields to indicate fraud.
        //            // See comments for PaymentTransaction in com.neeve.ccfd.state.state.xml
        //            try {
        //                //TODO can we avoid string concatenation
        //                UtlReflection.setNonNestedProperty(transaction, fieldName, (float)transformedFieldGenerators[i].nextGaussian());
        //                i++;
        //            }
        //            catch (Exception ex) {
        //                throw new IllegalStateException("This should not happen");
        //            }
        //        }
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
        merchantMessage.setMerchantId(generateId());
        merchantMessage.setName(DEFAULT_MERCHANT_NAME);
        merchantMessage.setAddress(DEFAULT_STREET_ADDRESS);
        merchantMessage.setCountryCode(DEFAULT_COUNTRY_CODE);
        merchantMessage.setPostOrZip(DEFAULT_POSTAL_CODE);

        for (int i = 0; i < storesCount; i++) {
            NewMerchantStoreDTO merchantStore = NewMerchantStoreDTO.create();
            merchantStore.setStoreId(generateId());
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
    public NewCardHolderMessage generateCardHolderMessage(final int transactions, final int cards, final String merchantId, final String merchantStoreId) throws Exception {
        // For testing we can use UUID as card identifier. We just need unique string value
        if (transactions % cards > 0) {
            throw new IllegalArgumentException("Number of transactions must be divisible by number of cards");
        }

        NewCardHolderMessage newCardHolderMessage = NewCardHolderMessage.create();
        newCardHolderMessage.setRequestId(generateId());
        newCardHolderMessage.setCardHolderId(generateId());
        int transactionsPerCard = transactions / cards;
        for (int i = 0; i < cards; i++) {
            String cardNumber = generateId();
            newCardHolderMessage.addCardNumbers(cardNumber);
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
    public PaymentTransactionDTO generateTransactionMessage(final String cardNumber,
                                                            final String merchantId,
                                                            final String merchantStoreId) {
        if (cardNumber == null || cardNumber.isEmpty()) {
            throw new IllegalArgumentException("'cardNumber' cannot be null or empty string.");
        }
        if (merchantId == null || merchantId.isEmpty()) {
            throw new IllegalArgumentException("'merchantId' cannot be null or empty string.");
        }

        PaymentTransactionDTO newTransaction = PaymentTransactionDTO.create();
        newTransaction.setCardNumber(cardNumber);
        newTransaction.setMerchantId(merchantId);
        newTransaction.setMerchantStoreId(merchantStoreId);

        newTransaction.setAmount(10 + amountGenerator.nextGaussian() * 1000);

        return newTransaction;
    }

    /**
     * Generates transaction message populated with random field values to represent statistically transformed transaction 
     *  
     * @param transaction to 'transform' 
     *  
     * @return transformed transaction
     */
    public TransformedPaymentTransactionDTO generateTransformedTransactionMessage(final PaymentTransactionDTO transaction) {
        int fraudIndicators[] = {};
        TransformedPaymentTransactionDTO newTransaction = TransformedPaymentTransactionDTO.create();
        newTransaction.setTransactionIdFrom(transaction.getTransactionIdUnsafe());
        newTransaction.setCardNumberFrom(transaction.getCardNumberUnsafe());
        newTransaction.setAmount(transaction.getAmount());
        newTransaction.setMerchantIdFrom(transaction.getMerchantIdUnsafe());
        newTransaction.setMerchantStoreIdFrom(transaction.getMerchantStoreIdUnsafe());
        generateRandomFields(false, fraudIndicators, newTransaction);

        return newTransaction;
    }

    /**
     * Generates transaction message populated with random field values to represent statistically transformed transaction. 
     * It generates all the ID fields. 
     *  
     * @return transformed transaction
     */
    public TransformedPaymentTransactionDTO generateTransformedTransactionMessage() {
        int fraudIndicators[] = {};
        TransformedPaymentTransactionDTO newTransaction = TransformedPaymentTransactionDTO.create();
        newTransaction.setTransactionId(TestDataGenerator.generateId());
        newTransaction.setCardNumber(TestDataGenerator.generateId());
        newTransaction.setAmount(10 + amountGenerator.nextGaussian() * 1000);
        newTransaction.setMerchantId(TestDataGenerator.generateId());
        newTransaction.setMerchantStoreId(TestDataGenerator.generateId());
        generateRandomFields(false, fraudIndicators, newTransaction);

        return newTransaction;
    }
}
