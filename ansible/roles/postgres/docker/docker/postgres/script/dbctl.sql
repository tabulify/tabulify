-- A wrapper around the dbctl application
-- to call the command
-- default timeout of one minute
create or replace procedure dbctl(IN arguments text, IN timeout integer DEFAULT 30)
  language plpython3u
as
$$
  import subprocess
  # plpy https://www.postgresql.org/docs/16/plpython-database.html
  import plpy
  command = 'dbctl ' + " " + arguments

  try:

    result = subprocess.run(['bash', '-c', command],
                            capture_output=True, text=True, timeout=timeout)
    std = result.stdout + "\n" + result.stderr
    if result.returncode != 0:
      raise RuntimeError("Command " + command + " failed: " + std)
    plpy.info(std)

  except subprocess.TimeoutExpired as e:
    raise RuntimeError("Command " + command + " timed out after " + timeout + " seconds")

$$;
