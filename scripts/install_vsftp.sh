#!/usr/bin/env bash
sudo dnf install vsftpd
sudo yum install libdb-utils
sudo echo "#%PAM-1.0" | sudo tee -a /etc/pam.d/vsftpd.virtual
sudo echo "auth	   required     pam_userdb.so db=/etc/vsftpd/vsftpd-virtual-user" | sudo tee -a /etc/pam.d/vsftpd.virtual
sudo echo "account    required     pam_userdb.so db=/etc/vsftpd/vsftpd-virtual-user" | sudo tee -a /etc/pam.d/vsftpd.virtual
sudo echo "session    required     pam_loginuid.so" | sudo tee -a /etc/pam.d/vsftpd.virtual

cd /etc/vsftpd
sudo echo john | sudo tee -a vusers.txt
sudo echo johnpass | sudo tee -a vusers.txt
sudo db_load -T -t hash -f vusers.txt vsftpd-virtual-user.db
sudo chmod 600 vsftpd-virtual-user.db # make it not global readable.
sudo rm vusers.txt
sudo mkdir mkdir /home/vsftpd
sudo chown -R ftp:ftp /home/vsftpd/