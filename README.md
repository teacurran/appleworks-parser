appleworks-parser
=================

This is a project I have been working off and on for a few years now whenever I get bored.  It is an attempt to build
a working parser for Appleworks and Clarisworks formats and convert them into text or Open Document Format Files.

You can see my research into the Appleworks/ClarisWorks file format here:
[https://web.archive.org/web/20140503002923/http://wiki.wirelust.com/x/index.php/Projects:Archiving:AppleWorks](https://web.archive.org/web/20140503002923/http://wiki.wirelust.com/x/index.php/Projects:Archiving:AppleWorks)

What is working: 
* document version 
* page size 
* margins 
* document content 

What I am currently working on: 
* styles – (bold, italic, underline) 
* footnotes 

Build Instructions
------------------
* install java
* install apache maven
* mvn package
* once packaged, running ./parse.sh should run the main executable.

