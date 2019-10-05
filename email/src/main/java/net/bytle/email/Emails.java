package net.bytle.email;

public class Emails {


    public EmailScore validation(Email email) {
        return EmailScore.of(email);
    }
}
