FROM ubuntu:24.04 AS initial
# installazione Gazebo
RUN apt update && DEBIAN_FRONTEND=noninteractive apt install -y task-italian lsb-release gnupg bash gawk sed grep bc coreutils wget binutils unzip curl language-pack-it python3-full pipx
RUN DEBIAN_FRONTEND=noninteractive apt upgrade -y

FROM initial AS console
RUN mkdir /working_dir
#STS
RUN cd /working_dir && wget -q https://cdn.spring.io/spring-tools/release/STS4/4.31.0.RELEASE/dist/e4.36/spring-tools-for-eclipse-4.31.0.RELEASE-e4.36.0-linux.gtk.x86_64.tar.gz
RUN cd /working_dir && tar -xzf spring-tools-for-eclipse-4.31.0.RELEASE-e4.36.0-linux.gtk.x86_64.tar.gz && ls && mv sts-4.31.0.RELEASE /opt/sts-4
RUN echo "deb https://download.sublimetext.com/ apt/stable/" |  tee /etc/apt/sources.list.d/sublime-text.list \
    && wget -qO - https://download.sublimetext.com/sublimehq-pub.gpg | apt-key add - \
    && apt-get update \
    && DEBIAN_FRONTEND=noninteractive apt-get install -y sublime-text
RUN apt-get update \
    && apt-get install ca-certificates curl gnupg -y \
    && curl -sSL https://pkgs.netbird.io/debian/public.key | gpg --dearmor --output /usr/share/keyrings/netbird-archive-keyring.gpg \
    && echo 'deb [signed-by=/usr/share/keyrings/netbird-archive-keyring.gpg] https://pkgs.netbird.io/debian stable main' | tee /etc/apt/sources.list.d/netbird.list \
    && apt-get update && apt-get install netbird -y
RUN apt-get update \
    && DEBIAN_FRONTEND=noninteractive apt-get install -y openssh-client nmap tcpdump wireshark iproute2 iputils-arping iputils-ping traceroute firefox apt-utils libcanberra-gtk3-module curl wget git vim openjdk-21-jdk filezilla putty evince thunar-archive-plugin netcat-traditional \
    iperf iperf3 corkscrew ostinato iptables
ENV DISPLAY=:1 \
    VNC_PORT=5901 \
    NO_VNC_PORT=6901 \
    HOME=/headless \
    TERM=xterm \
    STARTUPDIR=/dockerstartup \
    INST_SCRIPTS=/headless/install \
    NO_VNC_HOME=/headless/noVNC \
    DEBIAN_FRONTEND=noninteractive \
    VNC_COL_DEPTH=24 \
    VNC_RESOLUTION=1280x1024 \
    VNC_PW=pass123 \
    VNC_VIEW_ONLY=false
WORKDIR $HOME
ADD ./src/common/install/ /working_dir/
ADD ./src/debian/install/ /working_dir/
RUN /working_dir/tools.sh
ENV LANG='it_IT.UTF-8' LANGUAGE='it_IT:it' LC_ALL='it_IT.UTF-8'
RUN /working_dir/install_custom_fonts.sh
RUN /working_dir/tigervnc.sh
RUN /working_dir/no_vnc.sh
RUN /working_dir/icewm_ui.sh
ADD ./src/debian/icewm/ $HOME/
RUN /working_dir/libnss_wrapper.sh
ADD ./src/common/scripts $STARTUPDIR
RUN pipx install -q --system-site-packages zenoh-cli
RUN ln -s /headless/.local/share/pipx/venvs/zenoh-cli/bin/zenoh /usr/bin/zenoh
RUN /working_dir/set_user_permission.sh $STARTUPDIR $HOME
#COPY bg1.jpg /usr/local/share/doro-lxde-wallpapers/bg1.jpg
#COPY bg2.jpg /usr/local/share/doro-lxde-wallpapers/bg2.jpg
#COPY bg3.jpg /usr/local/share/doro-lxde-wallpapers/bg3.jpg
#COPY bg4.jpg /usr/local/share/doro-lxde-wallpapers/bg4.jpg
RUN apt-get clean && rm -rf /var/lib/apt/lists/* && rm -rf /working_dir

FROM ubuntu:24.04
ARG MAINTAINER="Andrea Ambrosini <andrea.ambrosini@rossonet.org>"
COPY --from=console / /
# noVNC webport, connect via http://IP:6901/?password=pass123
ENV DISPLAY=:1 \
    VNC_PORT=5901 \
    NO_VNC_PORT=6901 \
    HOME=/headless \
    TERM=xterm \
    STARTUPDIR=/dockerstartup \
    INST_SCRIPTS=/headless/install \
    NO_VNC_HOME=/headless/noVNC \
    DEBIAN_FRONTEND=noninteractive \
    VNC_COL_DEPTH=24 \
    VNC_RESOLUTION=1280x1024 \
    VNC_PW=pass123 \
    VNC_VIEW_ONLY=false
EXPOSE $VNC_PORT $NO_VNC_PORT
WORKDIR $HOME
#USER 1000
ENTRYPOINT ["/dockerstartup/vnc_startup.sh"]
CMD ["--wait"]
