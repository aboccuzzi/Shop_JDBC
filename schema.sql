-- HW4 schema.sql

create table products(
        pid		number(9) primary key,
        name		varchar(20),
        price		decimal(10,2)
);

create table customers(
	cid		number(9) primary key,
	name		varchar(20),
	budget		real -- can be negative
);

create table sales(
	pid		number(9),
	cid		number(9),
	quantity	number(9), -- can't be negative...
	primary key	(pid,cid),
	foreign key 	(pid) references products,
	foreign key 	(cid) references customers
);
