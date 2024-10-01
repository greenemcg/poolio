package nm.poolio.utils;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class PhoneNumberUtils {
    private static final PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();

    private PhoneNumberUtils() {
    }

    public static String formatPhoneNumber(String phoneNumberJustNumbers) {
        try {
            Phonenumber.PhoneNumber phoneNumber = phoneUtil.parse(phoneNumberJustNumbers, "US");
            return phoneUtil.format(phoneNumber, PhoneNumberUtil.PhoneNumberFormat.NATIONAL);
        } catch (NumberParseException e) {
            log.warn("Cannot convert phone number: {}", phoneNumberJustNumbers, e);
            return "";
        }
    }
}
