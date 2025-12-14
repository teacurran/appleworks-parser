appleworks-parser
=================

This is a project I have been working off and on for a few years now whenever I get bored.  It is an attempt to build
a working parser for Appleworks and Clarisworks formats and convert them into text or Open Document Format Files.

You can see my research into the Appleworks/ClarisWorks file format here:
[docs/index.adoc](docs/index.adoc)

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

Test Files Needed
-----------------
To complete reverse-engineering of the AppleWorks format, the following test files are needed. Each file should contain only the specific feature to isolate the binary differences.

### Text Formatting
- [ ] Strikethrough text
- [ ] Superscript text
- [ ] Subscript text
- [ ] Multiple font sizes (12pt, 18pt, 24pt, etc.)
- [ ] Font colors (red, blue, etc.)
- [ ] Combined styles (bold+italic, bold+underline, all three)
- [ ] Outline text style
- [ ] Shadow text style

### Paragraph Formatting
- [ ] Left-aligned paragraph
- [ ] Center-aligned paragraph
- [ ] Right-aligned paragraph
- [ ] Justified paragraph
- [ ] Line spacing variations (1.0, 1.5, 2.0)
- [ ] First-line indent
- [ ] Hanging indent
- [ ] Bulleted list
- [ ] Numbered list

### Document Structure
- [ ] Headers with content
- [ ] Footers with content
- [ ] Footnotes
- [ ] Multiple columns (2, 3 columns)
- [ ] Page breaks
- [ ] Section breaks

### Embedded Content
- [ ] Embedded image (PICT format)
- [ ] Embedded table
- [ ] Text box
- [ ] Drawing objects (lines, shapes)

### Spreadsheet Documents
- [ ] Cells with numbers
- [ ] Cells with text
- [ ] Cells with dates
- [ ] Simple formulas (SUM, AVERAGE)
- [ ] Cell formatting (borders, colors)
- [ ] Merged cells

### Version Coverage
For each test file above, versions from different eras are helpful:
- ClarisWorks 4.x (Mac OS Classic)
- ClarisWorks 5.x (Mac OS 9 / Windows)
- AppleWorks 6.x (Mac OS X)

