#!/bin/bash

###############################################################################
#
#  beetRoot Product Packager.
#  Version: 5.0
#
#  Notes:
#   -
#
#------------------------------------------------------------------------------
#
#  Copyright 2025 autumo GmbH
#  Date: 2025-12-08
#
###############################################################################


# Vars
VERSION=3.2.0
VERSION_SERVLET_API=6.1.0
LOGFILE="make.log"

# Logfile
if [ "$1" = "create" ]
then
	rm -f "$LOGFILE"
	exec > >(tee -a "$LOGFILE") 2>&1
fi

echo ""
echo "========================================"
echo " autumo beetRoot ${VERSION} - MAKE"
echo "========================================"
echo ""


# ------------------------------------------------
# -------- Usage
# ------------------------------------------------
show_usage() {
    echo " Usage"
    echo "----------------------------------------"
    echo " "
    echo "make clear"
    echo "make create"
    echo " "
    echo "Deployment Notes:"
    echo "- Customize beetroot.sh"
    echo "- Set ROOT for beetroot.sh"
    echo "- Configuration will be correct"
    echo "- Make sure logging levels are set correctly"
    echo " "
}

if [ "$1" = "help" ] || [ "$#" -lt 1 ]
then
    show_usage
    exit 1
fi


# ------------------------------------------------
# -------- DELETE PRODUCT
# ------------------------------------------------
if [ "$1" = "clear" ]
then

	echo " DELETING PRODUCTS"
    echo "----------------------------------------"
	echo ""

	cd product

	# remove working directory
	if [ -d "autumo-beetRoot-$VERSION" ]
	then
		rm -Rf autumo-beetRoot-$VERSION
	fi
	if [ -d "autumo-beetRoot-web-$VERSION" ]
	then
		rm -Rf autumo-beetRoot-web-$VERSION
	fi

	# remove package
	if [ -f "autumo-beetRoot-$VERSION.zip" ]
	then
    	rm autumo-beetRoot-$VERSION.zip
	fi
	if [ -f "autumo-beetRoot-web-$VERSION.zip" ]
	then
    	rm autumo-beetRoot-web-$VERSION.zip
	fi
	if [ -f "beetroot.war" ]
	then
    	rm beetroot.war
	fi
	if [ -f "beetroot-jetty.war" ]
	then
    	rm beetroot-jetty.war
	fi
	if [ -f "beetroot-weblogic.zip" ]
	then
    	rm beetroot-weblogic.zip
	fi

	echo " DONE."
	echo "========================================"
	echo ""

	exit 0
fi


# ------------------------------------------------
# -------- CREATE PRODUCT
# ------------------------------------------------
if [ "$1" != "create" ]
then
	show_usage
	exit 1
fi


echo " CREATING PRODUCTS"
echo "----------------------------------------"

# -----------------------------
# ---- Create unique secret key
# -----------------------------

HEX=`hexdump -vn16 -e'4/4 "%08x" 1 "\n"' /dev/urandom`


# -----------------------------
# ---- Pack & copy libs
# -----------------------------

echo ""
echo " Maven Build, pack latest beetRoot lib"
echo "----------------------------------------"
echo ""

# package beetroot
mkdir -p lib/repo/ch/autumo/beetroot/autumo-beetroot/$VERSION
mvn clean
mvn install


# -----------------------------
# ---- Cleanup & Prepare
# -----------------------------

echo ""
echo " Cleanup & Prepare"
echo "----------------------------------------"
echo ""

# delete old product package
if [ -f "product/autumo-beetRoot-$VERSION.zip" ]
then
	rm product/autumo-beetRoot-$VERSION.zip
fi
if [ -f "product/autumo-beetRoot-web-$VERSION.zip" ]
then
	rm product/autumo-beetRoot-web-$VERSION.zip
fi

# go to product
cd product

# make working directory
mkdir autumo-beetRoot-$VERSION
mkdir autumo-beetRoot-web-$VERSION

echo "-> Done."


# -----------------------------
# ---- Licenses
# -----------------------------

# We no longer copy licenses; all third-party licenses are referenced in THIRDPARTYLICENSES.html.


# -----------------------------
# ---- Copying config
# -----------------------------

echo ""
echo " Copying General Product Artifacts"
echo "----------------------------------------"
echo ""

echo "-> Copying config..."

mkdir autumo-beetRoot-$VERSION/cfg

cp ../cfg/beetroot_dist.cfg autumo-beetRoot-$VERSION/cfg/beetroot.cfg
cp ../cfg/languages.cfg autumo-beetRoot-$VERSION/cfg/languages.cfg
cp ../cfg/routing.xml autumo-beetRoot-$VERSION/cfg/routing.xml

mkdir autumo-beetRoot-web-$VERSION/WEB-INF
mkdir autumo-beetRoot-web-$VERSION/META-INF
mkdir autumo-beetRoot-web-$VERSION/META-INF/etc
mkdir autumo-beetRoot-web-$VERSION/META-INF/etc/licenses

