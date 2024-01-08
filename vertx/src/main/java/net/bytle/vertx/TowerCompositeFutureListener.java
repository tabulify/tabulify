package net.bytle.vertx;

public interface TowerCompositeFutureListener {

  void setCountSuccess(Integer countSuccess);
  void setCountTotal(Integer countTotal);
  void setCountComplete(Integer countComplete);

  void setCountFailure(Integer countFailed);

}
