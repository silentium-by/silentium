@echo off
title Game Server Registration Console
@java -Dlogback.configurationFile=./config/logback.xml -cp ./lib/*;* silentium.tools.gsregistering.GameServerRegister
@pause