insert into application_user (credit_amount, pay_as_you_go, created_date, created_by, version, id, user_name, name,
                              hashed_password)
values (0, false, now(), 'init', 1, '0', 'cash', 'Cash User',
        '$2a$10$xdbKoM48VySZqVSU/cSlVeJn0Z04XCZ7KZBjUBC00eKo5uLswyOpe');

insert into application_user (credit_amount, pay_as_you_go, created_date, created_by, version, id, user_name, name,
                              hashed_password)
values (0, true, now(), 'init', 1, '1', 'user', 'Test User',
        '$2a$10$xdbKoM48VySZqVSU/cSlVeJn0Z04XCZ7KZBjUBC00eKo5uLswyOpe');
insert into user_roles (user_id, roles)
values ('1', 'USER');
insert into application_user (credit_amount, pay_as_you_go, created_date, created_by, version, id, user_name, name,
                              hashed_password)
values (0, false, now(), 'init', 1, '2', 'admin', 'Admin User',
        '$2a$10$jpLNVNeA7Ar/ZQ2DKbKCm.MuT2ESe.Qop96jipKMq7RaUgCoQedV.');
insert into user_roles (user_id, roles)
values ('2', 'ADMIN');

insert into application_user (credit_amount, pay_as_you_go, created_date, created_by, version, id, user_name, name,
                              hashed_password)
values (200, false, now(), 'init', 1, '4', 'bobs_pay_as_u_go', 'Bobs Pay_As_You_Go',
        '$2a$10$xdbKoM48VySZqVSU/cSlVeJn0Z04XCZ7KZBjUBC00eKo5uLswyOpe');
insert into application_user (credit_amount, pay_as_you_go, created_date, created_by, version, id, user_name, name,
                              hashed_password)
values (0, false, now(), 'init', 1, '7', 'bobs_banker', 'Bobs Banker',
        '$2a$10$xdbKoM48VySZqVSU/cSlVeJn0Z04XCZ7KZBjUBC00eKo5uLswyOpe');


insert into pool (id, bank_user_id, status, week, created_by, created_date, version, amount, league, name, season,
                  pay_as_you_go_user_id, include_thursday, max_players_per_week)
values (5, 7, 'CLOSED', 'WEEK_1', 'init', now(), 1, 5, 'NFL', 'Bobs', 'S_2024', 4, false, 25);
insert into application_user (credit_amount, pay_as_you_go, phone, email, created_date, created_by, version, id,
                              user_name, name, hashed_password)
values (0, false, '5056603706', 'greenemcg@gmail.com', now(), 'init', 1, '6', 'greenemcg', 'Michael Greene',
        '$2a$10$26ql/311km2/j1mTENnmq.kVQg7ZsIe6nnamoDVw4FzwZSPuFVIoi');
insert into user_roles (user_id, roles)
values ('6', 'USER');
insert into user_roles (user_id, roles)
values ('6', 'ADMIN');
update application_user
set profile_picture = pg_read_binary_file('/tmp/greenemcg.png')
where id = 6;
update application_user
set profile_picture = pg_read_binary_file('/tmp/admin.jpg')
where id = 2;
update application_user
set profile_picture = pg_read_binary_file('/tmp/payor.png')
where id = 4;
update application_user
set profile_picture = pg_read_binary_file('/tmp/test-user.jpg')
where id = 1;
update pool
set profile_picture = pg_read_binary_file('/tmp/busch.jpeg')
where id = 5;
update application_user
set profile_picture = pg_read_binary_file('/tmp/bank.jpeg')
where id = 7;

update application_user
set profile_picture = pg_read_binary_file('/tmp/cash.jpeg')
where id = 0;





