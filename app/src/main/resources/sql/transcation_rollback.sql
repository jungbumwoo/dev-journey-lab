-- setup
drop table member if exists cascade;

create table member (
    member_id varchar(10),
    money integer not null default 0,
primary key (member_id)
);

insert into member(member_id, money) values ('memberA',10000);
insert into member(member_id, money) values ('memberB',10000);

-- session 1
set autocommit false;
update member set money=10000 - 2000 where member_id = 'memberA'; -- 성공
update member set money=10000 + 2000 where member_iddd = 'memberB'; -- 쿼리 예외
발생

select * from member; -- 8,000, 10,000

-- session 2
select * from member; -- 10,000 10,000

-- session 1
rollback; -- commit 하면 그냥 반영되어버렴; network 문제 상 rollback 을 db에서 받지 못하면? commit이 안되니까 반영 안됨.
select * from member; -- 10,000 10,000

