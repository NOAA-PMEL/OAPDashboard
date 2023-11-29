import os
import sys
import paramiko
from stat import S_ISDIR

_verbose=True

server ="sftp.pmel.noaa.gov"
username = "ncei_sftp"
path_to_ssh = os.path.join("/Users/kamb", ".ssh")
# path_to_hosts_file = os.path.join("~", ".ssh", "known_hosts")
path_to_hosts_file = os.path.join(path_to_ssh, "known_hosts")
path_to_pkey_file = os.path.join(path_to_ssh, "ncei-key-rsa")

pkey = paramiko.RSAKey.from_private_key_file(path_to_pkey_file)
ssh = paramiko.SSHClient()
ssh.load_host_keys(os.path.expanduser(path_to_hosts_file))
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect(server, username=username, pkey=pkey,look_for_keys=False,disabled_algorithms={'pubkeys': ['rsa-sha2-512', 'rsa-sha2-256']})

# client.connect(hostname=HOSTNAME,username=LOGIN_USERNAME,password=LOGIN_PASSWORD,sock=jumpbox_channel,pkey=pkey,look_for_keys=False,disabled_algorithms={'pubkeys': ['rsa-sha2-512', 'rsa-sha2-256']})

# ssh = paramiko.SSHClient()
# ssh.load_host_keys(os.path.expanduser(path_to_hosts_file))
# ssh.connect(server, username=username, key_filename=os.path.expanduser(path_to_pkey_file))

sftp = ssh.open_sftp()

def log(msg):
    if _verbose:
        print(msg)

def isdir(path):
    try:
        return S_ISDIR(sftp.stat(path).st_mode)
    except IOError:
        return False

def confirm(path):
    if isdir(path):
        files = sftp.listdir(path=path)
        print(f'Removing {path} will recursively remove directory and files ', end='')
        print(files)
        confirmed = input("Are you sure? [yN] ")
    else:
        confirmed = input(f'Remove file {path}? [yN] ')
    log(f"Confirmed:{confirmed}.")
    return confirmed.lower() == 'y'

def rmdR(path):
    log(f'rmdR {path}')
    if isdir(path):
        files = sftp.listdir(path=path)
        for f in files:
            filepath = os.path.join(path, f)
            if isdir(filepath):
                rmdR(filepath)
            else:
                log(f'remove file {filepath}')
                sftp.remove(filepath)

        log(f'remove dir {path}')
        sftp.rmdir(path)
    else:
        log(f'remove file {path}')
        sftp.remove(path)

def close():
    if ssh:
        ssh.close()
    exit()

if __name__ == "__main__":
    if len(sys.argv) <= 1:
        log('No path given.')
        close()
    path = sys.argv[1]
    if path == "/":
        print("Removing / is not allowed.")
        close()
    try:
        exists = sftp.stat(path)
        log(exists)
    except:
        log(f'Path {path} does not appear to exist!')
        close()
    if confirm(path):
        log(f'sftp remove path {path}')
        rmdR(path)
    else:
        log(f"Not removing {path}")

    close()
