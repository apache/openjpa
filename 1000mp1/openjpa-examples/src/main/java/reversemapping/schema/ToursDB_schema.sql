-- Licensed to the Apache Software Foundation (ASF) under one or more
-- contributor license agreements.  See the NOTICE file distributed with
-- this work for additional information regarding copyright ownership.
-- The ASF licenses this file to You under the Apache License, Version 2.0
-- (the "License"); you may not use this file except in compliance with
-- the License.  You may obtain a copy of the License at
--
--     http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.

CREATE TABLE AIRLINES
   (
      AIRLINE CHAR(2) NOT NULL ,
      AIRLINE_FULL VARCHAR(24),
      BASIC_RATE DOUBLE PRECISION,
      DISTANCE_DISCOUNT DOUBLE PRECISION,
      BUSINESS_LEVEL_FACTOR DOUBLE PRECISION,
      FIRSTCLASS_LEVEL_FACTOR DOUBLE PRECISION,
      ECONOMY_SEATS INTEGER,
      BUSINESS_SEATS INTEGER,
      FIRSTCLASS_SEATS INTEGER
   );


ALTER TABLE AIRLINES
   ADD CONSTRAINT AIRLINES_PK Primary Key (
      AIRLINE);


-- \************************************************************\

CREATE TABLE COUNTRIES
   (
      COUNTRY VARCHAR(26) NOT NULL,
      COUNTRY_ISO_CODE CHAR(2) NOT NULL ,
      REGION VARCHAR(26)
   );
 

ALTER TABLE COUNTRIES
   ADD CONSTRAINT COUNTRIES_PK Primary Key (
      COUNTRY_ISO_CODE);

ALTER TABLE COUNTRIES
   ADD CONSTRAINT COUNTRIES_UNQ_NM Unique (
      COUNTRY);


ALTER TABLE COUNTRIES
   ADD CONSTRAINT COUNTRIES_UC
   CHECK (country_ISO_code = upper(country_ISO_code) );


-- \************************************************************\

CREATE TABLE CITIES
   (
      CITY_ID INTEGER NOT NULL ,
      CITY_NAME VARCHAR(24) NOT NULL,
	COUNTRY VARCHAR(26) NOT NULL,
	AIRPORT VARCHAR(3),
	LANGUAGE  VARCHAR(16),
      COUNTRY_ISO_CODE CHAR(2) 
   );


ALTER TABLE CITIES
   ADD CONSTRAINT CITIES_PK Primary Key (
      CITY_ID);

ALTER TABLE CITIES
   ADD CONSTRAINT COUNTRIES_FK Foreign Key (
      COUNTRY_ISO_CODE)
   REFERENCES COUNTRIES (
      COUNTRY_ISO_CODE);


-- \************************************************************\

CREATE TABLE FLIGHTS
   (
      FLIGHT_ID CHAR(6) NOT NULL ,
      SEGMENT_NUMBER INTEGER NOT NULL ,
      ORIG_AIRPORT CHAR(3),
      DEPART_TIME TIME,
      DEST_AIRPORT CHAR(3),
      ARRIVE_TIME TIME,
      MEAL CHAR(1),
      FLYING_TIME DOUBLE PRECISION,
      MILES INTEGER,
      AIRCRAFT VARCHAR(6)
   );

CREATE INDEX DESTINDEX ON FLIGHTS (
      DEST_AIRPORT) ;
  
CREATE INDEX ORIGINDEX ON FLIGHTS (
      ORIG_AIRPORT) ;
 
ALTER TABLE FLIGHTS
   ADD CONSTRAINT FLIGHTS_PK Primary Key (
      FLIGHT_ID,
      SEGMENT_NUMBER);

ALTER TABLE FLIGHTS
   ADD CONSTRAINT MEAL_CONSTRAINT
   CHECK (meal IN ('B', 'L', 'D', 'S'));


-- \************************************************************\

CREATE TABLE FLIGHTAVAILABILITY
   (
      FLIGHT_ID CHAR(6) NOT NULL ,
      SEGMENT_NUMBER INTEGER NOT NULL ,
      FLIGHT_DATE DATE NOT NULL ,
      ECONOMY_SEATS_TAKEN INTEGER DEFAULT 0,
      BUSINESS_SEATS_TAKEN INTEGER DEFAULT 0,
      FIRSTCLASS_SEATS_TAKEN INTEGER DEFAULT 0
   );


ALTER TABLE FLIGHTAVAILABILITY
   ADD CONSTRAINT FLIGHTAVAIL_PK Primary Key (
      FLIGHT_ID,
      SEGMENT_NUMBER,
      FLIGHT_DATE);

ALTER TABLE FLIGHTAVAILABILITY
   ADD CONSTRAINT FLIGHTS_FK2 Foreign Key (
      FLIGHT_ID,
      SEGMENT_NUMBER)
   REFERENCES FLIGHTS (
      FLIGHT_ID,
      SEGMENT_NUMBER);


-- \************************************************************\

CREATE TABLE MAPS
   (
      MAP_ID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
      MAP_NAME VARCHAR(24) NOT NULL,
      REGION VARCHAR(26),
      AREA DECIMAL(8,4) NOT NULL,
      PHOTO_FORMAT VARCHAR(26) NOT NULL,
      PICTURE BLOB(102400),
      UNIQUE (MAP_ID, MAP_NAME)
   );


-- \************************************************************\

CREATE TABLE FLIGHTS_HISTORY
   (
      FLIGHT_ID CHAR(6),
      SEGMENT_NUMBER INTEGER,
      ORIG_AIRPORT CHAR(3),
      DEPART_TIME TIME,
      DEST_AIRPORT CHAR(3),
      ARRIVE_TIME TIME,
      MEAL CHAR(1),
      FLYING_TIME DOUBLE PRECISION,
      MILES INTEGER,
      AIRCRAFT VARCHAR(6), 
      STATUS VARCHAR (20)
   );


