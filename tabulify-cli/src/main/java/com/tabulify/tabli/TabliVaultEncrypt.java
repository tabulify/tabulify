package com.tabulify.tabli;

import net.bytle.cli.CliCommand;
import net.bytle.crypto.CryptoSymmetricCipher;
import net.bytle.crypto.Protector;
import com.tabulify.Tabular;
import com.tabulify.Vault;
import com.tabulify.memory.MemoryDataPath;
import com.tabulify.spi.DataPath;
import com.tabulify.stream.InsertStream;

import java.util.Collections;
import java.util.List;

import static net.bytle.cli.CliUsage.CODE_BLOCK;
import static net.bytle.cli.CliUsage.getFullChainOfCommand;

public class TabliVaultEncrypt {

  public static final String PLAINTEXT = "plaintext...";

  public static List<DataPath> run(Tabular tabular, CliCommand childCommand) {

    childCommand
      .setDescription("Encrypt sensitive information such as password")
      .addExample("To encrypt the text `alice`, you would execute",
        CODE_BLOCK,
        getFullChainOfCommand(childCommand)+" "+ TabliWords.PASSPHRASE_PROPERTY +" difficultToGuessPassPhrase! \"alice\"",
        CODE_BLOCK
      );

    childCommand.addArg(PLAINTEXT)
      .setDescription("One or more text to encrypt");

    childCommand.addProperty(TabliWords.PASSPHRASE_PROPERTY)
      .setMandatory(true);


    String passphrase = childCommand.parse().getString(TabliWords.PASSPHRASE_PROPERTY);

    MemoryDataPath feedback = (MemoryDataPath) tabular.getMemoryDataStore().getDataPath("Encryption")
      .setDescription("The encryption result")
      .createRelationDef()
      .addColumn("plaintext")
      .addColumn("ciphertext")
      .getDataPath();

    List<String> plaintexts = childCommand.parse().getStrings(PLAINTEXT);
    Protector protector = Protector.create(passphrase);
    try (
      InsertStream insertStream = feedback.getInsertStream()
    ) {
      for (String plaintext : plaintexts) {
        String cipherText = protector.encrypt(CryptoSymmetricCipher.AES_CBC_PKCS5PADDING, plaintext);
        insertStream.insert(plaintext, Vault.VAULT_PREFIX+cipherText);
      }
    }

    return Collections.singletonList(feedback);
  }
}
