@echo off
title Account Manager Console
@java -Dlogback.configurationFile=./config/logback.xml -cp ./lib/*;* silentium.tools.accountmanager.SQLAccountManager
@pause
