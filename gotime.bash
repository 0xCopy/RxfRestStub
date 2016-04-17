#!/usr/bin/env bash
o=(
#-Djavax.net.debug=all
-ea
-Drxf.content.root.debug=true
-Drxf.content.root=$PWD/target/anyrest-1.0-SNAPSHOT
-Drxf.cached.threadpool=true
-Drxf.debug.sendjson=true
-Drxf.secrets.path=$PWD/src/main/resources
-Drxf.ssl.truststore=$PWD/anyrest.jks
-D1xio.debug.visitor.origins=true

)
r=bin/run.sh
[[ -e  target/cp.txt ]] && r=bin/rerun.sh
set -x
h=$1
p=$2
${r} ${o[*]} srv.MyServer ${h:=$(hostname -f)} ${p:=8888}

