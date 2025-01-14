#!/bin/bash
set -e

if [ ! -z "$TRAVIS_TAG" ]
then
  git clone https://$GITHUB_TOKEN@github.com/sbtqa/page-factory-2-example.git
  cd page-factory-2-example
  git remote rm origin
  git remote add origin https://$GITHUB_TOKEN@github.com/sbtqa/page-factory-2-example.git
  sed -i "s#<pf2.version>[0-9.]*</pf2.version>#<pf2.version>$TRAVIS_TAG</pf2.version>#" pom.xml
  git add pom.xml
  git commit -m "bump pf2 version by page-factory-2"
  git pull origin master
  git push origin master
fi

