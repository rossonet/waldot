#!/bin/bash
source /opt/ros/$ROS_DISTRO/setup.bash
source /root/ros2_ws/install/setup.bash
export RMW_IMPLEMENTATION=rmw_zenoh_cpp
export RMW_ZENOH_Z_MODE=peer
export RMW_ZENOH_Z_INITIAL_PEERS=${ZENOH_PEERS:-tcp/zenoh:7447}
exec "$@"
