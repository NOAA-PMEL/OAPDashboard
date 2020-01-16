CREATE DEFINER = `root`@`localhost` FUNCTION `oapdashboard`.`UuidToBin`(_uuid BINARY(36))
RETURNS binary(16)
LANGUAGE SQL
DETERMINISTIC
CONTAINS SQL
SQL SECURITY INVOKER
COMMENT ''
RETURN
        UNHEX(CONCAT(
            SUBSTR(_uuid, 15, 4),
            SUBSTR(_uuid, 10, 4),
            SUBSTR(_uuid,  1, 8),
            SUBSTR(_uuid, 20, 4),
            SUBSTR(_uuid, 25) ))
