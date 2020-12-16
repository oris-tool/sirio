#!/bin/bash

ERROR="[\e[31;1mERROR\e[0m]"
INFO="[\e[34;1mINFO\e[0m]"
SUCCESS="[\e[32;1mSUCCESS\e[0m]"

if [ -z "$GITHUB_KNOWN_HOSTS" ]
then
    echo -e "$ERROR The environment variable GITHUB_KNOWN_HOSTS is not set."
    exit 1
fi

if [ -z "$GITHUB_SSH_KEY" ]
then
    echo -e "$ERROR The environment variable GITHUB_SSH_KEY is not set."
    exit 1
fi

if [ -z "$SONATYPE_USERNAME" ]
then
    echo -e "$ERROR The environment variable SONATYPE_USERNAME is not set."
    exit 1
fi

if [ -z "$SONATYPE_PASSWORD" ]
then
    echo -e "$ERROR The environment variable SONATYPE_PASSWORD is not set."
    exit 1
fi

if [ -z "$GPG_EXECUTABLE" ]
then
    echo -e "$ERROR The environment variable GPG_EXECUTABLE is not set."
    exit 1
fi

if [ -z "$GPG_OWNERTRUST" ]
then
    echo -e "$ERROR The environment variable GPG_OWNERTRUST is not set."
    exit 1
fi

if [ -z "$GPG_SECRET_KEYS" ]
then
    echo -e "$ERROR The environment variable GPG_SECRET_KEYS is not set."
    exit 1
fi

if [ -z "$GPG_PASSPHRASE" ]
then
    echo -e "$ERROR The environment variable GPG_PASSPHRASE is not set."
    exit 1
fi

if [ "$TRAVIS_PULL_REQUEST" != 'false' ]
then
    echo -e "$ERROR Pull requests are not to be deployed."
    exit 1
fi

if [ -z "$TRAVIS_TAG" -a "$TRAVIS_BRANCH" != 'master' ]
then
    echo -e "$ERROR Only tagged or master-branch commits are to be deployed."
    exit 1
fi

if [ ! -z "$TRAVIS_TAG" ]
then
    VERSION=${TRAVIS_TAG:1}
    echo -e "$INFO Deploying a new release: $VERSION"
    echo -e "$INFO Updating the project version..."
    mvn versions:set -DnewVersion=$VERSION 1>/dev/null 2>/dev/null
else
    VERSION=`mvn help:evaluate -Dexpression=project.version -B -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn  2>/dev/null| grep -v "^\["`
    echo -e "$INFO Deploying a new snapshot: $VERSION"
fi

set -e

echo -e "$INFO Setting up GPG keys..."
$GPG_EXECUTABLE --version
echo $GPG_SECRET_KEYS | base64 --decode | $GPG_EXECUTABLE --quiet --batch --import
echo $GPG_OWNERTRUST  | base64 --decode | $GPG_EXECUTABLE --quiet --batch --import-ownertrust

echo -e "$INFO Deploying to the Central Repository..."
mvn clean deploy --settings ../.travis/settings.xml -DskipTests=true -B -U -P release

echo -e "$INFO Setting up SSH known_hosts..."
mkdir -p -m 700 ~/.ssh
echo $GITHUB_KNOWN_HOSTS  | base64 --decode > ~/.ssh/known_hosts

echo -e "$INFO Setting up SSH key..."
echo $GITHUB_SSH_KEY | base64 --decode > ~/.ssh/id_rsa
chmod 600 ~/.ssh/*

echo -e "$INFO Configuring git..."
git config --global user.name "ORIS Bot"
git config --global user.email "oris-bot@users.noreply.github.com"

echo -e "$INFO Generating Javadoc..."
mvn -B javadoc:javadoc

echo -e "$INFO Updating oris-tool/java-api..."
cd target/site/apidocs
cp ../../../../LICENSE.txt LICENSE
git init
git add .
git commit -m 'Update Java API docs'
git remote add origin git@github.com:oris-tool/java-api.git
git clone git@github.com:oris-tool/java-api.git
git push -u -f --mirror

echo -e "$SUCCESS Deployment completed."
