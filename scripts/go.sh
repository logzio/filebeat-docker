#!/bin/bash

echo "Configuring the container.."
ruby /root/config.rb

echo "Running filebeat.."
filebeat -e
