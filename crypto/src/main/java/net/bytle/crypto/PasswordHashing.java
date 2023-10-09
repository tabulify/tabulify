package net.bytle.crypto;

import net.bytle.type.Base64Utility;
import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;

import java.nio.charset.StandardCharsets;

/**
 * Password hashing with Argon as
 * <a href="https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html#password-hashing-algorithms">...</a>
 * <p>
 * based on <a href="https://github.com/bcgit/bc-java/blob/main/core/src/test/java/org/bouncycastle/crypto/test/Argon2Test.java">...</a>
 */
public class PasswordHashing {


  private static final int HASH_LENGTH = 32;
  private final Argon2BytesGenerator generate;

  public PasswordHashing(String salt) {
    int iterations = 2;
    int memLimit = 66536;

    int parallelism = 1;

    Argon2Parameters.Builder builder = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
      .withVersion(Argon2Parameters.ARGON2_VERSION_13)
      .withIterations(iterations)
      .withMemoryAsKB(memLimit)
      .withParallelism(parallelism)
      .withSalt(salt.getBytes());

    this.generate = new Argon2BytesGenerator();
    generate.init(builder.build());

  }

  public static PasswordHashing createFromSalt(String salt) {
    return new PasswordHashing(salt);
  }

  public String toHash(String plainPassword) {


    byte[] result = new byte[HASH_LENGTH];
    generate.generateBytes(plainPassword.getBytes(StandardCharsets.UTF_8), result, 0, result.length);
    return Base64Utility.bytesToBase64String(result);

  }

}
