JBOSS_HOME=/app01/profcach/jboss-as-7.1.1.Final
PC_SRC_HOME=/home/milan/work/vfcz/svn/profile-cache/trunk
VERSION=10.0-SNAPSHOT

cp $PC_SRC_HOME/applications/profile-provider/target/profile-provider-$VERSION.war $JBOSS_HOME/standalone/deployments/
