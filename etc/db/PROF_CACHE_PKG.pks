CREATE OR REPLACE package PROF_CACHE_PKG AS
-- -----------------------------------------------------------------------------
-- |	DESCR: Profile cache support package
-- |	VER:   1.0.1
-- |	NOTE:
-- -----------------------------------------------------------------------------
-- Change History:
-- -----------------------------------------------------------------------------
-- Version     When             Who                      What
-- 1.0.0       01.11.2013       Petr Sorad, IBM          Created
-- 1.0.1       24.02.2016       Milan Kohut, IBM         Added is_volte attribute


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
);

end PROF_CACHE_PKG;
/

