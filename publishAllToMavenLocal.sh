#!/bin/bash

export IS_EXCLUDE_EXAMPLE=true
./gradlew clean \
&& ./gradlew :butcherknife-annotations:publishOfficialPublicationToMavenLocal \
&& ./gradlew :butcherknife-gradle-plugin:publishOfficialPublicationToMavenLocal \
&& ./gradlew clean \
&& export IS_EXCLUDE_EXAMPLE=false

