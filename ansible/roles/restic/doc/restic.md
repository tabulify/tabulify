# Restic

## About

`restic` is a backup tool that write immutable file system state:
* via a snapshot mechanism
* from several host
* into a repository

It works mostly as git does with commit, where a snapshot is a commit
of changes.

`restic`:
* can be run by root or by a user with the restic group.
* can read the whole system

For concept, see [Model](#model)

## Features / Advantage

* ransomware situations
* provide version history
* compress and save storage space
* deduplicate to save disk space

## Usage

### Datacadamia

As root, manually
  * To create a [repository](#repository) (one time)
```bash
resticdc init
```
  * to backup
```bash
resticdc-backup
```
  * to forget and prune old snapshots
```bash
resticdc-forget
```


## Run Operations


### Backup File System

Example of command that performs:
* the `backup` operations
* against the `~/work`
* into the repo: `/srv/restic-repo`
* with `verbose` - pass `--verbose` twice (or `--verbose=2`) to display detailed information

```bash
restic --verbose backup ~/work
```

The whole system:
```bash
sudo -u restic /home/restic/bin/restic --exclude={/dev,/media,/mnt,/proc,/run,/sys,/tmp,/var/tmp} -r /tmp backup /
```

We don't use `--files-from-verbatim /home/restic/.config/restic-daily-backup-paths.txt`
because if the set of files is changed restic does not find the parent snapshot
ie we get:
```
no parent snapshot found, will read all files
```

[More](https://restic.readthedocs.io/en/stable/040_backup.html)

### Backup and restore Stdin

Don't as the state of the file is unknown for restic

[Backup File](https://restic.readthedocs.io/en/stable/040_backup.html#reading-data-from-stdin)


### Restore

```bash
mkdir restore
restic restore --target /restore latest
# via snaphost id
restic restore --target restore 10fdbace
# only one file
restic restore --target / --include=/data/pg_dump/dumpfile.sql latest
ls restore/
```

### Find (list files in snapshot)

* Find: searches and list file with a [given pattern](https://pkg.go.dev/path/filepath#Match) in the repository.
```bash
restic find test.txt
restic find --snapshot 20320b550 /opt/www/datacadamia.com/*
````

### Browse
* List: You can list objects such as blobs, packs, index, snapshots, keys or locks with the following command:
```bash
restic list snapshots
```
* cat: display the JSON representation of the objects or their raw content.
```bash
restic cat snapshot d369ccc7d126594950bf74f0a348d5d98d9e99f3215082eb69bf02dc9b3e464c
```
* list files in snapshot
```bash
restic ls --long 7ef25e8f
```

https://restic.readthedocs.io/en/stable/manual_rest.html#browse-repository-objects

### Stats


```bash
restic stats latest
# per host
restic stats --host myserver latest
# on disk
restic stats --host myserver --mode raw-data latest
```

[More](https://restic.readthedocs.io/en/stable/manual_rest.html#metadata-handling)


### Check

There are two types of checks that can be performed:
* Structural consistency and integrity, e.g. snapshots, trees and pack files (default)
* Integrity of the actual data that you backed up (enabled with flags, see below)

Example: verify that all data is properly stored in the repository
```
restic check
```
Run this command regularly to make sure the internal structure of the repository is free of errors.

### Check Read Data

You’re verifying that the repo gives you
exactly the data back that you’ve sent to it,
which is a good thing to very from time to time.

After all, the data could have been corrupted at the service,
you’ll only discover that when you check it regularly.

Since `--read-data` has to download all pack files in the repository, beware that it might incur higher bandwidth costs

By default, the check command does not verify that the actual pack files on disk
in the repository are unmodified, because doing so requires reading a copy of every pack file in the repository.
To tell restic to also verify the integrity of the pack files in the repository, use the `--read-data` flag:
```bash
restic -r /srv/restic-repo check --read-data
```

Alternatively:
  * `--read-data-subset` parameter to check only a subset of the repository pack files at a time.

```bash
restic check --read-data-subset=1/5
restic check --read-data-subset=2/5
restic check --read-data-subset=3/5
restic check --read-data-subset=4/5
restic check --read-data-subset=5/5
```
  * `--read-data-subset` to check a randomly chosen subset of the repository pack files.
```bash
restic check --read-data-subset=2.5% # total percentage
restic check --read-data-subset=50M # size
```

https://restic.readthedocs.io/en/stable/045_working_with_repos.html#checking-integrity-and-consistency

### Diff (Comparing snapshots)

https://restic.readthedocs.io/en/stable/040_backup.html#comparing-snapshots

```bash
restic diff 5845b002 2ab627a6
```

### Forget (Remove snapshot)

The forget policy is applied to the [snapshots id](#snapshot-id-parent-snapshot)

The processed snapshots are evaluated against all `--keep-*` options
but a snapshot only need to match a single option to be kept (the results are ORed).

For example:
```bash
restic forget --dry-run --keep-daily 7 --keep-weekly 5 --keep-monthly 12 --keep-yearly 75
restic prune # remove the data no more referenced
restic check # make sure you are alerted if any damages was made to the repo
```
`forget` would keep:
* the most recent 7 daily by the forget snapshot identifier
* 4 last-day-of-the-week ones (since the 7 dailies already include 1 weekly).
* 12 or 11 last-day-of-the-month snapshots will be kept (depending on whether one of them ends up being the same as a daily or weekly).
* 75 or 74 last-day-of-the-year snapshots are kept, depending on whether one of them ends up being the same as an already kept snapshot.
  All other snapshots are removed.


More https://restic.readthedocs.io/en/stable/060_forget.html

## Run Options
### Dry Runs

```
restic backup ~/work --dry-run -vv | grep "added"
```

https://restic.readthedocs.io/en/stable/040_backup.html#dry-runs

### Exclude
https://restic.readthedocs.io/en/stable/040_backup.html#excluding-files

### Include

give restic a file containing lists of file patterns or paths to be backed up.

This is useful e.g.
* when you want to back up files from many different locations,
* when you use some other software to generate the list of files to back up.


https://restic.readthedocs.io/en/stable/040_backup.html#including-files

### Path

In all cases, paths may be absolute or relative to restic backup’s working directory.

### Verbose

to display detailed information, pass:
* `--verbose` twice
* or `--verbose=2`


## Model


### Repository

All data is stored in a restic repository.

The repo is set via:

* The `-r` option
* The `RESTIC_REPOSITORY` variable (can be scoped in a script such as [resticdc](../templates/resticdc))

The repo is read with the `RESTIC_PASSWORD`
Basic layout of a repository::

```txt
/tmp/restic-repo
├── config
├── data
│   ├── 21
│   │   └── 2159dd48f8a24f33c307b750592773f8b71ff8d11452132a7b2e2a6a01611be1
│   ├── 32
│   │   └── 32ea976bc30771cebad8285cd99120ac8786f9ffd42141d452458089985043a5
│   ├── 59
│   │   └── 59fe4bcde59bd6222eba87795e35a90d82cd2f138a27b6835032b7b58173a426
│   ├── 73
│   │   └── 73d04e6125cf3c28a299cc2f3cca3b78ceac396e4fcf9575e34536b26782413c
│   [...]
├── index
│   ├── c38f5fb68307c6a3e3aa945d556e325dc38f5fb68307c6a3e3aa945d556e325d
│   └── ca171b1b7394d90d330b265d90f506f9984043b342525f019788f97e745c71fd
├── keys
│   └── b02de829beeb3c01a63e6b25cbd421a98fef144f03b9a02e46eff9e2ca3f0bd7
├── locks
├── snapshots
│   └── 22a5af1bdc6e616f8a29579458c49627e01b32210d09adb288d1ecda7c5711ec
└── tmp
```

* A repository stores [data objects](#pack) which can later be requested based on an ID, known as `Storage ID`.
* A file is called a [pack](#pack)
* The `storage ID` is the SHA-256 hash of the content of a file.
* All files in a repository are only written once and never modified afterward.
* Only the prune operation removes data from the repository.
* Repositories consist of several directories and a top-level file called config.
* The file name is the lower case hexadecimal representation of the `storage ID`
* All files are encrypted with AES-256 in counter mode (CTR)

More:
* [Repo format](https://restic.readthedocs.io/en/stable/100_references.html#repository-format)
* [Preparing a new repo](https://restic.readthedocs.io/en/stable/030_preparing_a_new_repo.html)

### Snapshots

The contents of a directory at a specific point in time is called a `snapshot` in restic.

The state means the content and metadata like the name and modification time for the file or the directory and its contents.

Restic snapshots can be compared more to "virtual machine snapshots" or "lvm/zfs file system snapshots" than e.g. a tar file of what has changed.
If nothing has changed, a snapshot is still created to record "this was the current stat" at a particular point in time.

* A snapshot represents a directory with all files and subdirectories at a given point in time.
* For each backup that is made, a new snapshot is created.
* A snapshot is a JSON document that is stored in a file below the directory snapshots in the repository.

If you want to create a snapshot when data are changed, you need to use filesystem watchers.

```bash
restic snapshots --json | jq .
```

[More](https://restic.readthedocs.io/en/stable/100_references.html#snapshots)

### Snapshot ID (Parent Snapshot)

The previous backup snapshot, called `parent` snapshot is the snapshot:
* used to see if there was change.
* where the [forget policy](#forget-remove-snapshot) applies

By default, restic:
* groups snapshots by hostname and backup paths,
* and then selects the latest snapshot in the group that matches the current backup.

The grouping options can be set with `--group-by` (e.g. using `--group-by paths,tags`)

### Snapshot Tags

https://restic.readthedocs.io/en/stable/manual_rest.html#manage-tags
https://restic.readthedocs.io/en/stable/040_backup.html#tags-for-backup

### Pack

A Pack combines one or more Blobs, e.g. in a single file.

A pack is a file in the repo,
which has a name that’s the hexadecimal representation of 32 byte (SHA2 hash of its content).

So `uint(pack[0])` is the first byte, which can have values between 0 and 255.

### Blobs

A Blob combines a number of data bytes with identifying information like the SHA-256 hash of the data and its length.

```bash
restic list blobs
```
```
repository a446abc5 opened successfully, password is correct
data a9affc3085375e6d040e1a230abee2004094775a2489d68f794d08ab7f852e5f
tree f7a9c62a02a3f77c90b3fd39c37c48f2fe084d0a86fbb7041e11dd213339ed7c
tree 2c8de49018674b48747c8d77dbb2875ad1bc1342f19cd787121ac635fff10c4e
tree 0f9c1599f6b22c57c9223983feab66405dc9bf563caf4c957b6bde447a5070d0
tree 053702651c74ac8b689e394df02f7f6e8003181d5de2105998af2e5a1aee12b3
```

### Storage class

By default, backups to Amazon S3 will use the STANDARD storage class.

Available storage classes include
* STANDARD,
* STANDARD_IA,
* ONEZONE_IA,
* INTELLIGENT_TIERING,
* and REDUCED_REDUNDANCY.

* A different storage class could have been specified in the above command by using -o or --option:

```bash
./restic backup -o s3.storage-class=REDUCED_REDUNDANCY test.bin
```

### Priority Run / Resources

Restic in the so-called best effort class (-c2), with the highest possible priority (-n0)

You can chane it.

* [More](https://restic.readthedocs.io/en/stable/faq.html#how-to-prioritize-restic-s-io-and-cpu-time)
* [CPU Usage](https://restic.readthedocs.io/en/stable/047_tuning_backup_parameters.html#cpu-usage)

### Debug

```bash
DEBUG_LOG=/tmp/restic-debug.log restic backup ~/work
```
https://restic.readthedocs.io/en/stable/090_participating.html#debug-logs

### Scripting

```bash
restic snapshots --json | jq .
```
[More](https://restic.readthedocs.io/en/stable/manual_rest.html#scripting)

### Caching / Local Data

Restic keeps a cache with some files from the repository on the local machine. This allows faster operations, since meta data does not need to be loaded from a remote repository.

```bash
~/.cache/restic
# or in our case where there is:
# * a list of repos hash value of the repository name `a446abc5`
# * the directory `CACHEDIR.TAG`
/home/restic/.cache/restic
```

https://restic.readthedocs.io/en/stable/manual_rest.html#caching


### Host

Several hosts may use the same repository to back up directories and files leading to a greater de-duplication.

* [stats](#stats) by host
```bash
# per host
restic stats --host myserver latest
# on disk
restic stats --host myserver --mode raw-data latest
```

* Snapshot by host
```bash
sudo -E -u restic restic snapshots
```
```
repository 11f8283e opened successfully, password is correct
ID        Time                 Host            Tags        Paths
------------------------------------------------------------------------
d3386ada  2023-09-17 14:01:34  beau.bytle.net              /home/nickeau
------------------------------------------------------------------------
```

### File Change Detection

https://restic.readthedocs.io/en/stable/040_backup.html#file-change-detection
