package com.tabulify.email.flow;

import com.tabulify.Tabular;
import com.tabulify.Vault;
import com.tabulify.conf.Attribute;
import com.tabulify.conf.Origin;
import com.tabulify.connection.Connection;
import com.tabulify.connection.ConnectionAttributeEnum;
import com.tabulify.exception.NotFoundException;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.DataSystem;
import com.tabulify.spi.ProcessingEngine;
import com.tabulify.type.*;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import com.tabulify.exception.CastException;
import com.tabulify.exception.InternalException;
import com.tabulify.exception.NoValueException;
import com.tabulify.os.Oss;
import org.simplejavamail.api.mailer.Mailer;
import org.simplejavamail.api.mailer.config.TransportStrategy;
import org.simplejavamail.mailer.MailerBuilder;
import org.simplejavamail.mailer.internal.MailerRegularBuilderImpl;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class SmtpConnection extends Connection {


    private Mailer smtpClient;


    public SmtpConnection(Tabular tabular, Attribute name, Attribute uri) {

        super(tabular, name, uri);

        this.addAttributesFromEnumAttributeClass(SmtpConnectionAttributeEnum.class);

        // Default from
        InternetAddress from;
        try {
            try {
                from = new InternetAddress(Oss.getUser() + "@" + Oss.getFqdn().toStringWithoutRoot());
            } catch (UnknownHostException e) {
                from = new InternetAddress("tabulify@localhost");
            }
        } catch (AddressException ex) {
            throw new InternalException("An unforeseen internal error has occurred while setting the default from internet address for the connection " + name + ". Error: " + ex.getMessage(), ex);
        }
        this.setDefaultFrom(from);


    }

    private SmtpConnection setDefaultFrom(InternetAddress from) {
        Attribute attribute = this.getTabular().getVault()
                .createVariableBuilderFromAttribute(SmtpConnectionAttributeEnum.FROM)
                .setOrigin(Origin.DEFAULT)
                .buildSafe(from)
                // raw value is important for the creation of the tabul vault
                .setRawValue(from.toString());
        this.addAttribute(attribute);
        return this;
    }

    @Override
    public Connection addAttribute(KeyNormalizer name, Object value, Origin origin) {

        SmtpConnectionAttributeEnum smtpConnectionAttribute;
        try {
            smtpConnectionAttribute = Casts.cast(name, SmtpConnectionAttributeEnum.class);
        } catch (Exception e) {
            return super.addAttribute(name, value, origin);
        }


        Vault.VariableBuilder builder = this.getTabular().getVault()
                .createVariableBuilderFromAttribute(smtpConnectionAttribute)
                .setOrigin(Origin.MANIFEST);


        /**
         * The decoded value to transform in Internet Address
         */
        Function<Object, ?> transform = null;
        switch (smtpConnectionAttribute) {

            case FROM:

                transform = o -> {
                    InternetAddress[] internetAddresses;
                    try {
                        internetAddresses = InternetAddress.parse(o.toString());
                    } catch (AddressException e) {
                        throw new IllegalArgumentException("The from value (" + value + ") is not a valid internet address. Error:" + e.getMessage(), e);
                    }
                    if (internetAddresses.length == 0) {
                        throw new IllegalArgumentException("The from value (" + value + ") has no internet addresses");
                    }
                    if (internetAddresses.length > 1) {
                        throw new IllegalArgumentException("The from value (" + value + ") has too much internet addresses (only one)");
                    }
                    return internetAddresses[0];
                };
                break;

            case CC:
            case TO:
            case BCC:

                transform = o -> {
                    InternetAddress[] internetAddresses;
                    try {
                        internetAddresses = InternetAddress.parse(o.toString());
                    } catch (AddressException e) {
                        throw new IllegalArgumentException("The " + smtpConnectionAttribute + " value (" + value + ") is not a valid internet address. Error:" + e.getMessage(), e);
                    }
                    if (internetAddresses.length == 0) {
                        throw new IllegalArgumentException("The " + smtpConnectionAttribute + " value (" + value + ") has no internet addresses");
                    }
                    return Arrays.asList(internetAddresses);
                };
                break;

        }

        Attribute attribute;
        try {
            attribute = builder.build(value, transform);
        } catch (CastException e) {
            throw new IllegalArgumentException("Error while creating the variable (" + smtpConnectionAttribute + ") with the value (" + value + ") for the connection (" + this + "). Error:" + e.getMessage(), e);
        }
        this.addAttribute(attribute);


        return this;

    }

    private Boolean getTls() {

        return (Boolean) getAttribute(SmtpConnectionAttributeEnum.TLS).getValueOrDefault();

    }

    private Boolean getDebug() {

        return (Boolean) getAttribute(SmtpConnectionAttributeEnum.DEBUG).getValueOrDefault();

    }


    private String getSmtpPassword() throws NoValueException {

        return this.getPassword();

    }

    private String getSmtpUser() throws NoValueException {

        return (String) this.getUser().getValueOrDefault();


    }

    @Override
    public DataSystem getDataSystem() {
        throw new RuntimeException("The smtp does not have any data system implemented");
    }

    @Override
    public DataPath getDataPath(String pathOrName, MediaType mediaType) {
        throw new RuntimeException("The smtp does not have any data system implemented");
    }

    @Override
    public DataPath getDataPath(String pathOrName) {
        throw new RuntimeException("The smtp does not have any data system implemented");
    }

    @Override
    public String getCurrentPathCharacters() {
        throw new RuntimeException("The smtp does not have any data system implemented");
    }

    @Override
    public String getParentPathCharacters() {
        throw new RuntimeException("The smtp does not have any data system implemented");
    }

    @Override
    public String getSeparator() {
        throw new RuntimeException("The smtp does not have any data system implemented");
    }

    @Override
    public DataPath getCurrentDataPath() {
        return null;
    }


    @Override
    public DataPath getRuntimeDataPath(DataPath dataPath, MediaType mediaType) {
        throw new RuntimeException("Smtp does not support scripting");
    }

    @Override
    public ProcessingEngine getProcessingEngine() {
        throw new RuntimeException("The smtp does not have any data system implemented");
    }

    @Override
    public Boolean ping() {

        Mailer mailer = this.getSmtpClient();
        /**
         * We don't use {@link Mailer#testConnection()} because it just blocks
         * if the service is not on
         * https://github.com/bbottema/simple-java-mail/issues/116
         */
        // disable the warning
        Logger.getLogger("org.simplejavamail.mailer.internal.MailerImpl").setLevel(Level.SEVERE);
        Session session = mailer.getSession();
        try (Transport transport = session.getTransport()) {
            transport.connect();
            return true;
        } catch (Exception e) {
            return false;
        }

    }

    public Mailer getSmtpClient() {

        if (smtpClient != null) {
            return smtpClient;
        }

        /*
         * We build it late because the username and the
         * password may be passed later at build time.
         * Bad yeah.
         */
        /*
         * Host may be set by env or uri
         */
        DnsName host;
        UriEnhanced uri = this.getUri();
        try {
            host = uri.getHost();
        } catch (NotFoundException e) {
            throw new RuntimeException("A host is mandatory for a smtp connection and was not found in the uri: " + uri, e);
        }
        MailerRegularBuilderImpl mailerBuilder = MailerBuilder.withSMTPServerHost(host.toString());

        /*
         * Port may be set by env or uri
         */
        Integer port = uri.getPort();
        if (port != null) {
            mailerBuilder.withSMTPServerPort(port);
        }

        try {
            String userName = this.getSmtpUser();
            mailerBuilder.withSMTPServerUsername(userName);
        } catch (NoValueException e) {
            // ok
        }

        try {
            String userPassword = this.getSmtpPassword();
            mailerBuilder.withSMTPServerPassword(userPassword);
        } catch (NoValueException e) {
            // ok
        }

        Boolean tls = this.getTls();
        if (tls) {
            mailerBuilder.withTransportStrategy(TransportStrategy.SMTP_TLS);
        }

        Boolean debug = this.getDebug();
        if (!debug) {
            // To get rid of this log each time an email is sent
            // jakarta.mail.event.TransportEvent[source=smtp://admin@localhost]
            Logger.getLogger("jakarta.mail").setLevel(Level.OFF);
        } else {
            mailerBuilder.withDebugLogging(true);
        }

        this.smtpClient = mailerBuilder.buildMailer();
        return smtpClient;

    }


    /**
     * To inject for test
     *
     * @param smtpServer - inject the smtp server
     * @return the object for chaining
     */
    public SmtpConnection setSmtpClient(Mailer smtpServer) {
        this.smtpClient = smtpServer;
        return this;
    }


    public List<InternetAddress> getDefaultToInternetAddresses() {
        //noinspection unchecked
        return (List<InternetAddress>) this.getAttribute(SmtpConnectionAttributeEnum.TO).getValueOrDefault();
    }

    public List<InternetAddress> getDefaultCcInternetAddresses() {
        //noinspection unchecked
        return (List<InternetAddress>) this.getAttribute(SmtpConnectionAttributeEnum.CC).getValueOrDefault();
    }

    public List<InternetAddress> getDefaultBccInternetAddresses() {

        //noinspection unchecked
        return (List<InternetAddress>) this.getAttribute(SmtpConnectionAttributeEnum.BCC).getValueOrDefault();
    }

    public SmtpConnection setDefaultTo(InternetAddress... addressLists) {

        List<InternetAddress> list = Arrays.asList(addressLists);
        Attribute attribute = this.getTabular().getVault()
                .createVariableBuilderFromAttribute(SmtpConnectionAttributeEnum.TO)
                .setOrigin(Origin.DEFAULT)
                .buildSafe(list)
                // raw value is important for the creation of the tabul vault as yaml does not understand InternetAddress
                .setRawValue(list.stream().map(InternetAddress::toString).collect(Collectors.joining(", ")));
        this.addAttribute(attribute);

        return this;

    }

    public SmtpConnection setDefaultTo(String addressLists) {

        try {
            this.setDefaultTo(InternetAddress.parse(addressLists));
        } catch (AddressException e) {
            throw new IllegalArgumentException("The value (" + addressLists + ") is not a valid address format. Error: " + e.getMessage(), e);
        }
        return this;

    }

    @Override
    public List<Class<? extends ConnectionAttributeEnum>> getAttributeEnums() {
        List<Class<? extends ConnectionAttributeEnum>> attributeEnums = new ArrayList<>(super.getAttributeEnums());
        attributeEnums.add(SmtpConnectionAttributeEnum.class);
        return attributeEnums;
    }


    public InternetAddress getDefaultFrom() {
        return (InternetAddress) this.getAttribute(SmtpConnectionAttributeEnum.FROM).getValueOrDefault();
    }

    public Connection setDefaultFrom(String fromValue) {
        try {
            InternetAddress[] parse = InternetAddress.parse(fromValue);
            if (parse.length != 1) {
                throw new IllegalArgumentException("The from value (" + fromValue + ") should be only one address.");
            }
            this.setDefaultFrom(parse[0]);
        } catch (AddressException e) {
            throw new IllegalArgumentException("The from value (" + fromValue + ") is not a valid address format. Error: " + e.getMessage(), e);
        }
        return this;
    }
}
