with users as (select user_id, user_email, user_realm_id
               from cs_realms.realm_user
               where user_realm_id = $1 -- 1 -- realm id
                 and user_email like $2 -- '%gmail%' email
),
     registrations as (select users.*, registration.registration_creation_time, registration.registration_status
                       from users
                              join cs_realms.realm_list_registration registration
                                   on users.user_realm_id = registration.registration_realm_id
                                     and users.user_id = registration.registration_user_id
                                     and registration.registration_list_id = $3 -- 1 -- list id
     ),
     rowNumbered as (select ROW_NUMBER() OVER (ORDER BY registration_creation_time DESC) AS rn,
                            *
                     from registrations),
     final as (select *
               from rowNumbered
               where rn >= 1 + $4::BIGINT * $5::BIGINT -- 1
                 and rn < $6::BIGINT * $7::BIGINT + 1 --10
     )
select *
from final
order by registration_creation_time desc
