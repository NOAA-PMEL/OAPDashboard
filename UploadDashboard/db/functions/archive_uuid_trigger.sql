DELIMITER ;;
CREATE TRIGGER before_insert_submissions
BEFORE INSERT ON archive_submissions
FOR EACH ROW
BEGIN
  IF new.submission_key IS NULL THEN
    SET new.submission_key = uuid();
  END IF;
END
;;
