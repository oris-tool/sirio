#!/bin/bash

ERROR="[\e[31;1mERROR\e[0m]"
INFO="[\e[34;1mINFO\e[0m]"
SUCCESS="[\e[32;1mSUCCESS\e[0m]"

if [ -z "$GH_GITHUB" ]
then
    echo -e "$ERROR The environment variable GH_GITHUB is not set."
    exit 1
fi

if [ -z "$GH_WEBSITE" ]
then
    echo -e "$ERROR The environment variable GH_WEBSITE is not set."
    exit 1
fi

if [ -z "$GH_JAVADOC" ]
then
    echo -e "$ERROR The environment variable GH_JAVADOC is not set."
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
echo $GPG_SECRET_KEYS | base64 --decode | $GPG_EXECUTABLE --quiet --import
echo $GPG_OWNERTRUST  | base64 --decode | $GPG_EXECUTABLE --quiet --import-ownertrust

echo -e "$INFO Deploying to the Central Repository..."
# mvn clean deploy --settings ../.travis/settings.xml -DskipTests=true -B -U -P release

echo -e "$INFO Publishing new Javadoc to: www.oris-tool.org/apidoc."

echo -e "$INFO Setting up SSH keys..."
mkdir -p -m 700 ~/.ssh
echo $GH_JAVADOC | base64 --decode > ~/.ssh/gh_javadoc
echo $GH_WEBSITE | base64 --decode > ~/.ssh/gh_website
echo $GH_GITHUB  | base64 --decode > ~/.ssh/known_hosts
chmod 600 ~/.ssh/*
git config --global user.name "Marco Paolieri"
git config --global user.email "paolieri@users.noreply.github.com"


ls target/site
mvn -B javadoc:javadoc
ls target/site

cd target/site

echo -e "$INFO Updating oris-tool/sirio-javadoc..."
GIT_SSH_COMMAND="ssh -i ~/.ssh/gh_javadoc" git clone git@github.com:oris-tool/sirio-javadoc.git
cd sirio-javadoc
rm -rf ../sirio-javadoc/*
git checkout LICENSE
cp -a ../apidocs/* .
git add .
git commit -m "Update Javadoc"
GIT_SSH_COMMAND="ssh -i ~/.ssh/gh_javadoc" git push
cd ..

echo -e "$INFO Updating oris-tool/oris-tool.github.io..."
GIT_SSH_COMMAND="ssh -i ~/.ssh/gh_website" git clone --recurse-submodules https://github.com/oris-tool/oris-tool.github.io.git
cd oris-tool.github.io
GIT_SSH_COMMAND="ssh -i ~/.ssh/gh_website" git submodule update --recursive --remote
git add apidoc
git commit -m "Update Javadoc"
GIT_SSH_COMMAND="ssh -i ~/.ssh/gh_website" git push git@github.com:oris-tool/oris-tool.github.io.git
cd ..

echo -e "$SUCCESS Deployment completed."
