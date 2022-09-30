#!/bin/sh

###############################################################################
#
#  beetRoot product packager.
#  Version: 4.0
#
#  Notes:
#   -
#
#------------------------------------------------------------------------------
#
#  Copyright 2022 autumo GmbH
#  Date: 11.02.2022
#
###############################################################################




# Vars
VERSION=1.3.0





# ------------------------------------------------
# -------- Usage
# ------------------------------------------------
if [ "$1" = "help" -o "$#" -lt 1 ]
then
	echo " "
	echo "make clear"
	echo "make create"
	echo " "
	echo " "
	echo "Deployment Notes:"
	echo "- Make sure logging levels are set correctly"
	echo "- Set ROOT for beetroot.sh"
	echo "- Customize beetroot.sh"
	echo "- Configuration will be correct."
	echo " "
	exit 1
fi




# ------------------------------------------------
# -------- DELETE PRODUCT
# ------------------------------------------------
if [ "$1" = "clear" ]
then
	cd product
	
	# remove working directory
	rm -Rf autumo-beetRoot-$VERSION
	rm -Rf autumo-beetRoot-web-$VERSION
	
	# remove package
	rm autumo-beetRoot-$VERSION.zip
	rm autumo-beetRoot-web-$VERSION.zip

	rm beetroot.war
	rm beetroot-jetty.war
	
	exit 1
fi



# ------------------------------------------------
# -------- CREATE PRODUCT
# ------------------------------------------------
if [ "$1" = "create" ]
then


# -----------------------------
# ---- Create unique secret key
# -----------------------------
HEX=`hexdump -vn16 -e'4/4 "%08x" 1 "\n"' /dev/urandom`


# -----------------------------
# ---- Pack & copy libs
# -----------------------------
		
#	echo "-> Pack and copy newest beetroot lib..."
	
	# package beetroot
#	mvn package
#	cp target/autumo-beetroot-$VERSION.jar lib/
	

# -----------------------------
# ---- Cleanup & Prepare
# -----------------------------

	echo "-> Cleanup & prepare..."
	
	# delete old product package
	rm product/autumo-beetRoot-$VERSION.zip
	rm product/autumo-beetRoot-web-$VERSION.zip
	
	# go to product
	cd product
	
	# make working directory
	mkdir autumo-beetRoot-$VERSION
	mkdir autumo-beetRoot-web-$VERSION


