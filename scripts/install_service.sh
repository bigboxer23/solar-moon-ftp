#!/usr/bin/env bash
host=${host}
user=${user}
identity_file=${identity_file}

scp -i ${identity_file} -o StrictHostKeyChecking=no -r solar-moon-ftp.service $user@$host:~/
ssh -i ${identity_file} -t $user@$host -o StrictHostKeyChecking=no "sudo mv ~/solar-moon-ftp.service /lib/systemd/system"
ssh -i ${identity_file} -t $user@$host -o StrictHostKeyChecking=no "sudo systemctl daemon-reload"
ssh -i ${identity_file} -t $user@$host -o StrictHostKeyChecking=no "sudo systemctl enable solar-moon-ftp.service"
ssh -i ${identity_file} -t $user@$host -o StrictHostKeyChecking=no "sudo systemctl start solar-moon-ftp.service"