cp ../cfg/beetroot_dist.cfg autumo-beetRoot-web-$VERSION/beetroot.cfg
cp ../cfg/languages.cfg autumo-beetRoot-web-$VERSION/languages.cfg
cp ../cfg/routing.xml autumo-beetRoot-web-$VERSION/routing.xml
cp ../cfg/context.xml autumo-beetRoot-web-$VERSION/META-INF/context.xml
cp ../cfg/web.xml autumo-beetRoot-web-$VERSION/WEB-INF/web.xml

cp ../LICENSE.md autumo-beetRoot-web-$VERSION/META-INF/etc/
cp ../etc/licenses/*.* autumo-beetRoot-web-$VERSION/META-INF/etc/licenses/


# -----------------------------
# ---- Copying logging config
# -----------------------------

echo "-> Copying logging config..."

cp ../cfg/logging-dist.xml autumo-beetRoot-$VERSION/cfg/logging.xml

cp ../cfg/logging-web.xml autumo-beetRoot-web-$VERSION/logging.xml


# -----------------------------
# ---- General copying
# -----------------------------

echo "-> Making directories and copying..."

# Copy libs
mkdir autumo-beetRoot-$VERSION/lib
cp ../lib/*.jar autumo-beetRoot-$VERSION/lib/
mkdir -p autumo-beetRoot-$VERSION/lib/repo/ch/autumo/beetroot/autumo-beetroot/$VERSION
# Copy autumo-beetroot to local repo
cp ../lib/autumo-beetroot-$VERSION.jar autumo-beetRoot-$VERSION/lib/repo/ch/autumo/beetroot/autumo-beetroot/$VERSION/
# Copy local repo for dev (not yet public maven libs)
cp -R ../lib/repo autumo-beetRoot-$VERSION/lib/
mkdir autumo-beetRoot-web-$VERSION/WEB-INF/lib
cp ../lib/*.jar autumo-beetRoot-web-$VERSION/WEB-INF/lib/


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

# --------- Gen resources

mkdir autumo-beetRoot-$VERSION/gen

cp -r ../gen autumo-beetRoot-$VERSION/

# --------- Web resources

mkdir autumo-beetRoot-$VERSION/web

cp -r ../web autumo-beetRoot-$VERSION/

cp -r ../web autumo-beetRoot-web-$VERSION/

# --------- SSL resources

mkdir autumo-beetRoot-$VERSION/ssl

cp -r ../ssl autumo-beetRoot-$VERSION/

cp -r ../ssl autumo-beetRoot-web-$VERSION/

# --------- Copy scripts

mkdir autumo-beetRoot-$VERSION/bin

cp ../bin/pwencoder.sh autumo-beetRoot-$VERSION/bin
cp ../bin/pwencoder.bat autumo-beetRoot-$VERSION/bin

cp ../bin/beetroot.sh autumo-beetRoot-$VERSION/bin
cp ../bin/beetroot.bat autumo-beetRoot-$VERSION/bin

cp ../bin/plant.sh autumo-beetRoot-$VERSION/bin
cp ../bin/plant.bat autumo-beetRoot-$VERSION/bin

cp ../bin/version.sh autumo-beetRoot-$VERSION/bin
cp ../bin/version.bat autumo-beetRoot-$VERSION/bin

# --------- Copy infos

mkdir autumo-beetRoot-$VERSION/doc
mkdir autumo-beetRoot-$VERSION/doc/migration

cp ../doc/*.md autumo-beetRoot-$VERSION/doc
cp ../doc/migration/*.md autumo-beetRoot-$VERSION/doc/migration/

cp ../README.md autumo-beetRoot-$VERSION/
cp ../LICENSE.md autumo-beetRoot-$VERSION/
cp ../THIRDPARTYLICENSES.html autumo-beetRoot-$VERSION/
# Copy dev pom.xml
cp ../etc/pom/pom.xml autumo-beetRoot-$VERSION/

cp ../README.md autumo-beetRoot-web-$VERSION/
cp ../LICENSE.md autumo-beetRoot-web-$VERSION/
cp ../THIRDPARTYLICENSES.html autumo-beetRoot-web-$VERSION/


# -----------------------------
# ---- Create Product
# -----------------------------

echo ""
echo " Building Standalone server"
echo "----------------------------------------"
echo ""

# -- 1. Standalone Server
# Replace unique secret key (seed)
sed -i '' "s/secret_key_seed=.*/secret_key_seed=$HEX/" autumo-beetRoot-${VERSION}/cfg/beetroot.cfg
# The web application server needs a servlet API JAR
(cd autumo-beetRoot-${VERSION}/lib && curl -LO https://repo1.maven.org/maven2/jakarta/servlet/jakarta.servlet-api/${VERSION_SERVLET_API}/jakarta.servlet-api-${VERSION_SERVLET_API}.jar)
# Create archive
zip -r "autumo-beetRoot-${VERSION}.zip" autumo-beetRoot-${VERSION} \
	-x "*/.gitignore" \
	-x "*/.DS_Store" \
	-x "*/__MACOSX"


echo ""
echo " Building Generic Web Build"
echo "----------------------------------------"
echo ""

# -- 2. Generic Web Build
# Enter working directory
cd autumo-beetRoot-web-${VERSION}
# Add servlet context variable to db url
sed -i '' 's|db_url=jdbc:h2.*|db_url=jdbc:h2:[WEB-CONTEXT-PATH]/db/h2/db/beetroot|' beetroot.cfg
# -- Replace unique secret key (seed)
sed -i '' "s/secret_key_seed=.*/secret_key_seed=$HEX/" beetroot.cfg
# Leave working directory
cd ..
# Create archive
zip -r "autumo-beetRoot-web-${VERSION}.zip" autumo-beetRoot-web-${VERSION} \
	-x "*/.gitignore" \
	-x "*/.DS_Store" \
	-x "*/__MACOSX"


echo ""
echo " Building Tomcat"
echo "----------------------------------------"
echo ""

# -- 3. Tomcat
# Copy config
cp ../cfg/logging-web-tomcat.xml autumo-beetRoot-web-$VERSION/logging.xml
# Enter working directory
cd autumo-beetRoot-web-${VERSION}
# Adjust config: Change port (used for email templates)
sed -i '' 's/ws_port=.*/ws_port=8080/' beetroot.cfg
# Adjust config: Add servlet name for WAR version!
sed -i '' 's/web_html_ref_pre_url_part=/web_html_ref_pre_url_part=beetroot/' beetroot.cfg
# Create Web Archive
jar --create --file "beetroot.war" *
echo "-> WAR created."
# Move WAR
mv *.war ../
# Cleanup
rm -f logging.xml
# Leave working directory
cd ..


echo ""
echo " Building WebLogic"
echo "----------------------------------------"
echo ""

# -- 4. WebLogic
# Copy config
cp ../cfg/web-weblogic.xml autumo-beetRoot-web-$VERSION/WEB-INF/web.xml
cp ../cfg/weblogic.xml autumo-beetRoot-web-${VERSION}/WEB-INF/weblogic.xml
cp ../cfg/logging-web-weblogic.xml autumo-beetRoot-web-${VERSION}/WEB-INF/log4j2.xml
# Change port (used for email templates)
sed -i '' 's/ws_port=.*/ws_port=7001/' autumo-beetRoot-web-${VERSION}/beetroot.cfg
sed -i '' 's/mail_session_name=.*/mail_session_name=beetRootMailSession/' autumo-beetRoot-web-${VERSION}/beetroot.cfg
# Pack it for open directory deployment
mkdir beetroot/
cp -R autumo-beetRoot-web-${VERSION}/* beetroot/
# ZIP it (for WebLogc staging, open directory deployment)
zip -r "beetroot-weblogic.zip" beetroot/ \
	-x "*/.gitignore" \
	-x "*/.DS_Store" \
	-x "*/__MACOSX"
# Cleanup
rm -fR beetroot/
rm -f autumo-beetRoot-web-${VERSION}/WEB-INF/log4j2.xml
rm -f autumo-beetRoot-web-${VERSION}/WEB-INF/weblogic.xml
rm -f autumo-beetRoot-web-${VERSION}/WEB-INF/lib/log4j-web-*.jar


echo ""
echo " Building Jetty"
echo "----------------------------------------"
echo ""

# -- 5. Jetty
# Copy config
cp ../cfg/web-jetty.xml autumo-beetRoot-web-$VERSION/WEB-INF/web.xml
cp ../cfg/jetty-web.xml autumo-beetRoot-web-$VERSION/WEB-INF/jetty-web.xml
cp ../cfg/logging-web-jetty.xml autumo-beetRoot-web-${VERSION}/WEB-INF/log4j2.xml
# Adjust config: No AUTO_SERVER=TRUE switch
sed -i '' 's|db_url=jdbc:h2:.*|db_url=jdbc:h2:[WEB-CONTEXT-PATH]/db/h2/db/beetroot;IFEXISTS=TRUE|' autumo-beetRoot-web-${VERSION}/beetroot.cfg
# Adjust config: Change back mailing implementation
sed -i '' 's/mail_implementation=.*/mail_implementation=jakarta/' autumo-beetRoot-web-${VERSION}/beetroot.cfg
sed -i '' 's/mail_session_name=.*/mail_session_name=/' autumo-beetRoot-web-${VERSION}/beetroot.cfg
# Adjust config: Change port (used for email templates)
sed -i '' 's/ws_port=.*/ws_port=8080/' autumo-beetRoot-web-${VERSION}/beetroot.cfg
# Enter working directory
cd autumo-beetRoot-web-${VERSION}
# Create Web Archive
jar --create --file "beetroot-jetty.war" *
echo "-> WAR created."
# Move WAR
mv *.war ../
# Leave working directory
cd ..


echo ""
echo " Cleanup"
echo "----------------------------------------"
echo ""

# Remove working directories
rm -Rf autumo-beetRoot-$VERSION
rm -Rf autumo-beetRoot-web-$VERSION


# -----------------------------
# ---- END
# -----------------------------

# leave product folder
cd ..

echo " DONE."
echo "========================================"
echo ""
