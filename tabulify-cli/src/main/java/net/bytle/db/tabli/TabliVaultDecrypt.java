package net.bytle.db.tabli;

import net.bytle.cli.CliCommand;
import net.bytle.crypto.Protector;
import net.bytle.db.Tabular;
import net.bytle.db.Vault;
import net.bytle.db.memory.MemoryDataPath;
import net.bytle.db.spi.DataPath;
import net.bytle.db.stream.InsertStream;

import java.util.Collections;
import java.util.List;

import static net.bytle.cli.CliUsage.CODE_BLOCK;
import static net.bytle.cli.CliUsage.getFullChainOfCommand;

public class TabliVaultDecrypt {

  private static final String CIPHER_TEXT = "ciphertext...";

  public static List<DataPath> run(Tabular tabular, CliCommand childCommand) {

    childCommand
      .setDescription("Decrypt ciphertext into plaintext")
      .addExample("To decrypt, you would execute",
        CODE_BLOCK,
        getFullChainOfCommand(childCommand)+" "+ TabliWords.PASSPHRASE_PROPERTY +" difficultToGuessPassPhrase! \"vaultQVE9PT5KeU1OK1RXNWNSVDJCcVRq\"",
        CODE_BLOCK
      );

    childCommand.addArg(CIPHER_TEXT)
      .setDescription("One or more ciphertext to decrypt");

    childCommand.addProperty(TabliWords.PASSPHRASE_PROPERTY)
      .setMandatory(true);


    String passphrase = childCommand.parse().getString(TabliWords.PASSPHRASE_PROPERTY);

    MemoryDataPath feedback = (MemoryDataPath) tabular.getMemoryDataStore().getDataPath("Decryption")
      .setDescription("The cipher decryption:")
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