# -----------------------------
# ---- Licenses
# -----------------------------
		
	echo "-> Copying 3rd party licenses..."
	
	mkdir autumo-beetRoot-$VERSION/etc
	mkdir autumo-beetRoot-$VERSION/etc/licenses
	cp ../etc/licenses/* autumo-beetRoot-$VERSION/etc/licenses


# -----------------------------
# ---- Copying config
# -----------------------------
		
	echo "-> Copying config..."
	
	mkdir autumo-beetRoot-$VERSION/cfg
	
	cp ../cfg/beetroot.cfg autumo-beetRoot-$VERSION/cfg/beetroot.cfg


	mkdir autumo-beetRoot-web-$VERSION/WEB-INF
	mkdir autumo-beetRoot-web-$VERSION/META-INF
	mkdir autumo-beetRoot-web-$VERSION/META-INF/etc
	mkdir autumo-beetRoot-web-$VERSION/META-INF/etc/licenses

	cp ../cfg/beetroot.cfg autumo-beetRoot-web-$VERSION/beetroot.cfg
	cp ../cfg/web.xml autumo-beetRoot-web-$VERSION/WEB-INF/web.xml
	cp ../cfg/context.xml autumo-beetRoot-web-$VERSION/META-INF/context.xml

	cp ../LICENSE.txt autumo-beetRoot-web-$VERSION/META-INF/etc/
	cp ../etc/licenses/*.* autumo-beetRoot-web-$VERSION/META-INF/etc/licenses/
	
	
# -----------------------------
# ---- Copying logging config
# -----------------------------
		
	echo "-> Copying logging config..."

	cp ../cfg/logging-dist.xml autumo-beetRoot-$VERSION/cfg/logging.xml
	
	cp ../cfg/logging-web.xml autumo-beetRoot-web-$VERSION/logging.xml


# -----------------------------
# ---- General copying & signing
# -----------------------------

	echo "-> Making directories and copying..."
	
	# replace productive passwords!
	#for file in interfaces/templates/*.ifacex
	#do
	#	sed -i '' 's/rest_in_api_key=.*/rest_in_api_key=<YOUR_OWN_API_KEY>/' $file
	#	
	#done

	# copy libs
	mkdir autumo-beetRoot-$VERSION/lib
	cp ../lib/*.jar autumo-beetRoot-$VERSION/lib/
	rm autumo-beetRoot-$VERSION/lib/jakarta.mail-api*.jar
	#rm autumo-beetRoot-$VERSION/lib/jakarta.mail*.jar
	#rm autumo-beetRoot-$VERSION/lib/jakarta.activation*.jar
	rm autumo-beetRoot-$VERSION/lib/javax.servlet*.jar
	#cp ../lib/jakarta.activation-api*.jar autumo-beetRoot-$VERSION/lib/
	rm autumo-beetRoot-$VERSION/lib/mysql*.jar
	
	mkdir autumo-beetRoot-web-$VERSION/WEB-INF/lib
	cp ../lib/*.jar autumo-beetRoot-web-$VERSION/WEB-INF/lib/
	rm autumo-beetRoot-web-$VERSION/WEB-INF/lib/jakarta.mail*.jar
	rm autumo-beetRoot-web-$VERSION/WEB-INF/lib/jakarta.activation*.jar
	rm autumo-beetRoot-web-$VERSION/WEB-INF/lib/javax.servlet*.jar
	rm autumo-beetRoot-web-$VERSION/WEB-INF/lib/mysql*.jar
	cp ../lib/jakarta.activation-api*.jar autumo-beetRoot-web-$VERSION/WEB-INF/lib/
	
	
	echo "-> Signing libs..."
	jarsigner -storepass 73UtVBzPU7ULY5Ewp6sSQMpi -keystore ../cfg/KeyStore.jks -tsa http://tsa.pki.admin.ch/tsa autumo-beetRoot-$VERSION/lib/autumo-beetroot-${VERSION}.jar autumo.ch

	# make empty dirs
	mkdir autumo-beetRoot-$VERSION/log

# --------- H2 DB

	mkdir autumo-beetRoot-$VERSION/db
	cd autumo-beetRoot-$VERSION/db
	mkdir h2
	cd h2
	mkdir db
	cd ..
	cd ..
	cd ..
	
	cp ../db/h2/h2* autumo-beetRoot-$VERSION/db/h2/
	cp ../db/h2/db/dist/* autumo-beetRoot-$VERSION/db/h2/db/
	cp ../db/*.sql autumo-beetRoot-$VERSION/db/
	
	
	mkdir autumo-beetRoot-web-$VERSION/db
	cd autumo-beetRoot-web-$VERSION/db
	mkdir h2
	cd h2
	mkdir db
	cd ..
	cd ..
	cd ..

	cp ../db/h2/h2* autumo-beetRoot-web-$VERSION/db/h2/
	cp ../db/h2/db/dist/* autumo-beetRoot-web-$VERSION/db/h2/db/
	cp ../db/*.sql autumo-beetRoot-web-$VERSION/db/
	

# --------- Web resources

	mkdir autumo-beetRoot-$VERSION/web
	
	cp -r ../web autumo-beetRoot-$VERSION/

	
	cp -r ../web autumo-beetRoot-web-$VERSION/
	
# --------- Copy scripts

	cp ../pwencoder.sh autumo-beetRoot-$VERSION/.
	cp ../pwencoder.bat autumo-beetRoot-$VERSION/.

	cp ../beetroot.sh autumo-beetRoot-$VERSION/.
	cp ../beetroot.bat autumo-beetRoot-$VERSION/.

	cp ../plant.sh autumo-beetRoot-$VERSION/.
	cp ../plant.bat autumo-beetRoot-$VERSION/.


# --------- Copy infos

	cp ../README.md autumo-beetRoot-$VERSION/
	cp ../LICENSE.txt autumo-beetRoot-$VERSION/
	cp ../THIRDPARTYLICENSES.txt autumo-beetRoot-$VERSION/

	cp ../README.md autumo-beetRoot-web-$VERSION/
	cp ../LICENSE.txt autumo-beetRoot-web-$VERSION/
	cp ../THIRDPARTYLICENSES.txt autumo-beetRoot-web-$VERSION/


# -----------------------------
# ---- Create Product
# -----------------------------

	echo "-> Create PRODUCT..."


	# -- Replace unique secret key (seed)
	sed -i '' "s/secret_key_seed=.*/secret_key_seed=$HEX/" autumo-beetRoot-${VERSION}/cfg/beetroot.cfg

	# create archive
	zip -r "autumo-beetRoot-${VERSION}.zip" autumo-beetRoot-${VERSION} \
		-x "*/.DS_Store" \
		-x "*/__MACOSX"



	cd autumo-beetRoot-web-${VERSION}
	# add servlet context variable to db url
	sed -i '' 's|db_url=.*|db_url=jdbc:h2:[WEB-CONTEXT-PATH]/db/h2/db/beetroot|' beetroot.cfg
	# -- Replace unique secret key (seed)
	sed -i '' "s/secret_key_seed=.*/secret_key_seed=$HEX/" beetroot.cfg
	cd ..

	# create archive
	zip -r "autumo-beetRoot-web-${VERSION}.zip" autumo-beetRoot-web-${VERSION} \
		-x "*/.DS_Store" \
		-x "*/__MACOSX"


	# -- BUILD container products

	# -- 1. Tomcat
	cd autumo-beetRoot-web-${VERSION}
	# change port (used for email templates)
	sed -i '' 's/ws_port=.*/ws_port=8080/' beetroot.cfg
	# add servlet name for WAR version!
	sed -i '' 's/web_html_ref_pre_url_part=/web_html_ref_pre_url_part=beetroot/' beetroot.cfg
	jar --create --file "beetroot.war" *
	mv *.war ../
	cd ..


	# -- 2. WebLogic
	cp ../cfg/weblogic.xml autumo-beetRoot-web-${VERSION}/WEB-INF/weblogic.xml
	cp ../cfg/logging-web-weblogic.xml autumo-beetRoot-web-${VERSION}/logging.xml
	# change port (used for email templates)
	sed -i '' 's/ws_port=.*/ws_port=7001/' autumo-beetRoot-web-${VERSION}/beetroot.cfg
	# Use Javax for mailing
	sed -i '' 's/mail_implementation=.*/mail_implementation=javax/' autumo-beetRoot-web-${VERSION}/beetroot.cfg
	sed -i '' 's/mail_session_name=.*/mail_session_name=beetRootMailSession/' autumo-beetRoot-web-${VERSION}/beetroot.cfg
	# Pack it
	zip -r "beetroot-weblogic.zip" autumo-beetRoot-web-${VERSION}/ \
		-x "*/.DS_Store" \
		-x "*/__MACOSX"


	# -- 3. Jetty
	rm -f autumo-beetRoot-web-${VERSION}/WEB-INF/weblogic.xml
	cp ../cfg/web-jetty.xml autumo-beetRoot-web-$VERSION/WEB-INF/web.xml
	cp ../cfg/jetty-web.xml autumo-beetRoot-web-$VERSION/WEB-INF/jetty-web.xml
	cp ../lib/ext/slf4j-simple* autumo-beetRoot-web-$VERSION/WEB-INF/lib
	rm -f autumo-beetRoot-web-$VERSION/logging.xml
	rm -f autumo-beetRoot-web-$VERSION/WEB-INF/lib/log4j*
	# no AUTO_SERVER=TRUE switch
	sed -i '' 's|db_url=jdbc:h2:.*|db_url=jdbc:h2:[WEB-CONTEXT-PATH]/db/h2/db/ifacex;IFEXISTS=TRUE|' autumo-beetRoot-web-${VERSION}/beetroot.cfg
	# change port (used for email templates)
	sed -i '' 's/ws_port=.*/ws_port=8080/' autumo-beetRoot-web-${VERSION}/beetroot.cfg
	cd autumo-beetRoot-web-${VERSION}
	jar --create --file "beetroot-jetty.war" *
	mv *.war ../
	cd ..


	rm -Rf autumo-beetRoot-$VERSION
	rm -Rf autumo-beetRoot-web-$VERSION
	
	
# -----------------------------
# ---- END
# -----------------------------
	
	# leave product folder
	cd ..

	
else
	echo "Nope! -> make create|clear "
	echo " "
fi



