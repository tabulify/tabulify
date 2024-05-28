CREATE OR REPLACE FUNCTION dbctl(args TEXT)
    RETURNS TEXT
    LANGUAGE plpython3u
AS
$$
    import subprocess
    result = subprocess.run(['bash', '-c', 'dbctl ' + " ".join(args)], capture_output=True, text=True)
    # result.returncode
    std = result.stdout + "\n" + result.stderr
    return std;
$$;
