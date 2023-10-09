package net.bytle.tower.util;

import net.bytle.crypto.PasswordHashing;
import net.bytle.exception.NoSecretException;
import net.bytle.vertx.ConfigAccessor;

public class PasswordHashManager {

  public static final String PASSWORD_HASH_SALT = "password.hash.salt";
  private static PasswordHashManager passwordHashManager;
  private final PasswordHashing passwordHash;

  public PasswordHashManager(String salt) {
    this.passwordHash = PasswordHashing.createFromSalt(salt);
  }


  public static void init(ConfigAccessor jsonConfig) throws NoSecretException {

    String salt = jsonConfig.getString(PASSWORD_HASH_SALT);
    if (salt == null) {
      throw new NoSecretException("PasswordHashManager: A salt is mandatory to hash the password. Add one in the conf file with the attribute (" + PASSWORD_HASH_SALT + ")");
    }

    passwordHashManager = new PasswordHashManager(salt);

  }

  public static PasswordHashManager get(){
    return passwordHashManager;
  }

  public String hash(String plainText) {
    return this.passwordHash.toHash(plainText);
  }

}
