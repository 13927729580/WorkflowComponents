You have to give run.sh permission to run on the linux server.  do this with:
	"chmod +x run.sh"

run.sh is used on linux machines and runOnWindows.bat is used for windows

To run on a windows machine, change run.sh to runOnWindows.bat in the 
	build.properties file.  You'll also need to change the path to java
	and the path to other files inside of runOnWindows.bat.
	
To remake the TetradClassifier.jar, look at makeJar.bat in the parent directory.

-Peter