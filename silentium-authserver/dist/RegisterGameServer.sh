#!/bin/sh
java -Dlogback.configurationFile=./config/logback.xml -cp ./lib/*:* silentium.tools.gsregistering.GameServerRegister
