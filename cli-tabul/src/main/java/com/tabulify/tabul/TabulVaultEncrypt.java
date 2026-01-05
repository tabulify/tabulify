package com.tabulify.tabul;

import com.tabulify.Tabular;
import com.tabulify.TabularAttributeEnum;
import com.tabulify.Vault;
import com.tabulify.cli.CliCommand;
import com.tabulify.cli.CliParser;
import com.tabulify.crypto.CryptoSymmetricCipher;
import com.tabulify.crypto.Protector;
import com.tabulify.memory.MemoryDataPath;
import com.tabulify.spi.DataPath;
import com.tabulify.stream.InsertStream;

import java.util.Collections;
import java.util.List;

import static com.tabulify.cli.CliUsage.CODE_BLOCK;
import static com.tabulify.cli.CliUsage.getFullChainOfCommand;

public class TabulVaultEncrypt {

    public static final String PLAINTEXT = "plaintext...";

    public static List<DataPath> run(Tabular tabular, CliCommand childCommand) {

        childCommand
                .setDescription("Encrypt sensitive information such as password")
                .addExample("To encrypt the text `alice`, you would execute",
                        CODE_BLOCK,
                        getFullChainOfCommand(childCommand) + " " + TabulWords.PASSPHRASE_PROPERTY + " difficultToGuessPassPhrase! \"alice\"",
                        CODE_BLOCK
                );

        childCommand.addArg(PLAINTEXT)
                .setDescription("One or more text to encrypt");

        childCommand.addProperty(TabulWords.PASSPHRASE_PROPERTY)
                .setMandatory(false);


        CliParser parse = childCommand.parse();
        String passphrase = parse.getString(TabulWords.PASSPHRASE_PROPERTY);
        if (passphrase == null) {
            String tabulPassphraseOsEnv = "TABUL_"+ TabularAttributeEnum.PASSPHRASE;
            passphrase = System.getenv(tabulPassphraseOsEnv);
            if (passphrase == null) {
                throw new IllegalArgumentException("Tabul passphrase property was not set and Os environment variable " + tabulPassphraseOsEnv + " does not exist.");
            }
        }

        MemoryDataPath feedback = (MemoryDataPath) tabular.getMemoryConnection().getDataPath("Encryption")
                .setComment("The encryption result")
                .createRelationDef()
                .addColumn("plaintext")
                .addColumn("ciphertext")
                .getDataPath();

        List<String> plaintexts = parse.getStrings(PLAINTEXT);
        Protector protector = Protector.create(passphrase);
        try (
                InsertStream insertStream = feedback.getInsertStream()
        ) {
            for (String plaintext : plaintexts) {
                String cipherText = protector.encrypt(CryptoSymmetricCipher.AES_CBC_PKCS5PADDING, plaintext);
                insertStream.insert(plaintext, Vault.VAULT_PREFIX + cipherText);
            }
        }

        return Collections.singletonList(feedback);
    }
}
