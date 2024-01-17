-- History
-- 2017-09-06 : Milan Kohut    : Scripts taken from production

-- Unable to render TABLE DDL for object PROFILE_OWN.MSISDN_LIST with DBMS_METADATA attempting internal generator.
CREATE TABLE PROFILE_OWN.MSISDN_LIST
(
  ID NUMBER NOT NULL
, MSISDN VARCHAR2(12 BYTE)
, CONSTRAINT MSISDN_LIST_PK PRIMARY KEY
  (
    ID
  )
  ENABLE
)
LOGGING
TABLESPACE PROFILE_DATA
PCTFREE 10
INITRANS 1
STORAGE
(
  INITIAL 65536
  NEXT 1048576
  MINEXTENTS 1
  MAXEXTENTS UNLIMITED
  BUFFER_POOL DEFAULT
)
NOCOMPRESS
NOPARALLELCREATE UNIQUE INDEX PROFILE_OWN.MSISDN_LIST_IND ON PROFILE_OWN.MSISDN_LIST (MSISDN ASC)
LOGGING
TABLESPACE PROFILE_DATA
PCTFREE 10
INITRANS 2
STORAGE
(
  INITIAL 65536
  NEXT 1048576
  MINEXTENTS 1
  MAXEXTENTS UNLIMITED
  BUFFER_POOL DEFAULT
)
NOPARALLEL


CREATE UNIQUE INDEX PROFILE_OWN.MSISDN_LIST_PK ON PROFILE_OWN.MSISDN_LIST (ID ASC)
LOGGING
TABLESPACE PROFILE_DATA
PCTFREE 10
INITRANS 2
STORAGE
(
  INITIAL 65536
  NEXT 1048576
  MINEXTENTS 1
  MAXEXTENTS UNLIMITED
  BUFFER_POOL DEFAULT
)
NOPARALLEL;

-- Unable to render TABLE DDL for object PROFILE_OWN.ATTRIBUTE_LIST with DBMS_METADATA attempting internal generator.
CREATE TABLE PROFILE_OWN.ATTRIBUTE_LIST
(
  ID NUMBER NOT NULL
, ATTRIBUTE_NAME VARCHAR2(100 BYTE) NOT NULL
, LAST_UPDATE DATE
, EVENT_ID NUMBER
, ATTRIBUTE_VALUE VARCHAR2(100 BYTE)
, CONSTRAINT SYS_C004579 PRIMARY KEY
  (
    ID
  , ATTRIBUTE_NAME
  )
  ENABLE
)
LOGGING
TABLESPACE PROFILE_DATA
PCTFREE 10
INITRANS 1
STORAGE
(
  INITIAL 65536
  NEXT 1048576
  MINEXTENTS 1
  MAXEXTENTS UNLIMITED
  BUFFER_POOL DEFAULT
)
NOCOMPRESS
NOPARALLELCREATE UNIQUE INDEX PROFILE_OWN.SYS_C004579 ON PROFILE_OWN.ATTRIBUTE_LIST (ID ASC, ATTRIBUTE_NAME ASC)
LOGGING
TABLESPACE PROFILE_DATA
PCTFREE 10
INITRANS 2
STORAGE
(
  INITIAL 65536
  NEXT 1048576
  MINEXTENTS 1
  MAXEXTENTS UNLIMITED
  BUFFER_POOL DEFAULT
)
NOPARALLEL
;

-- Unable to render SEQUENCE DDL for object PROFILE_OWN.MSISDN_LIST_ID_SEQ with DBMS_METADATA attempting internal generator.
CREATE SEQUENCE PROFILE_OWN.MSISDN_LIST_ID_SEQ INCREMENT BY 1 MAXVALUE 999999999999999999999999999 MINVALUE 1 CACHE 20 ORDER;
