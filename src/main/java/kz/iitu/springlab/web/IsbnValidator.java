package kz.iitu.springlab.web;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** ISBN-10: sum of digit*(10..1) divisible by 11, last digit may be X. ISBN-13: weights 1,3,1,3... divisible by 10. */
public class IsbnValidator implements ConstraintValidator<Isbn, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;                                   // optional field: blank passes
        }
        String s = value.replace("-", "").replace(" ", "").toUpperCase();
        return switch (s.length()) {
            case 10 -> isValid10(s);
            case 13 -> isValid13(s);
            default -> false;                              // wrong length
        };
    }

    static boolean isValid10(String s) {
        int sum = 0;
        for (int i = 0; i < 10; i++) {
            char c = s.charAt(i);
            int digit;
            if (c == 'X' && i == 9) digit = 10;
            else if (Character.isDigit(c)) digit = c - '0';
            else return false;
            sum += digit * (10 - i);
        }
        return sum % 11 == 0;
    }

    static boolean isValid13(String s) {
        int sum = 0;
        for (int i = 0; i < 13; i++) {
            char c = s.charAt(i);
            if (!Character.isDigit(c)) return false;
            sum += (c - '0') * (i % 2 == 0 ? 1 : 3);
        }
        return sum % 10 == 0;
    }
}
