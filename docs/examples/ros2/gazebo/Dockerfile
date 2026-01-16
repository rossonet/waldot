FROM ubuntu:24.04 AS initial
# installazione Gazebo
RUN apt update && DEBIAN_FRONTEND=noninteractive apt install -y task-italian lsb-release gnupg bash gawk sed grep bc coreutils wget binutils unzip curl language-pack-it
RUN DEBIAN_FRONTEND=noninteractive apt upgrade -y

FROM initial AS gazebo
RUN curl https://packages.osrfoundation.org/gazebo.gpg --output /usr/share/keyrings/pkgs-osrf-archive-keyring.gpg
RUN echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/pkgs-osrf-archive-keyring.gpg] https://packages.osrfoundation.org/gazebo/ubuntu-stable $(lsb_release -cs) main" > /etc/apt/sources.list.d/gazebo-stable.list
RUN apt update && DEBIAN_FRONTEND=noninteractive apt install -y gz-harmonic
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
RUN /working_dir/set_user_permission.sh $STARTUPDIR $HOME
#COPY bg1.jpg /usr/local/share/doro-lxde-wallpapers/bg1.jpg
#COPY bg2.jpg /usr/local/share/doro-lxde-wallpapers/bg2.jpg
#COPY bg3.jpg /usr/local/share/doro-lxde-wallpapers/bg3.jpg
#COPY bg4.jpg /usr/local/share/doro-lxde-wallpapers/bg4.jpg
RUN apt-get clean && rm -rf /var/lib/apt/lists/* && rm -rf /working_dir

FROM ubuntu:24.04
ARG MAINTAINER="Andrea Ambrosini <andrea.ambrosini@rossonet.org>"
COPY --from=gazebo / /
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
