#! /usr/bin/env bash
#
# Copyright (C) 2013-2015 Bilibili
# Copyright (C) 2013-2015 Zhang Rui <bbcallen@gmail.com>
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

source ./init-repo-url.sh

IJK_XML2_UPSTREAM=$BIK_PLAYER_XML2_UPSTREAM
IJK_XML2_FORK=$BIK_PLAYER_XML2_FORK
IJK_XML2_COMMIT=$BIK_PLAYER_XML2_COMMIT
IJK_XML2_LOCAL_REPO=extra/libxml2

set -e
TOOLS=tools

echo "== pull xml2 base =="
sh $TOOLS/pull-repo-base.sh $IJK_XML2_UPSTREAM $IJK_XML2_LOCAL_REPO

function pull_fork()
{
    echo "== pull xml2 fork $1 =="
    sh $TOOLS/pull-repo-ref.sh $IJK_XML2_FORK android/contrib/libxml2-$1 ${IJK_XML2_LOCAL_REPO}
    cd android/contrib/libxml2-$1
    git checkout ${IJK_XML2_COMMIT} -B ijkplayer
    cd -
}

pull_fork "armv5"
pull_fork "armv7a"
pull_fork "arm64"
pull_fork "x86"
pull_fork "x86_64"
