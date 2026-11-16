package kz.iitu.springlab.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Lab 10, step 2.2: run this main (IntelliJ: the green arrow) to produce hashes for V5__security.sql.
 * Run it twice - the hash of the same password is different every time, because of the random salt.
 */
public class HashGenerator {

    public static void main(String[] args) {
        var encoder = new BCryptPasswordEncoder();
        System.out.println("user123  -> " + encoder.encode("user123"));
        System.out.println("user123  -> " + encoder.encode("user123") + "   (same password, new salt)");
        System.out.println("admin123 -> " + encoder.encode("admin123"));
    }
}
