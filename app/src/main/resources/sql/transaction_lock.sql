-- setup
drop table member if exists cascade;

create table member (
    member_id varchar(10),
    money integer not null default 0,
primary key (member_id)
);

insert into member(member_id, money) values ('memberA',10000);

-- session 1
set autocommit false;
update member set money=500 where member_id = 'memberA';
-- select 시에도 락을 잡을 수 있다. ex) select * from member where member_id='memberA' for update;

-- session 2 : session 1 이 락 점유한 동안 update 불가
SET LOCK_TIMEOUT 60000;
set autocommit false;
update member set money=1000 where member_id = 'memberA';

-- session 1
commit;


