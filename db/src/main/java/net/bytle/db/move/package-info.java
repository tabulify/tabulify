/**
 * This package should stay in the core as the main entry is in the {@link net.bytle.db.spi.Tabulars} class
 *
 * ie:
 *   * {@link net.bytle.db.spi.Tabulars#move(net.bytle.db.spi.DataPath, net.bytle.db.spi.DataPath)}
 *   * {@link net.bytle.db.spi.Tabulars#move(net.bytle.db.spi.DataPath, net.bytle.db.spi.DataPath, net.bytle.db.move.MoveProperties)}
 *   * ...
 *
 * The package is not tested in this module but in the move module as the tests depends also on the data store module.
 */
package net.bytle.db.move;