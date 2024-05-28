CREATE OR REPLACE FUNCTION plpython_env()
  RETURNS TEXT
  LANGUAGE plpython3u
AS
$$
  import subprocess

  result = subprocess.run(['bash', '-c', 'env'], capture_output=True, text=True)

  std = result.stdout + "\n" + result.stderr
  return std

$$;
