## What?
This is a universal bootstrapper for Java.
It allows you to start any java main class without caring too much about configuring classpath.

## Why ?
Do not have to care about the jar files to pass to the JVM. Or having to script something to go through all libs in some folder to collect jars. Script that will not be the same on linux and Windows.   

## How?
First build the bootstrapper.
Then use it like this: 

`java -jar itineric-bootstrapper-x.x.x.jar -l lib com.example.MyMainClass -- param1 param2`

To get details about options use `-h` like this:

`java -jar itineric-bootstrapper-x.x.x.jar -h`

