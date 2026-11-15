-- Lab 10, individual variant 7: persistent remember-me tokens.
-- The table layout is the one JdbcTokenRepositoryImpl expects. A new V6, never an edit to V5.
create table persistent_logins (
    username  varchar(64) not null,
    series    varchar(64) primary key,     -- stays the same for one remembered browser
    token     varchar(64) not null,        -- changes on every automatic login
    last_used timestamp   not null
);
