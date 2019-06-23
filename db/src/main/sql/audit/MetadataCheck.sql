-- Eerst parameter schema van de powercenter repository
-- Tweede paramter database link van de powercenter repository

Rem to suppress the old and new message when parameters are used
Set Verify Off
Rem to suppress the header of the SQL result
Set Heading On
Rem Width of a line infinite (before wrapping to the next line) in charcater
Set Linesize 150
Rem To authorize dbms_output
Set Serveroutput On;
Rem To suppress the message PL/SQL procedure successfully completed but also no rows selected
set feedback off

Rem Environement
define environement=idle
column connect_id new_value environement 
select decode(lower('&_CONNECT_IDENTIFIER'),'d1042t','dev','d1633t','tst',lower('&_CONNECT_IDENTIFIER')) connect_id from dual; 

Set Markup Html On Entmap Off
Spool RapportMetaDataCheck_at_&environement..html

Rem Creatie van de view om een lijst van alle mappingen te controleren
@@WorkflowSessionCreateView &1 &2

Prompt <H1>Rapport Check</H1>
Prompt <H2>PowerCenter Metadata Verification</H2>

Prompt <H3>Mapping Check</H3>
Prompt <ul>
Prompt <li>Mapping Target Load Order Check</li>
@@MappingTargetLoadOrderCheck &1 &2
Prompt <li>Test the presence of the audit trail instance (DUAL_START EN EIND)</li>
@@MappingAuditTrailCheck &1 &2
Prompt </ul>

Prompt <H3>Audit Trail Process Check</H3>
Prompt <ul>
Prompt <li>Process in COBI_AT_PROCESSEN zijn goed</li>
@AuditTrailCobiAtProcessenControl
Prompt </ul>


Set Markup Html Off
spool off
exit

