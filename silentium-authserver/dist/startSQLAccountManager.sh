#!/bin/sh
java -Dlogback.configurationFile=./config/logback.xml -cp ./lib/*:* silentium.tools.accountmanager.SQLAccountManager
