/**
 * This package should stay in the core as the main entry is in the {@link com.tabulify.spi.Tabulars} class
 *
 * ie:
 *   * {@link com.tabulify.spi.Tabulars#move(DataPathAbs, DataPathAbs)}
 *   * {@link com.tabulify.spi.Tabulars#move(DataPathAbs, DataPathAbs, TransferProperties)}
 *   * ...
 *
 * The package is not tested in this module but in the transfer module as the tests depends also on the data store module.
 *
 * See the documentation in the transfer module
 */
package com.tabulify.transfer;

import com.tabulify.spi.DataPathAbs;
