# HTTPS

If you run beetRoot as a standalone server and you want to run it with the HTTPS protocol, there's a prepared and self-signed keystore file: `ssl/beetroot.jks` that is valid forever.
To do so switch the configuration parameter `ws_https` to 'yes' and use this default keystore file which is specified by the configuration parameter `keystore`.

Your browser will still complain, because it is not issued by a valid Certificate Authority (CA), but you can force the browser to still load the web-app by adding this exception.
If you run beetRoot in productive mode, you have to acquire a valid certificate and store it in the keystore file or in a an own; Java supports the PKCS\#12 format and Java keystore files can be opened with tools such as this one: https://keystore-explorer.org.

The password for `ssl/beetroot.jks` is **`beetroot`**.


<br>
<br>
Click <a href="../README.md">here</a> to go to the main page.

<p align="right"><a href="#top">&uarr;</a></p>
