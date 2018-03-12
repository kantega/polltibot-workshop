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
JAR=pollti-bot-workshop-1.0-SNAPSHOT-jar-with-dependencies.jar
# [END useful]

function print_usage() {
  echo "Usage: ${0} gce | gce-many | down"
  echo ""
  echo "This command is useful as a place to let you easily move back and forth between running or"
  echo "deploying the bookshelf app.  You may add all your configuration here, so you don't need"
  echo "to change them in every version of this application."
  echo ""
  echo "gce      - mvn package; gsutil cp; gcloud compute instances create ...; - deploys to Compute Engine"
  echo "down     - tears down a single instance group"

}

if [ $# = 0 ]; then
  print_usage
  exit
fi

COMMAND=$1

case $COMMAND in
  # usage flags
  --help|-help|-h)
    print_usage
    exit
    ;;



gce)
  set -v
# [START gce-single]
  # mvn clean install assembly:assembly



  #no http for now
  #gcloud compute firewall-rules create allow-http-bookshelf \
  #  --allow tcp:80 \
  #  --source-ranges 0.0.0.0/0 \
  #  --target-tags ${TAGS} \
  #  --description "Allow port 80 access to instances tagged with ${TAGS}"

  gcloud compute instances create pollti-workshop-instance \
    --machine-type=${MACHINE_TYPE} \
    --scopes=${SCOPES} \
    --metadata-from-file startup-script=${STARTUP_SCRIPT} \
    --zone=${ZONE} \
    --tags=${TAGS} \
    --image-family=${IMAGE_FAMILY} \
    --image-project=${IMAGE_PROJECT} \
    --metadata BUCKET=${BUCKET}
# [END gce-single]
  ;;

down)
  set -v
  gcloud compute instances delete pollti-workshop-instance --zone=${ZONE}
  ;;


esac
set +v