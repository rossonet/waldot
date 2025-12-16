#!/usr/bin/env bash
### every exit != 0 fails the script
set -e

echo "Install some common tools for further installation"
apt-get install -y nmap tcpdump vim wget net-tools locales bzip2 procps apt-utils \
    python3-numpy #used for websockify/novnc

echo "generate locales for it_IT.UTF-8"
echo "it_IT.UTF-8 UTF-8" > /etc/locale.gen
locale-gen
