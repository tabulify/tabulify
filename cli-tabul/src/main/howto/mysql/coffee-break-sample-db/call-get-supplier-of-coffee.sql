{ call GET_SUPPLIER_OF_COFFEE(?,?) }
-- To use an OUT parameter (ie the first ? parameter), the expression should be escaped (ie enclosed by { and })
-- Otherwise you get the error: This statement does not declare an OUT parameter.  Use { ?= call ... } to declare one
