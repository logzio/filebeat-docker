#!/bin/bash

export TAG="kenshoo/kenshoo-filebeat:latest"

docker build -t $TAG ./
echo "Built: $TAG"
