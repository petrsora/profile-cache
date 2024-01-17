#!/usr/bin/bash
URL=http://localhost:8080/profileproviderweb/profile
FILE=$1

/usr/bin/wget --post-file=$FILE $URL
