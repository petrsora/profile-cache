CREATE OR REPLACE package body PROF_CACHE_PKG AS
-- -----------------------------------------------------------------------------
-- |	DESCR: Profile cache support package
-- |	VER:   1.0.3
-- |	NOTE:
-- -----------------------------------------------------------------------------
-- Change History:
-- -----------------------------------------------------------------------------
-- Version     When             Who                      What
-- 1.0.0       01.11.2013       Petr Sorad, IBM          Created
-- 1.0.1       10.02.2014       Petr Sorad, IBM          Grant added
-- 1.0.2       18.02.2014       Petr Sorad, IBM          Null attrs chk added
-- 1.0.3       24.02.2016       Milan Kohut, IBM         Added is_volte attribute

function getId(p_msisdn IN VARCHAR2) return NUMBER
as
  v_id NUMBER;
begin
  select ID
  into v_id
  from MSISDN_LIST 
  where MSISDN=p_msisdn
  ;
  return v_id;
exception
  when NO_DATA_FOUND then
    return NULL;  
end getId;

-- inserts or updates MSISDN's profile 
procedure upsert_profile(
  p_msisdn         IN VARCHAR2,
  p_is_prepaid     IN VARCHAR2,
  p_is_child       IN VARCHAR2,
  p_is_restricted  IN VARCHAR2,
  p_has_prsm       IN VARCHAR2,
  p_sch_prf        IN VARCHAR2,
  p_cust_loc       IN VARCHAR2,
  p_has_mpenebar   IN VARCHAR2,
  p_is_volte       IN VARCHAR2,
  p_id            OUT NUMBER
) as
  v_id NUMBER;
begin
  v_id := getId(p_msisdn);
  if v_id is NULL then
    -- get new ID and insert master rec
    select MSISDN_LIST_ID_SEQ.NEXTVAL into v_id from dual;    
    insert into MSISDN_LIST (ID, MSISDN) values (v_id, p_msisdn);
    p_id := NULL;     
  else
    -- delete old profile attrs, ID and master rec is reused
    delete from ATTRIBUTE_LIST where ID=v_id;
    p_id := v_id;     
  end if;
  -- do attr insert
  if p_is_prepaid is not null then
    insert into ATTRIBUTE_LIST (ID, ATTRIBUTE_NAME, LAST_UPDATE, EVENT_ID, ATTRIBUTE_VALUE) 
    values (v_id, 'IS_PREP',  SYSDATE, 1, p_is_prepaid);
  end if;
  if p_is_child is not null then  
    insert into ATTRIBUTE_LIST (ID, ATTRIBUTE_NAME, LAST_UPDATE, EVENT_ID, ATTRIBUTE_VALUE) 
    values (v_id, 'IS_CHILD',  SYSDATE, 1, p_is_child);
  end if;
  if p_is_restricted is not null then
    insert into ATTRIBUTE_LIST (ID, ATTRIBUTE_NAME, LAST_UPDATE, EVENT_ID, ATTRIBUTE_VALUE) 
    values (v_id, 'IS_RESTRICTED',  SYSDATE, 1, p_is_restricted);
  end if;
  if p_has_prsm is not null then  
    insert into ATTRIBUTE_LIST (ID, ATTRIBUTE_NAME, LAST_UPDATE, EVENT_ID, ATTRIBUTE_VALUE) 
    values (v_id, 'HAS_PRSM',  SYSDATE, 1, p_has_prsm);
  end if;
  if p_sch_prf is not null then   
    insert into ATTRIBUTE_LIST (ID, ATTRIBUTE_NAME, LAST_UPDATE, EVENT_ID, ATTRIBUTE_VALUE) 
    values (v_id, 'SCH_PRF',  SYSDATE, 1, p_sch_prf);
  end if;
  if p_cust_loc is not null then  
    insert into ATTRIBUTE_LIST (ID, ATTRIBUTE_NAME, LAST_UPDATE, EVENT_ID, ATTRIBUTE_VALUE) 
    values (v_id, 'CUST_LOC',  SYSDATE, 1, p_cust_loc);
  end if;
  if p_has_mpenebar is not null then  
    insert into ATTRIBUTE_LIST (ID, ATTRIBUTE_NAME, LAST_UPDATE, EVENT_ID, ATTRIBUTE_VALUE) 
    values (v_id, 'HAS_MPENEBAR',  SYSDATE, 1, p_has_mpenebar);
  end if;
  if p_is_volte is not null then
    insert into ATTRIBUTE_LIST (ID, ATTRIBUTE_NAME, LAST_UPDATE, EVENT_ID, ATTRIBUTE_VALUE)
    values (v_id, 'IS_VOLTE',  SYSDATE, 1, p_is_volte);
  end if;
  p_id := 0;
end upsert_profile; 

end PROF_CACHE_PKG;
/
GRANT EXECUTE ON PROF_CACHE_PKG TO PROFILE_USR
/ 

