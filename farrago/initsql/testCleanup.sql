-- $Id$
-- This script effects a cleanup after unit tests
-- (actual cleanup work is done by FarragoTestCase.CleanUp),
-- and also verifies repository integrity.
!set verbose true

-- should return empty set
select * from table(sys_boot.mgmt.repository_integrity_violations());
