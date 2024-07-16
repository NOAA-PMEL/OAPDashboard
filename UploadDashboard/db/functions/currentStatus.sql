mysql> select s.db_id, s.submission_id, s.status, s.status_time from submission_status s
    -> join (
    ->   select submission_id, max(db_id) as max_id
    ->   from submission_status 
    ->   group by submission_id
    -> ) as max_ids
    -> on s.db_id = max_ids.max_id;

