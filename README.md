[![CodeQL](https://github.com/bigboxer23/solar-moon-ftp/actions/workflows/codeql.yml/badge.svg)](https://github.com/bigboxer23/solar-moon-ftp/actions/workflows/codeql.yml)

# solar-moon-ftp

This project runs a small local service that allows remote configuration of a vsftpd instances users and shares.

It listens to an SQS queue for a message, which when received triggers the program to look at the user/accesskey table and
recreate the list of user/pw which vsftpd uses for access to ftp. It handles all user CRUD actions and manages vsftpd
service restarts and directory permissions

## scripts

### `solar-moon-ftp.service`

- service definition file for running this project as a linux service

### `install_service.sh`

- responsible for installing/reloading/restarting the service. It does not build and install the actual jar file ( `mvn package`
  takes care of this)

### `install_vsftp.sh`

- responsible for installing and configuring the vsftpd program. It installs both the program but also other bits needed
  to build a dynamic user and share list

