#!/bin/bash
# Licensed to DynamoBI Corporation (DynamoBI) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  DynamoBI licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at

#   http:www.apache.org/licenses/LICENSE-2.0

# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

usage() {
    echo "Usage:  buildEigenbaseRelease.sh <tag> <major> <minor> <point>"
}

# Get parameters
if [ "$#" != 4 ]; then
    usage
    exit -1
fi

set -e
set -v

TAG="$1"
MAJOR="$2"
MINOR="$3"
POINT="$4"

# Construct release names
RELEASE_NUMBER="$MAJOR.$MINOR.$POINT"
BINARY_RELEASE="eigenbase-$RELEASE_NUMBER"
LUCIDDB_BINARY_RELEASE="luciddb-$RELEASE_NUMBER"
SRC_RELEASE="eigenbase-src-$RELEASE_NUMBER"
FENNEL_RELEASE="fennel-$RELEASE_NUMBER"
FARRAGO_RELEASE="farrago-$RELEASE_NUMBER"

DIST_DIR=$(cd `dirname $0`; pwd)
OPEN_DIR=$DIST_DIR/../..

# Detect platform
cygwin=false
case "`uname`" in
  CYGWIN*) cygwin=true ;;
esac

if [ $cygwin = "true" ]; then
    ARCHIVE_SUFFIX=zip
else
    ARCHIVE_SUFFIX=tar.bz2

    if [ `uname` != "Darwin" ]; then
        # Verify that chrpath is available
        if [ ! -e /usr/bin/chrpath ]; then
            echo "Error:  /usr/bin/chrpath is not installed"
            exit -1
        fi
    fi
fi

# Stash any changes dev might have made before checking out clean tag
cd $OPEN_DIR
git add -A
git add -u
git stash save
git checkout $TAG
git clean -f -d

GIT_COMMIT=`git rev-parse HEAD`

# Generate version info
# This will fail if requested tag doesn't exist
echo "$BINARY_RELEASE" > $DIST_DIR/VERSION
echo "Git commit @$GIT_COMMIT" >> $DIST_DIR/VERSION
git log -n 1 --pretty $TAG >> $DIST_DIR/VERSION

# Verify that client was mapped correctly
if [ ! -e thirdparty ]; then
    echo "Error:  thirdparty is not where it should be"
    exit -1
fi
if [ ! -e fennel ]; then
    echo "Error:  fennel is not where it should be"
    exit -1
fi
if [ ! -e farrago ]; then
    echo "Error:  farrago is not where it should be"
    exit -1
fi
if [ ! -e luciddb ]; then
    echo "Error:  luciddb is not where it should be"
    exit -1
fi
if [ ! -e extensions ]; then
    echo "Error:  extensions is not where it should be"
    exit -1
fi

# Create farrago/customBuild.properties to set GPL release flag
echo 'build.mode=release' > farrago/customBuild.properties
echo 'build.mode=release' > luciddb/customBuild.properties

# Append setting to pick up custom LucidDB release properties
echo 'release.properties.source=${luciddb.dir}/src/FarragoRelease.properties' \
    >> farrago/customBuild.properties

# Kludge for forcing 32-bit runtime on MacOS
if [ `uname` = "Darwin" ]; then
    echo 'assertions.jvmarg=-ea -esa -d32' >> farrago/customBuild.properties
fi

if [ $cygwin = "false" ]; then

