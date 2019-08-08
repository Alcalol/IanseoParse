import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.opencsv.*;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class IanseoParse {

    public static void main(String args[]){

        //First parse parameters if any
        /*
            -a : single event address to process
            -t : text file containing multiple addresses (1 each line)
            -h : write header in CSV or not (0 or 1 for false or true)
            -p : specify path for where to save CSV files
         */

        boolean addressSet = false;     //Does not run unless address is set.
        boolean writeHeaders = true;    //Provide CSV header row? 1 for true, everything else is false.
        boolean multiAddress = false;   //Provide a text file with an even in each line?
        String addressInfo = "";        //If multiAddress, path to text file, otherwise URL to event.
        String outputPath = "";         //Where to save, blank means current working directory

        boolean singleProvided = false;
        boolean multiProvided = false;

        //Process runtime parameters
        for(int i = 0; i < args.length; i++){
            if(args[i].equals("-a") && i < args.length - 1){
                //Single address provided
                addressInfo = args[i + 1];
                addressSet = true;
                singleProvided = true;
            }
            else if(args[i].equals("-t") && i < args.length - 1){
                //Multiple address text file provided
                multiAddress = true;
                addressInfo = args[i + 1];
                addressSet = true;
                multiProvided = true;
            }
            else if(args[i].equals("-h") && i < args.length - 1){
                //Write header option provided
                if (!args[i + 1].equals("1")){
                    writeHeaders = false;
                }
            }
            else if(args[i].equals("-p") && i < args.length - 1){
                //Output path specified
                outputPath = args[i + 1];
            }
            else if(args[0].equals("-h")){
                System.out.println("Available commands:");
                System.out.println("-a <url> Provide a single address for parsing.");
                System.out.println("-t <url> Provide a text file containing one event every line for parsing multiple events.");
                System.out.println("-h <0 | 1 (default)> Specify if a header row is wanted in CSVs, on by default.");
                System.out.println("-p Specify a path to save the event folder. (default is executable directory");
                System.out.println(" ");
                System.out.println("Either -a or -t must be provided, other parameters are optional");
            }
        }

        if(!addressSet){
            System.out.println("Please specify an event address using -a or a text file with multiple events using -t.  Exiting.");
            return;
        }
        if(singleProvided && multiProvided){
            System.out.println("Config conflict!  Please only use -a or -t, but not both.  Exiting.");
            return;
        }

        if(!multiAddress){
            ProcessEvent(outputPath, addressInfo, writeHeaders);
        }
        else{
            try{

                //Multi address file provide, cycle through each line and process events
                File file = new File(addressInfo);

                BufferedReader buffer = new BufferedReader(new FileReader(file));

                String eventURL;

                while((eventURL = buffer.readLine()) != null){

                    ProcessEvent(outputPath, eventURL, writeHeaders);
                }

                buffer.close();

            }
            catch(IOException e){
                System.out.println("Error reading address list file.");
                System.out.println(e.getMessage());
            }

        }

    }

    public static void ProcessEvent(String filePath, String url, boolean writeHeaders){
        //Extract event code from event URL
        String eventCode = url.split("=")[1];
        System.out.println("Procesing event " + eventCode);

        //Hashmap to store all qualification and bracket categories inside current event
        //DetectEventDiscipline searches for all categories listed within the event page.
        HashMap<String, String> linksToParse = DetectEventDisciplines(url);

        linksToParse.forEach((k, v) -> {
            //Process each category individually

            //First check path is legit, if the path to save contains any type of slashes at the end, remove.
            //This path is the raw save path only, does not include file name.
            String fullPath = "";

            if(filePath.equals("")){
                fullPath = eventCode;
            }
            else if(filePath.lastIndexOf("/") == filePath.length() - 1 || filePath.lastIndexOf("\\") == filePath.length() - 1){
                fullPath =  filePath + eventCode;
            }
            else{
                fullPath = filePath + "/" + eventCode;
            }

            if(k.contains("Qualification")){
                //If the event is a qualification, call the appropriate processor
                System.out.println(k);
                ParseAndSaveQualification(fullPath, eventCode + "-" + k + ".csv", v, writeHeaders);
            }
            else if (k.contains("Brackets")){
                //If event is a brackets (H2H), call the appropriate processor
                System.out.println(k);
                if(k.contains("Individual")){

                    ParseAndSaveIndividualBrackets(fullPath, eventCode + "-" + k + ".csv", v, writeHeaders);
                }
                else{
                    //Team - To be implemented

                }
            }
            else if(k.contains("Round_Only")){
                //Round only event, like 1440, call the appropriate processor
                ParseAndSaveRoundOnly(fullPath, eventCode, v, writeHeaders);
            }
        });
    }

    public static HashMap<String, String> DetectEventDisciplines(String url){
        //This function finds all the categories available in provided event
        //Note: Junior only rounds currently not implemented (There sre so many!!)

        //Hashmap listing all possible rounds that are currently supported
        HashMap<String, String> output = new HashMap<>();
        String[] disciplinesArray = {"IC.php", "IQRM.php", "IQRW.php", "IQCM.php", "IQCW.php", "IQBM.php", "IQBW.php", "IQLM.php", "LQLW.php", "IBRM.php", "IBRW.php", "IBCM.php", "IBCW.php", "IBBM.php", "IBBW.php", "IBLM.php", "IBLW.php", "TQRM.php", "TQRW.php", "TQCM.php", "TQCW.php", "TQBM.php", "TQBW.php", "TQLM.php", "TQLW.php", "TBRM.php", "TBRW.php", "TBCM.php", "TBCW.php", "TBBM.php", "TBBW.php", "TBLM.php", "TBLW.php"};

        try{

            Document doc = Jsoup.connect(url).get();

            //Search HTML for all links
            Elements linkTags = doc.getElementsByTag("a");

            linkTags.forEach(t -> {

                //In each link, see if the linked page contains any of the supported category codes
                //If so, add category to list

                if (Arrays.stream(disciplinesArray).parallel().anyMatch(t.toString()::contains)){
                    String linkPath = t.toString().split("\"")[1];

                    String eventType = "";

                    if(linkPath.contains("IC")){
                        eventType = "Round_Only";
                    }
                    else if (linkPath.contains("IQ")){
                        eventType = "Individual_Qualification";
                    }
                    else if(linkPath.contains(("IB"))){
                        eventType = "Individual_Brackets";
                    }
                    else if(linkPath.contains(("TQ"))){
                        eventType = "Team_Qualification";
                    }
                    else if(linkPath.contains(("TB"))){
                        eventType = "Team_Brackets";
                    }
                    else{
                        eventType = "Error";
                    }

                    String discipline = "";

                    if(eventType.equals("Round_Only")){
                        discipline = "All";
                    }
                    else{
                        if(linkPath.contains("RM")){
                            discipline = "Recurve_Men";
                        }
                        else if(linkPath.contains("RW")){
                            discipline = "Recurve_Women";
                        }
                        else if(linkPath.contains("CM")){
                            discipline = "Compound_Men";
                        }
                        else if(linkPath.contains("CW")){
                            discipline = "Compound_Women";
                        }
                        else if(linkPath.contains("BM")){
                            discipline = "Barebow_Men";
                        }
                        else if(linkPath.contains("BW")){
                            discipline = "Barebow_Women";
                        }
                        else if(linkPath.contains("LM")){
                            discipline = "Longbow_Men";
                        }
                        else if(linkPath.contains("LW")) {
                            discipline = "Longbow_Women";
                        }
                        else{
                            discipline = "Error";
                        }
                    }

                    //Only add to list if the event is legit
                    if(!(eventType.equals("Error") || eventType.equals("") || discipline.equals("Error") || discipline.equals(""))){
                        output.put(eventType  + "-" + discipline, "https://ianseo.net" + linkPath);
                    }
                }
            });

            return output;

        }
        catch (IOException e){
            System.out.println("Problem accessing Ianseo event page, please check link and try again.");
            System.out.println("Error: " + e.getMessage());
        }

        return null;
    }

    public static void ParseAndSaveQualification(String filePath, String fileName, String url, boolean writeHeaders){
        try{
            Document doc = Jsoup.connect(url).get();

            //Extract Accordion object
            Element accordion = doc.getElementById("Accordion");

            //Processor is shared with Round only because format is the same
            ProcessQualificationAccordion(filePath, fileName, accordion.getElementsByClass("accordion").first(),writeHeaders);

        }
        catch (IOException ex){
            System.out.println("Error writing " + filePath + " to disk.");
            System.out.println(ex.getMessage());
        }
    }

    public static void ParseAndSaveRoundOnly(String filePath, String eventCode, String url, boolean writeHeaders){
        try{
            String fileName = "";

            Document doc = Jsoup.connect(url).get();

            //Round only has multiple inner accordions per category, nested without two outer accordions
            Element firstAccordion = doc.getElementById("Accordion");

            Element secondAccordion = doc.getElementById("Accordion");

            Elements innerAccordions = doc.getElementsByClass("accordion");

            for(int i = 0; i < innerAccordions.size(); i++){
                //Loop through all inner accordions, thereby processing each category individually

                System.out.println(innerAccordions.get(i).getElementsByClass("title").get(0).text().replace(" ", "").split("\\[")[0]);

                //Set file name to output, only needed in this mode because category is unknown until processing
                fileName = eventCode + "-RoundOnly-" + innerAccordions.get(i).getElementsByClass("title").get(0).text().replace(" ", "").split("\\[")[0] + ".csv";

                //Process current category and write to CSV
                ProcessQualificationAccordion(filePath, fileName, innerAccordions.get(i), writeHeaders);
            }
        }
        catch(IOException e){

        }
    }

    public static void ProcessQualificationAccordion(String filePath, String fileName, Element accordion, boolean writeHeaders){

            //Inner accordion structure is the same for both Qualification AND Round Only types

            //Extract rows within Griglia class table.
            Element table = accordion.getElementsByClass("Griglia").get(0);
            Elements competitors = table.getElementsByTag("tr");

            ArrayList<String[]> csvRows = new ArrayList<>();

            if(writeHeaders){
                String[] headers = {"Rank", "Name", "Country", "Score", "10 + X", "X Only"};

                csvRows.add(headers);
            }

            for(int i = 1; i < competitors.size(); i++){
                //Cycle through per class extracting wanted information

                String rank = competitors.get(i).getElementsByClass("Rank").get(0).text();
                String name = competitors.get(i).getElementsByClass("Athlete").get(0).text();
                String country = competitors.get(i).getElementsByClass("CoCode").get(0).text();
                String score = competitors.get(i).getElementsByClass("Score").get(0).text();
                String golds = competitors.get(i).getElementsByClass("Golds").get(0).text();
                String xCount = competitors.get(i).getElementsByClass("Golds").get(1).text();

                String[] row = {rank, name, country, score, golds, xCount};

                //Add collected row to list
                csvRows.add(row);
            }

            //Write completed CSV row list to file
            WriteToCsv(filePath, fileName, csvRows);
    }

    public static void ParseAndSaveIndividualBrackets(String filePath, String fileName, String url, boolean writeHeaders){
        try{
            Document doc = Jsoup.connect(url).get();

            //First extract accordion, then extract Griglia table
            Element accordion = doc.getElementById("Accordion");
            Element wholeTable = accordion.getElementsByClass("Griglia").first();

            //Extract table rows inside Griglia
            Elements resultRows = wholeTable.getElementsByTag("tr");

            //Extract header data from first row to acquire pass names
            Elements bracketsDef = resultRows.get(0).getElementsByTag("th");
            ArrayList<String> passNameList = new ArrayList<>();

            //Add pass names to list for later use
            bracketsDef.forEach(th -> {
                passNameList.add(th.getElementsByTag("div").get(0).text());
            });

            int roundsCount = passNameList.size();

            ArrayList<ArrayList<String[]>> matchesStorage = new ArrayList<>();

            //Initialize all match lists
            for(int i = 0; i < roundsCount; i++){
                matchesStorage.add(new ArrayList<String[]>());
            }

            for(int i = 1; i < resultRows.size() - 1; i++){
                //Loop through each row in table
                for(int j = 0; j < roundsCount; j++){
                    //Loop through each column in table
                    //First pass - 8 columns
                    //All subsequent passes - 5 columns

                    Elements currentRow = resultRows.get(i).getElementsByTag("td");

                    if(j == 0){
                        //If j ==0, info is first pass, skip byes and empty rows
                        if (!currentRow.get(1).text().equals("") && !currentRow.get(5).text().equals("Bye") && !currentRow.get(5).text().equals("")){
                            //Not blank, has player data
                            String[] tempCompetitor = {currentRow.get(1).text(), currentRow.get(5).text()};

                            //Store competitor in correct pass list
                            matchesStorage.get(j).add(tempCompetitor);
                        }
                    }
                    else{
                        //All subsequent passes, with field column calcualted as below
                        int targetColumn = 7 + (5 * (j - 1)) + 2;

                        if (!currentRow.get(targetColumn).text().equals("") && !currentRow.get(targetColumn + 1).text().equals("Bye") && !currentRow.get(targetColumn + 1).text().equals("")){
                            //Not blank, has player data
                            String[] tempCompetitor = {currentRow.get(targetColumn).text(), currentRow.get(targetColumn + 1).text()};

                            if(j == roundsCount - 1){
                                //Finals, add in reverse order (because Ianseo displays Gold before Bronze)
                                //Store competitor in correct pass list
                                matchesStorage.get(j).add(0, tempCompetitor);
                            }
                            else{
                                //Store competitor in correct pass list
                                matchesStorage.get(j).add(tempCompetitor);
                            }
                        }

                    }
                }
            }

            ArrayList<String[]> csvArray = new ArrayList<>();

            //Cycle through each pass and write to CSV

            if(writeHeaders){
                String[] csvHeader = {"Pass", "A Name", "A Score", "A Win", "B Name", "B Score", "B Win"};
                csvArray.add(csvHeader);
            }

            for(int i = 0; i < passNameList.size(); i++){
                //Loop through each pass
                String passName = passNameList.get(i);

                if(matchesStorage.get(i).size() >= 2){
                    //In each pass, couple current entry and next entry as a H2H pair.
                    //This works because byes and empty fields are not added to this list.
                    //So there should always be an even number of competitors and every
                    //2 items in the list should be the same H2H match
                    for(int j = 0; j < matchesStorage.get(i).size() - 1; j += 2){

                        //If final pass, label matches according to current j index
                        if(i == passNameList.size() - 1 && j == 0){
                            passName = "Bronze";
                        }
                        else if(i == passNameList.size() - 1 && j == 2){
                            passName = "Gold";
                        }

                        int aWin = 0;
                        int bWin = 0;

                        //Detect stars in score box to determin winner for matches with shoot-off
                        if(matchesStorage.get(i).get(j)[1].contains("*")){
                            aWin = 1;
                        }
                        else if(matchesStorage.get(i).get(j + 1)[1].contains("*")){
                            bWin = 1;
                        }
                        else{
                            int aScore = Integer.parseInt(matchesStorage.get(i).get(j)[1]);
                            int bScore = Integer.parseInt(matchesStorage.get(i).get(j + 1)[1]);

                            if(aScore > bScore){
                                aWin = 1;
                            }
                            else{
                                bWin = 1;
                            }
                        }

                        //Save deets to array
                        String[] matchDeets = {passName, matchesStorage.get(i).get(j)[0], matchesStorage.get(i).get(j)[1], aWin + "", matchesStorage.get(i).get(j + 1)[0], matchesStorage.get(i).get(j + 1)[1], bWin + ""};

                        //Add array to csv list for saving
                        csvArray.add(matchDeets);
                    }
                }

            }

            //CSV row list written, save to file
            WriteToCsv(filePath, fileName, csvArray);

        }
        catch(IOException e){

        }


    }

    public static void WriteToCsv(String filePath, String fileName, ArrayList<String[]> rowArray){
        try {

            File dir = new File(filePath);
            dir.mkdirs();

            CSVWriter csvWriter = new CSVWriter(new FileWriter(filePath + "/" + fileName));

            csvWriter.writeAll(rowArray);

            csvWriter.close();
        }
        catch(IOException e){
            System.out.println("Error writing csv file(s).");
            System.out.println(e.getMessage());
        }

    }

}


