#! /bin/bash


# [START script]
set -e
set -v

# Talk to the metadata server to get the project id
PROJECTID=$(curl -s "http://metadata.google.internal/computeMetadata/v1/project/project-id" -H "Metadata-Flavor: Google")
BUCKET=$(curl -s "http://metadata.google.internal/computeMetadata/v1/instance/attributes/BUCKET" -H "Metadata-Flavor: Google")

echo "Project ID: ${PROJECTID}  Bucket: ${BUCKET}"

# get our file(s)
gsutil cp "gs://${BUCKET}/"** .

# Install dependencies from apt
apt-get update
apt-get install -t jessie-backports -yq openjdk-8-jdk

# Make Java8 the default
update-alternatives --set java /usr/lib/jvm/java-8-openjdk-amd64/jre/bin/java

# Project Setup
mkdir -p /opt/pollti-workshop/temp
mkdir -p /var/log/pollti-workshop

# very important - by renaming the war to root.war, it will run as the root servlet.
mv pollti-bot-workshop-1.0-SNAPSHOT-jar-with-dependencies.jar /opt/pollti-workshop/pollti-workshop.jar
mv wiki.no.vec /opt/pollti-workshop/temp/wiki.no.vec



# -Dorg.eclipse.jetty.util.log.class=org.eclipse.jetty.util.log.JavaUtilLog

# Reload daemon to pick up new service
systemctl daemon-reload

# Install logging monitor. The monitor will automatically pickup logs sent to syslog.
curl -s "https://storage.googleapis.com/signals-agents/logging/google-fluentd-install.sh" | bash
service google-fluentd restart &


cd /opt/pollti-workshop

# Run jar
java -jar /opt/pollti-workshop/pollti-workshop.jar
cd /

echo "Startup Complete"
# [END script]

#wget -q https://storage.googleapis.com/cloud-debugger/compute-java/format_env_gce.sh
#chmod +x format_env_gce.sh
# CDBG_ARGS="$( sudo ./format_env_gce.sh --app_class_path=ZZZZZZ.jar )"
# java ${CDBG_ARGS} -cp sparky/hellosparky-1.0-SNAPSHOT-jar-with-dependencies.jar com.example.hellosparky.App
