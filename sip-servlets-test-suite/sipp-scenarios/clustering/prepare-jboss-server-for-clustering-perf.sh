#export JBOSS_HOME=/home/deruelle/tests/mss-1.0/jboss-4

#echo "Script name is		[$0]"
#echo "First Parameter is		[$1]"
#echo "Second Parameter is		[$2]"
#echo "This Process ID is		[$$]"
#echo "This Parameter Count is	[$#]"
#echo "All Parameters		[$@]"
#echo "The FLAGS are			[$-]"


rm -rf $JBOSS_HOME/server/port-1
rm -rf $JBOSS_HOME/server/port-2

#mvn clean install -f ../../../pom.xml -P jboss,jboss-cluster -Dnode=all
cp setup/jboss/performance/mss-sip-stack.properties $JBOSS_HOME/server/all/conf/mss-sip-stack.properties
cp setup/jboss/jboss-context.xml $JBOSS_HOME/server/all/deploy/jboss-web.deployer/context.xml
cp setup/jboss/jboss-tomcat-service.xml $JBOSS_HOME/server/all/deploy/jboss-web.deployer/META-INF/jboss-service.xml
cp setup/jboss/webserver-xmbean.xml $JBOSS_HOME/server/all/deploy/jboss-web.deployer/META-INF/webserver-xmbean.xml
cp setup/jboss/performance/log4j.xml $JBOSS_HOME/server/all/conf/jboss-log4j.xml
cp setup/jboss/server-jboss-failover-all.xml $JBOSS_HOME/server/all/deploy/jboss-web.deployer/server.xml
cp setup/jboss/jboss-service-all.xml $JBOSS_HOME/server/all/conf/jboss-service.xml
mkdir $JBOSS_HOME/server/all/conf/dars

if [ $# -ne 0 ]; then
	case $1 in	
	    proxy)
	    		echo "Distributed example used is proxy"
	    		mvn clean install -f ../../../sip-servlets-examples/location-service-distributable/pom.xml
				cp ../../../sip-servlets-examples/location-service-distributable/target/location-service-distributable-*.war $JBOSS_HOME/server/all/deploy
				cp ../../../sip-servlets-examples/location-service-distributable/distributable-location-service-dar.properties $JBOSS_HOME/server/all/conf/dars/distributable-dar.properties
	            ;;
	    b2bua)
	            echo "Distributed example used is b2bua"
	    		mvn clean install -f ../../../sip-servlets-examples/call-forwarding-distributable/pom.xml
				cp ../../../sip-servlets-examples/call-forwarding-distributable/target/call-forwarding-distributable-*.war $JBOSS_HOME/server/all/deploy
				cp ../../../sip-servlets-examples/call-forwarding-distributable/distributable-call-forwarding-dar.properties $JBOSS_HOME/server/all/conf/dars/distributable-dar.properties
	            ;;
	    c2c)
	    		echo "Distributed example used is Click To call"
	    		mvn clean install -f ../../../sip-servlets-examples/click2call-distributable/pom.xml
				cp ../../../sip-servlets-examples/click2call-distributable/target/click2call-distributable*.war $JBOSS_HOME/server/all/deploy/click2call-distributable.war
				echo "" > $JBOSS_HOME/server/all/conf/dars/distributable-dar.properties
				;;	    
	    *)
	            echo "Distributed example used is uas"
	    		mvn clean install -f ../../../sip-servlets-examples/simple-sip-servlet-distributable/pom.xml
				cp ../../../sip-servlets-examples/simple-sip-servlet-distributable/target/simple-sip-servlet-distributable-*.war $JBOSS_HOME/server/all/deploy
				cp ../../../sip-servlets-examples/simple-sip-servlet-distributable/distributable-simple-dar.properties $JBOSS_HOME/server/all/conf/dars/distributable-dar.properties
	            ;;
    esac
fi

cp -rf $JBOSS_HOME/server/all $JBOSS_HOME/server/port-1
cp -rf $JBOSS_HOME/server/all $JBOSS_HOME/server/port-2
cp setup/jboss/jboss-service-port-1.xml $JBOSS_HOME/server/port-1/conf/jboss-service.xml
cp setup/jboss/jboss-service-port-2.xml $JBOSS_HOME/server/port-2/conf/jboss-service.xml
cp setup/jboss/performance/server-jboss-failover-port-1.xml $JBOSS_HOME/server/port-1/deploy/jboss-web.deployer/server.xml
cp setup/jboss/performance/server-jboss-failover-port-2.xml $JBOSS_HOME/server/port-2/deploy/jboss-web.deployer/server.xml

if [ $# -ne 0 ]; then
	case $1 in		    
	    uac)
	            echo "Distributed example used is uac"
	    		mvn clean process-resources -Dsend.on.init=true install -f ../../../sip-servlets-examples/shootist-sip-servlet-distributable/pom.xml
				cp ../../../sip-servlets-examples/shootist-sip-servlet-distributable/target/shootist-sip-servlet-distributable-*.war $JBOSS_HOME/server/port-1/deploy
				mvn clean process-resources -Dsend.on.init=false install -f ../../../sip-servlets-examples/shootist-sip-servlet-distributable/pom.xml
				cp ../../../sip-servlets-examples/shootist-sip-servlet-distributable/target/shootist-sip-servlet-distributable-*.war $JBOSS_HOME/server/port-2/deploy
				cp ../../../sip-servlets-examples/shootist-sip-servlet-distributable/distributable-shootist-dar.properties $JBOSS_HOME/server/port-1/conf/dars/distributable-dar.properties
				cp ../../../sip-servlets-examples/shootist-sip-servlet-distributable/distributable-shootist-dar.properties $JBOSS_HOME/server/port-2/conf/dars/distributable-dar.properties
	            ;;
	    *)	            
	            ;;
    esac
fi
