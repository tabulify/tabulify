package net.bytle.tower.eraldy.objectProvider;

import io.vertx.core.Future;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.model.manual.FileObject;
import net.bytle.tower.eraldy.module.realm.model.Realm;

public class FileProvider {


  private final EraldyApiApp apiApp;

  public FileProvider(EraldyApiApp eraldyApiApp) {
    this.apiApp = eraldyApiApp;
  }


  public Future<FileObject> getFile(Long fileId, Realm realm) {
      return null;
  }

}
