#!/bin/bash

export TAG="logzio/logzio-filebeat:latest"

docker build -t $TAG ./
echo "Built: $TAG"
