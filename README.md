# cs 5110 project 2

a console based chat application

### Compile the server:

(in the directory with the java file)

    javac ChatServer.java


### Run the server:

    java ChatServer


### Compile the client:

    javac -classpath .:jline-1.0.jar: ChatClient.java


### Run the client:

    java -classpath .:jline-1.0.jar ChatClient <server address>

(or in the directory with runClient.sh)

    ./runClient.sh


### JLine

The client uses the JLine library to enable enable reading a single character from the console and querying the width of the console: http://jline.sourceforge.net/

Part of this problem is discussed here: http://www.darkcoding.net/software/non-blocking-console-io-is-not-possible/

JLine ConsoleReader documentation: http://jline.sourceforge.net/apidocs/index.html
