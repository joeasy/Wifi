#! /bin/sh
SCRIPT_PATH="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

OPEN_SRC_PATH="http://172.21.0.100/svn/col/DVR/branches/software/system/open_source"
OPEN_SRC_DNAME=open_source

cd $SCRIPT_PATH/../../../../../../../../system/
if [ ! -d "$OPEN_SRC_DNAME" ]; then
	svn co -N $OPEN_SRC_PATH
	cd $OPEN_SRC_DNAME
	svn up upnp
fi

