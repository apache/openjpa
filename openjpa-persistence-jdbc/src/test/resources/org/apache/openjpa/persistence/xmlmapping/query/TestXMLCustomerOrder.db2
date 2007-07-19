DELETE FROM TORDER t0
SELECT t0.countryCode, t0.id, t0.version, t0.city, t0.state, t0.street, t0.zip, t0.name FROM TCUSTOMER t0 
DELETE FROM TCUSTOMER WHERE countryCode = ? AND id = ? AND version = ?
DELETE FROM TCUSTOMER WHERE countryCode = ? AND id = ? AND version = ?
INSERT INTO TORDER (oid, amount, delivered, shipAddress, version, customer_countryCode, customer_id) VALUES (?, ?, ?, ?, ?, ?, ?)
INSERT INTO TCUSTOMER (countryCode, id, creditRating, name, version, city, state, street, zip) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
INSERT INTO TCUSTOMER (countryCode, id, creditRating, name, version, city, state, street, zip) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
INSERT INTO TORDER (oid, amount, delivered, shipAddress, version, customer_countryCode, customer_id) VALUES (?, ?, ?, ?, ?, ?, ?)
SELECT t0.shipAddress FROM TORDER t0 
SELECT t0.oid, t0.version, t0.amount, t0.customer_countryCode, t0.customer_id, t0.delivered, t0.shipAddress FROM TORDER t0 
SELECT t0.version, t0.countryCode, t0.id, t0.city, t0.state, t0.street, t0.zip, t0.name FROM TCUSTOMER t0 WHERE t0.countryCode = ? AND t0.id = ?  optimize for 1 row
SELECT t0.oid, t0.version, t0.amount, t0.customer_countryCode, t0.customer_id, t0.delivered, t0.shipAddress FROM TORDER t0 JOIN TORDER t1 ON (1 = 1) WHERE (XMLEXISTS('$t0.shipAddress/*[City = $t1.shipAddress/*/City]' PASSING t0.shipAddress AS "t0.shipAddress", t1.shipAddress AS "t1.shipAddress")) 
SELECT t0.oid, t0.version, t0.amount, t0.customer_countryCode, t0.customer_id, t0.delivered, t0.shipAddress FROM TORDER t0 JOIN TCUSTOMER t1 ON (1 = 1) WHERE (XMLEXISTS('$t0.shipAddress/*[City = $t1.city]' PASSING t0.shipAddress AS "t0.shipAddress", t1.city AS "t1.city")) 
SELECT t0.oid, t0.version, t0.amount, t0.customer_countryCode, t0.customer_id, t0.delivered, t0.shipAddress FROM TORDER t0 WHERE (XMLEXISTS('$t0.shipAddress/*[City = $Parm]' PASSING t0.shipAddress AS "t0.shipAddress", CAST(? AS VARCHAR(254)) AS "Parm")) 
SELECT t0.oid, t0.version, t0.amount, t0.customer_countryCode, t0.customer_id, t0.delivered, t0.shipAddress FROM TORDER t0 WHERE (XMLEXISTS('$t0.shipAddress/*[City = $Parm]' PASSING t0.shipAddress AS "t0.shipAddress", CAST(? AS VARCHAR(254)) AS "Parm")) 
SELECT t0.oid, t0.version, t0.amount, t0.customer_countryCode, t0.customer_id, t0.delivered, t0.shipAddress FROM TORDER t0 WHERE (XMLEXISTS('$t0.shipAddress/*[City = $Parm]' PASSING t0.shipAddress AS "t0.shipAddress", CAST(? AS VARCHAR(254)) AS "Parm")) 
SELECT t0.oid, t0.version, t0.amount, t0.customer_countryCode, t0.customer_id, t0.delivered, t0.shipAddress FROM TORDER t0 WHERE (XMLEXISTS('$t0.shipAddress/*[City = $Parm]' PASSING t0.shipAddress AS "t0.shipAddress", CAST(? AS VARCHAR(254)) AS "Parm")) 
SELECT t0.version, t0.countryCode, t0.id, t0.city, t0.state, t0.street, t0.zip, t0.name FROM TCUSTOMER t0 WHERE t0.countryCode = ? AND t0.id = ?  optimize for 1 row
UPDATE TORDER SET shipAddress = ?, version = ? WHERE oid = ? AND version = ?
SELECT t0.oid, t0.version, t0.amount, t0.customer_countryCode, t0.customer_id, t0.delivered, t0.shipAddress FROM TORDER t0 
SELECT t0.version, t0.countryCode, t0.id, t0.city, t0.state, t0.street, t0.zip, t0.name FROM TCUSTOMER t0 WHERE t0.countryCode = ? AND t0.id = ?  optimize for 1 row
