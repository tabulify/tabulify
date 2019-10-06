#!/usr/bin/env bash

xmlExtract.sh -xpath "//Repository/DECLARE/ConnectionPool[@name='Writeback_Column_Names' and contains(@parentName, 'WRITEBACK_COLUMN_NAMES')]" phase_out_merge.xml

xmlExtract.sh -xpath "//Repository/DECLARE/ConnectionPool[@name='Writeback_Column_Names' and @parentName='\"WRITEBACK_COLUMN_NAMES\"']" phase_out_merge.xml