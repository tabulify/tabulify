package net.bytle.email;

public class EmailScore {


    private final Email email;

    public EmailScore(Email email) {
        this.email = email;
    }

    public static EmailScore of(Email email) {
        return new EmailScore(email);
    }
}
