package net.bytle.email;

import net.bytle.type.Strings;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Email {

    private static final String AT_SIGN = "@";
    private final String email;
    private final String domain;
    private final String local_part;

    private Email(String email) {
        this.email = email;
        final String[] split = email.split(AT_SIGN);
        if (split.length!=2){
            throw new RuntimeException("("+email+") is not a valid email");
        }
        this.domain = split[1];
        this.local_part = split[0];
    }

    public String getDomain() {
        return domain;
    }

    public String getLocalPart() {
        return local_part;
    }

    public static Email of(String email) {
        return new Email(email);
    }

    public List<String> getDomainParts() {

        return Arrays.asList(domain.split("\\."));

    }

    public String getRootDomain() {

        List<String> rootDomainParts = IntStream
                .range(0, 2)
                .mapToObj(i -> getDomainParts().get(getDomainParts().size() - 1 - i))
                .sorted(Collections.reverseOrder())
                .collect(Collectors.toList());

        return String.join(".", rootDomainParts);

    }

    public Integer getDomainDots() {
        return getDomainParts().size()-1;
    }

    public Integer getLocalPartDigitCount() {
        return Strings.getDigitCount(local_part);
    }

    public String getEmail() {
        return email;
    }
}
