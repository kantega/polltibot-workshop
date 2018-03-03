#!/usr/bin/env bash

set -ex

BUCKET=pollti-bot-workshop

# [START useful]
ZONE=europe-west1-b

GROUP=pollti-workshop-group
TEMPLATE=$GROUP-tmpl
MACHINE_TYPE=g1-small
IMAGE_FAMILY=debian-8
IMAGE_PROJECT=debian-cloud
STARTUP_SCRIPT=startupScript.sh
SCOPES="userinfo-email,logging-write,storage-full,cloud-platform"
TAGS=ml-training

MIN_INSTANCES=1
MAX_INSTANCES=10
TARGET_UTILIZATION=0.6

SERVICE=ml-task
JAR=robo-madness-server-1.0-SNAPSHOT-jar-with-dependencies.jar


  gsutil cp -r target/${JAR} gce/base gs://${BUCKET}/gce/



esac
set +v