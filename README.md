# IanseoParse
A Parser to convert Ianseo events into CSV files

Download pre-compiled executable here <a href="https://github.com/Alcalol/IanseoParse/releases">Releases</a>

This parser can take any Ianseo event page (pages which has URL ending in toId?=xxxx),
which contains all the qualification and H2H data and converts them into separate CSV files
by categories.

Project was written in Java within Intellij IDEA and compiled into a Jar file that includes all dependencies.

To run executable in cmd:
java -jar IanseoParse.jar <parameters>

Usage:
-a <url>              Provide a single address for parsing.
-t <text file>        Provide a text file containing one event every line for parsing multiple events.
-h <0 | 1 (default)>  Specify if a header row is wanted in CSVs, on by default.
-p <path>             Specify a path to save the event folder.





Additional Info:
In event pages, the links to each category has a set system, with event type followed by category.
Individual qualification pages starts with IQ.
Individual brackets pages starts with IB.
Team qualification pages starts with TQ.
Team brackets pages starts with TB.

The above codes are followed by discipline, for example RM is Recurve Men, CW is Compound Women.

Combining the above provides exact page names, eg: Recurve Men Qualification = IQRM.php
Using this method the parser scans through the event page for all combinations possible to pick up every link for every category shot at the event.

The format of each individual qualification/bracket pages of an event are the following:

Ianseo displays round and match info inside "Accordion" divs.

Inside each accordion there is a HTML table with class "Griglia".

If Qualification, the first row tells us the column labels depending on round.  
Subsequent rows in qualification pages each contains one competitor, sorted by rank, with all required info in named classes.
  
If H2H, the first row tells us the title of each pass (eg. 1/32, 1/8, Finals etc).
The subsequent rows of H2H pages are a bit more complicated:  
 - Each row spans the entire page, encompassing every pass columns, so more than one competitor can appear in one row
  
 - The position each competitor appears determines which pass they featured in:
  
 - The info for first pass always appears in the columns cells 1 - 8 with order Rank, Name, Flag, Country code, Country, Score, null, null.
  
 - The cells for all subsequent passes spans 5 cells per pass(9 - 13, 14 - 18 etc...) with order of null, score, shoot-off score (null if N/A), null, null.
  
 - After the finals <td> cells, there are always 5 cells that follows it )presumable for formatting.
  
 - If a shoot-off was required, a star will appear next to BOTH the set score AND the shoot-off score for the winner.

With the above information, the total number of <td> cells in each row in a bracket page can be calculated by formula (n = passes in event):
  
8 + (5 * (n - 1)) + 5
eg. for 6 pass events, there are 8 + (5 * (6 - 1)) + 5 = 38 cells and 28 cells in a 4 pass event.       

For rounds that are non WA70 (and/or H2H), like 1440, the formatting is very similar to qualification but with additional columns for distance scores.


