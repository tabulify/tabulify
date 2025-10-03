package com.tabulify.tabul;

import com.tabulify.Tabular;
import com.tabulify.Vault;
import com.tabulify.memory.MemoryDataPath;
import com.tabulify.spi.DataPath;
import com.tabulify.stream.InsertStream;
import net.bytle.cli.CliCommand;
import net.bytle.crypto.Protector;

import java.util.Collections;
import java.util.List;

import static net.bytle.cli.CliUsage.CODE_BLOCK;
import static net.bytle.cli.CliUsage.getFullChainOfCommand;

public class TabulVaultDecrypt {

  private static final String CIPHER_TEXT = "ciphertext...";

  public static List<DataPath> run(Tabular tabular, CliCommand childCommand) {

    childCommand
      .setDescription("Decrypt ciphertext into plaintext")
      .addExample("To decrypt, you would execute",
        CODE_BLOCK,
        getFullChainOfCommand(childCommand) + " " + TabulWords.PASSPHRASE_PROPERTY + " difficultToGuessPassPhrase! \"vaultQVE9PT5KeU1OK1RXNWNSVDJCcVRq\"",
        CODE_BLOCK
      );

    childCommand.addArg(CIPHER_TEXT)
      .setDescription("One or more ciphertext to decrypt");

    childCommand.addProperty(TabulWords.PASSPHRASE_PROPERTY)
      .setMandatory(true);


    String passphrase = childCommand.parse().getString(TabulWords.PASSPHRASE_PROPERTY);

    MemoryDataPath feedback = (MemoryDataPath) tabular.getMemoryConnection().getDataPath("Decryption")
      .setComment("The cipher decryption:")
      .createRelationDef()
      .addColumn("plaintext")// Plain text first otherwise we don't see it in the doc
      .addColumn("ciphertext")
      .getDataPath();

    List<String> cipherTexts = childCommand.parse().getStrings(CIPHER_TEXT);
    Protector protector = Protector.create(passphrase);
    try (
      InsertStream insertStream = feedback.getInsertStream()
    ) {
      for (String ciphertext : cipherTexts) {
        String ciphertextToDecrypt = ciphertext;
        if (ciphertext.startsWith(Vault.VAULT_PREFIX)){
          ciphertextToDecrypt = ciphertext.substring(Vault.VAULT_PREFIX.length());
        }
        String plaintext = protector.decrypt(ciphertextToDecrypt);
        insertStream.insert(plaintext,ciphertext);
      }
    }

    return Collections.singletonList(feedback);
  }
}
