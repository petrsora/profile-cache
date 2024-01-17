JBOSS_HOME=/app01/profcach/jboss-as-7.1.1.Final
PC_SRC_HOME=/home/milan/work/vfcz/svn/profile-cache-lsvn/trunk
VERSION=10.0-SNAPSHOT

cp $PC_SRC_HOME/applications/events-listener/target/events-listener-$VERSION.ear $JBOSS_HOME/standalone/deployments/