# Build full source release first before projects get polluted by builds
cd $DIST_DIR
rm -f $SRC_RELEASE.$ARCHIVE_SUFFIX
rm -rf $SRC_RELEASE
mkdir -p $SRC_RELEASE/thirdparty
cp -R $OPEN_DIR/thirdparty/* $SRC_RELEASE/thirdparty/
# Delete and stub out irrelevant thirdparty archives
rm -f $SRC_RELEASE/thirdparty/icu-2.8.patch.tgz
rm -f $SRC_RELEASE/thirdparty/tpch.tar.gz
rm -f $SRC_RELEASE/thirdparty/postgresql-*
rm -f $SRC_RELEASE/thirdparty/logging-log4j-*
rm -f $SRC_RELEASE/thirdparty/jfreechart-*
rm -f $SRC_RELEASE/thirdparty/jdbcappender.zip
rm -f $SRC_RELEASE/thirdparty/jcommon-*
rm -rf $SRC_RELEASE/thirdparty/GroboUtils
touch $SRC_RELEASE/thirdparty/logging-log4j-1.3alpha-8.tar.gz
touch $SRC_RELEASE/thirdparty/log4j
touch $SRC_RELEASE/thirdparty/jdbcappender.zip
touch $SRC_RELEASE/thirdparty/jdbcappender
touch $SRC_RELEASE/thirdparty/jtds-1.2-dist.zip
touch $SRC_RELEASE/thirdparty/jtds
touch $SRC_RELEASE/thirdparty/tpch.tar.gz
touch $SRC_RELEASE/thirdparty/tpch
cp -R $OPEN_DIR/fennel $SRC_RELEASE
cp -R $OPEN_DIR/farrago $SRC_RELEASE
cp -R $OPEN_DIR/luciddb $SRC_RELEASE
cp -R $OPEN_DIR/extensions $SRC_RELEASE
cp $DIST_DIR/VERSION $SRC_RELEASE
cp $DIST_DIR/README.src $SRC_RELEASE/README
cp $OPEN_DIR/farrago/COPYING $SRC_RELEASE
tar cjvf $SRC_RELEASE.$ARCHIVE_SUFFIX $SRC_RELEASE
rm -rf $SRC_RELEASE

# Build Farrago-only source release
rm -f $DIST_DIR/$FARRAGO_RELEASE.$ARCHIVE_SUFFIX
rm -rf $DIST_DIR/$FARRAGO_RELEASE
rm -rf $DIST_DIR/farrago
cp -R $OPEN_DIR/farrago $DIST_DIR
cd $DIST_DIR
mv farrago $FARRAGO_RELEASE
cp $DIST_DIR/VERSION $FARRAGO_RELEASE
tar cjvf $FARRAGO_RELEASE.$ARCHIVE_SUFFIX $FARRAGO_RELEASE
rm -rf $DIST_DIR/$FARRAGO_RELEASE


# Build Fennel-only source release
rm -rf $DIST_DIR/$FENNEL_RELEASE
rm -f $DIST_DIR/$FENNEL_RELEASE.$ARCHIVE_SUFFIX
cd $DIST_DIR
mkdir $FENNEL_RELEASE
cp -R $OPEN_DIR/fennel $FENNEL_RELEASE
cp $DIST_DIR/VERSION $FENNEL_RELEASE
tar cjvf $FENNEL_RELEASE.$ARCHIVE_SUFFIX $FENNEL_RELEASE
rm -rf $FENNEL_RELEASE

fi

# Build full binary release
rm -f $DIST_DIR/$BINARY_RELEASE.$ARCHIVE_SUFFIX
cp -f $DIST_DIR/VERSION $OPEN_DIR/farrago/dist
cp -f $DIST_DIR/README.bin $OPEN_DIR/farrago/dist/README
cat > $OPEN_DIR/farrago/dist/FarragoRelease.properties <<EOF
package.name=eigenbase
product.name=Eigenbase Data Management System
product.version.major=$MAJOR
product.version.minor=$MINOR
product.version.point=$POINT
jdbc.driver.name=FarragoJdbcDriver
jdbc.driver.version.major=$MAJOR
jdbc.driver.version.minor=$MINOR
jdbc.url.base=jdbc:farrago:
jdbc.url.port.default=5433
jdbc.url.http.port.default=8033
EOF
cat > $OPEN_DIR/luciddb/src/FarragoRelease.properties <<EOF
package.name=luciddb
product.name=LucidDB
product.version.major=$MAJOR
product.version.minor=$MINOR
product.version.point=$POINT
jdbc.driver.name=LucidDbJdbcDriver
jdbc.driver.version.major=$MAJOR
jdbc.driver.version.minor=$MINOR
jdbc.url.base=jdbc:luciddb:
jdbc.url.port.default=5434
jdbc.url.http.port.default=8034
EOF

cd $OPEN_DIR/farrago
./initBuild.sh --with-fennel --with-optimization --without-debug
./distBuild.sh --skip-init-build
# non-cygwin-
# farrago builds default with change-release in addition to normal specs.
if [ $cygwin = "true" ]; then
  BIN_NAME=farrago.zip
else
  BIN_NAME=$BINARY_RELEASE.$GIT_COMMIT.$ARCHIVE_SUFFIX
fi
cp ../farrago/dist/$BIN_NAME $DIST_DIR/$BINARY_RELEASE.$GIT_COMMIT.$ARCHIVE_SUFFIX

cd $OPEN_DIR/luciddb
./initBuild.sh --without-farrago-build --with-optimization --without-debug
mv dist/luciddb.$ARCHIVE_SUFFIX \
    $DIST_DIR/$LUCIDDB_BINARY_RELEASE.$ARCHIVE_SUFFIX

# Finally attempt to restore their stash. If any of our files would be
# overwritten this will fail and the stash will not be applied.
if [[ "" != `git stash list` ]]; then
  git stash pop
fi

