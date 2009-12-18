-- JBoss DNA (http://www.jboss.org/dna)
-- See the COPYRIGHT.txt file distributed with this work for information
-- regarding copyright ownership.  Some portions may be licensed
-- to Red Hat, Inc. under one or more contributor license agreements.
-- See the AUTHORS.txt file in the distribution for a full listing of 
-- individual contributors. 
--
-- JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
-- is licensed to you under the terms of the GNU Lesser General Public License as
-- published by the Free Software Foundation; either version 2.1 of
-- the License, or (at your option) any later version.
--
-- JBoss DNA is distributed in the hope that it will be useful,
-- but WITHOUT ANY WARRANTY; without even the implied warranty of
-- MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
-- Lesser General Public License for more details.
--
-- You should have received a copy of the GNU Lesser General Public
-- License along with this software; if not, write to the Free
-- Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
-- 02110-1301 USA, or see the FSF site: http://www.fsf.org.

CREATE TABLE CHAIN (ID NUMERIC NOT NULL PRIMARY KEY, NAME VARCHAR(30) NOT NULL)
CREATE TABLE AREA (ID NUMERIC NOT NULL PRIMARY KEY, NAME VARCHAR(30) NOT NULL, CHAIN_ID NUMERIC NOT NULL)
CREATE TABLE REGION (ID NUMERIC NOT NULL PRIMARY KEY, NAME VARCHAR(30) NOT NULL, AREA_ID NUMERIC NOT NULL)
CREATE TABLE DISTRICT (ID NUMERIC NOT NULL PRIMARY KEY, NAME VARCHAR(30) NOT NULL, REGION_ID NUMERIC NOT NULL)

CREATE TABLE SALES (ID NUMERIC NOT NULL, SALES_DATE DATETIME NOT NULL, DISTRICT_ID NUMERIC NOT NULL, AMOUNT NUMERIC(10, 2) NULL)
ALTER TABLE SALES ADD CONSTRAINT PK_SALES PRIMARY KEY (ID, SALES_DATE)

ALTER TABLE AREA ADD CONSTRAINT FK_CHAIN FOREIGN KEY(CHAIN_ID) REFERENCES CHAIN(ID) ON DELETE CASCADE
ALTER TABLE REGION ADD CONSTRAINT FK_AREA FOREIGN KEY(AREA_ID) REFERENCES AREA(ID) ON DELETE CASCADE
ALTER TABLE DISTRICT ADD CONSTRAINT FK_REGION FOREIGN KEY(REGION_ID) REFERENCES REGION(ID) ON DELETE CASCADE
