FROM ros:jazzy-ros-base
RUN apt-get update && apt-get install -y \
        ros-jazzy-rmw-zenoh-* \
        ros-jazzy-ros-gz-* \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /root/ros2_ws
COPY src ./src
RUN . /opt/ros/$ROS_DISTRO/setup.sh && \
    colcon build --symlink-install
COPY docker_entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh
ENTRYPOINT ["/entrypoint.sh"]
